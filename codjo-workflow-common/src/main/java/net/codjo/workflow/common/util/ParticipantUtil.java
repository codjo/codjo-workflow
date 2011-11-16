package net.codjo.workflow.common.util;
import net.codjo.agent.AclMessage;
import static net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Behaviour;
import net.codjo.agent.MessageTemplate;
import static net.codjo.agent.MessageTemplate.and;
import static net.codjo.agent.MessageTemplate.matchConversationId;
import static net.codjo.agent.MessageTemplate.matchPerformative;
import static net.codjo.agent.MessageTemplate.matchProtocol;
import static net.codjo.agent.MessageTemplate.or;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.protocol.JobProtocol;
/**
 *
 */
public class ParticipantUtil {
    private final Behaviour behaviour;
    private JobRequest jobToExecute;
    private AclMessage requestMessage;


    public ParticipantUtil(Behaviour behaviour) {
        this.behaviour = behaviour;
    }


    public ParticipantUtil(Behaviour behaviour, AclMessage requestMessage) {
        this.behaviour = behaviour;
        setRequestMessage(requestMessage);
    }


    public void setRequestMessage(AclMessage requestMessage) {
        if (requestMessage == null) {
            this.jobToExecute = null;
            this.requestMessage = null;
            return;
        }
        this.requestMessage = requestMessage;
        this.jobToExecute = (JobRequest)requestMessage.getContentObject();
    }


    public JobRequest getJobRequest() {
        return jobToExecute;
    }


    public String getJobRequestType() {
        return jobToExecute.getType();
    }


    public AclMessage getRequestMessage() {
        return requestMessage;
    }


    public static MessageTemplate createRequestTemplate() {
        return and(matchProtocol(JobProtocol.ID), matchPerformative(Performative.REQUEST));
    }


    public MessageTemplate createListenParticipantTemplate() {
        return and(matchProtocol(JobProtocol.ID),
                   and(matchConversationId(requestMessage.getConversationId()),
                       or(matchPerformative(Performative.INFORM),
                          matchPerformative(Performative.REFUSE))));
    }


    public void sendAudit(JobAudit audit) {
        audit.setRequestId(jobToExecute.getId());

        AclMessage inform = requestMessage.createReply(Performative.INFORM);
        inform.setContentObject(audit);
        behaviour.getAgent().send(inform);
    }
}

