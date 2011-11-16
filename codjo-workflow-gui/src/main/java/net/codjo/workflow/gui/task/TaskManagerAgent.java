package net.codjo.workflow.gui.task;
import net.codjo.agent.AclMessage;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Agent;
import net.codjo.agent.Aid;
import net.codjo.agent.DFService;
import net.codjo.agent.DFService.DFServiceException;
import net.codjo.agent.protocol.InitiatorHandler;
import net.codjo.agent.protocol.RequestInitiator;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.agent.protocol.SubscribeInitiator;
import net.codjo.agent.protocol.SubscribeProtocol;
import net.codjo.workflow.common.Service;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.XmlCodec;
/**
 *
 */
public class TaskManagerAgent extends Agent {
    private final XmlCodec xmlCodec = new XmlCodec();
    private final Callback callback;
    private Aid organiserAid;
    private SubscribeInitiator organiserListener;


    public TaskManagerAgent(Callback callback) {
        this.callback = callback;
    }


    @Override
    protected void setup() {
        try {
            organiserAid = DFService.searchFirstAgentWithService(this, Service.ORGANISER_SERVICE);
        }
        catch (DFServiceException e) {
            die();
            return;
        }

        AllJobsHandler jobsHandler = new AllJobsHandler();
        addBehaviour(new RequestInitiator(this, jobsHandler, createGetAllJobsMessage(organiserAid)));
        organiserListener = new SubscribeInitiator(this, jobsHandler, createSubscribeMessage(organiserAid));
        addBehaviour(organiserListener);
    }


    @Override
    protected void tearDown() {
        if (organiserAid != null) {
            organiserListener.cancel(organiserAid, true);
        }
    }


    private AclMessage createGetAllJobsMessage(Aid aid) {
        AclMessage message = new AclMessage(Performative.QUERY, RequestProtocol.QUERY);
        message.addReceiver(aid);
        message.setContent("allJobs");
        return message;
    }


    private AclMessage createSubscribeMessage(Aid aid) {
        AclMessage subscribeMessage = new AclMessage(Performative.SUBSCRIBE, SubscribeProtocol.ID);
        subscribeMessage.addReceiver(aid);
        return subscribeMessage;
    }


    interface Callback {
        void jobReceived(Job job);
    }

    private class AllJobsHandler implements InitiatorHandler {

        public void handleAgree(AclMessage agree) {
        }


        public void handleRefuse(AclMessage refuse) {
        }


        public void handleInform(AclMessage inform) {
            for (Job job : xmlCodec.xmlToJobs(inform.getContent())) {
                callback.jobReceived(job);
            }
        }


        public void handleFailure(AclMessage failure) {
        }


        public void handleOutOfSequence(AclMessage outOfSequenceMessage) {
        }


        public void handleNotUnderstood(AclMessage notUnderstoodMessage) {
        }
    }
}
