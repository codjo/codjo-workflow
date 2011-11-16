/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.subscribe;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.agent.Aid;
import net.codjo.agent.DFService;
import net.codjo.agent.DFService.DFServiceException;
import net.codjo.agent.protocol.InitiatorHandler;
import net.codjo.agent.protocol.SubscribeInitiator;
import net.codjo.agent.protocol.SubscribeProtocol;
import net.codjo.workflow.common.Service;
import net.codjo.workflow.common.message.JobEvent;
import org.apache.log4j.Logger;
/**
 * Agent responsable d'écouter les évènements lié aux 'Job' ({@link net.codjo.workflow.common.message.JobAudit}
 * et {@link net.codjo.workflow.common.message.JobRequest}).
 */
public class JobListenerAgent extends Agent {
    private static final Logger LOG = Logger.getLogger(JobListenerAgent.class);

    private SubscribeInitiator jobListener;
    private JobEventHandler eventHandler;
    private Aid jobLeaderAid;


    public JobListenerAgent(JobEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }


    public String createNickName() {
        return "listener-" + Integer.toHexString(System.identityHashCode(this));
    }


    @Override
    protected void setup() {
        try {
            jobLeaderAid = DFService.searchFirstAgentWithService(this, Service.JOB_LEADER_SERVICE);
        }
        catch (DFServiceException exception) {
            LOG.error("Impossible de trouver un agent de type : " + Service.JOB_LEADER_SERVICE, exception);
            die();
        }

        AclMessage subscribe = new AclMessage(AclMessage.Performative.SUBSCRIBE);
        subscribe.setProtocol(SubscribeProtocol.ID);
        subscribe.addReceiver(jobLeaderAid);

        jobListener = new SubscribeInitiator(this, new HandlerAdapter(), subscribe);
        addBehaviour(jobListener);
    }


    @Override
    protected void tearDown() {
        if (jobLeaderAid != null) {
            jobListener.cancel(jobLeaderAid, true);
        }
    }


    private class HandlerAdapter implements InitiatorHandler {

        public void handleAgree(AclMessage agree) {
        }


        public void handleRefuse(AclMessage refuse) {
            eventHandler.receiveError(new ProtocolErrorEvent(ProtocolErrorEvent.Type.REFUSE, refuse));
        }


        public void handleInform(AclMessage inform) {
            eventHandler.receive((JobEvent)inform.getContentObject());
        }


        public void handleFailure(AclMessage failure) {
            eventHandler.receiveError(new ProtocolErrorEvent(ProtocolErrorEvent.Type.FAILURE, failure));
        }


        public void handleOutOfSequence(AclMessage outOfSequenceMessage) {
            eventHandler.receiveError(
                  new ProtocolErrorEvent(ProtocolErrorEvent.Type.OUT_OF_SEQUENCE, outOfSequenceMessage));
        }


        public void handleNotUnderstood(AclMessage notUnderstoodMessage) {
            eventHandler.receiveError(
                  new ProtocolErrorEvent(ProtocolErrorEvent.Type.NOT_UNDERSTOOD, notUnderstoodMessage));
        }
    }
}
