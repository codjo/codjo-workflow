/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.protocol;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Aid;
import net.codjo.agent.Behaviour;
import net.codjo.agent.MessageTemplate;
import net.codjo.agent.UserId;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.util.RequestUtil;
import java.util.Date;
/**
 * Behaviour 'Initiateur' dans le protocole job.
 *
 * @see JobProtocol
 */
public abstract class JobProtocolInitiator extends Behaviour {
    private final JobRequest request;
    private final Aid participantAID;
    private final UserId userId;
    private int step = 0;
    private boolean done = false;
    private MessageTemplate conversationIdTemplate;
    private long replyTimeout;


    protected JobProtocolInitiator(JobRequest jobRequest,
                                   Aid participantAID,
                                   UserId userId,
                                   long replyTimeout) {
        if (jobRequest.getId() == null) {
            jobRequest.setId(RequestUtil.generateConversationId(System.currentTimeMillis(), this));
        }
        this.userId = userId;
        this.request = jobRequest;
        this.participantAID = participantAID;
        this.replyTimeout = replyTimeout;
    }


    protected JobProtocolInitiator(JobRequest jobRequest,
                                   Aid participantAID,
                                   long replyTimeout) {
        this(jobRequest, participantAID, null, replyTimeout);
    }


    @Override
    protected void action() {
        if (step == 0) {
            getAgent().send(createRequest(request));

            conversationIdTemplate = MessageTemplate.matchConversationId(request.getId());

            step = 1;
        }
        else {
            AclMessage message = getAgent().receive(conversationIdTemplate);

            if (message == null) {
                block();
                return;
            }

            if (AclMessage.Performative.INFORM == message.getPerformative()
                && JobProtocol.ID.equals(message.getProtocol())) {
                JobAudit audit = (JobAudit)message.getContentObject();
                if (JobAudit.Type.PRE == audit.getType() && audit.hasError()) {
                    stopProtocol();
                }
                else if (JobAudit.Type.POST == audit.getType()) {
                    stopProtocol();
                }
                handleInform(message);
            }
            else if (AclMessage.Performative.FAILURE == message.getPerformative()) {
                stopProtocol();
                handleFailure(message);
            }
            else {
                handleOutOfSequence(message);
            }
        }
    }


    @Override
    public boolean done() {
        return done;
    }


    protected JobRequest getJobRequest() {
        return request;
    }


    protected AclMessage createRequest(JobRequest requestToSend) {
        AclMessage message = new AclMessage(AclMessage.Performative.REQUEST);
        message.addReceiver(participantAID);
        message.setConversationId(requestToSend.getId());
        message.setProtocol(JobProtocol.ID);
        message.setContentObject(requestToSend);
        message.setReplyByDate(new Date(System.currentTimeMillis() + replyTimeout));
        if (userId != null) {
            message.encodeUserId(userId);
        }
        return message;
    }


    protected abstract void handleInform(AclMessage inform);


    protected void handleFailure(AclMessage failure) {
    }


    protected void handleOutOfSequence(AclMessage outOfSequence) {
    }


    protected void stopProtocol() {
        done = true;
    }
}
