package net.codjo.workflow.server.api;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.agent.DFService;
import net.codjo.agent.protocol.ContractNetParticipant;
import net.codjo.agent.protocol.FailureException;
import net.codjo.agent.protocol.NotUnderstoodException;
import net.codjo.agent.protocol.RefuseException;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.ScheduleContract;
import net.codjo.workflow.server.plugin.WorkflowServerPlugin;
import org.apache.log4j.Logger;
/**
 *
 */
public class ScheduleAgent extends Agent {
    private static final Logger LOG = Logger.getLogger(ScheduleAgent.class);
    private final Handler handler;


    public ScheduleAgent(Handler handler) {
        this.handler = handler;
    }


    @Override
    protected final void setup() {
        try {
            DFService.register(this, createDescription());
        }
        catch (Exception exception) {
            LOG.error("Impossible de s'enregistrer auprès du DF " + getClass(), exception);
            die();
            return;
        }

        addBehaviour(new ContractNetParticipant(this, new ContractNetAdapter()));
    }


    @Override
    protected final void tearDown() {
        try {
            DFService.deregister(this);
        }
        catch (DFService.DFServiceException exception) {
            LOG.error("Impossible de s'enlever auprès du DF " + getClass(), exception);
        }
    }


    private DFService.AgentDescription createDescription() {
        String simpleName = getClass().getName();
        simpleName = simpleName.substring(simpleName.lastIndexOf('.') + 1, simpleName.length());
        return new DFService.AgentDescription(
              new DFService.ServiceDescription(WorkflowServerPlugin.WORKFLOW_SCHEDULE_SERVICE,
                                               "scheduler-" + simpleName));
    }


    public interface Handler {
        public ScheduleAgent.KnowledgeLevel getKnowledgeLevel();


        public boolean acceptContract(ScheduleContract contract);


        public JobRequest createNextRequest(ScheduleContract contract);


        public AclMessage getMessage();


        public void setMessage(AclMessage message);
    }

    public static abstract class AbstractHandler implements Handler {
        private KnowledgeLevel knowledgeLevel;
        private AclMessage message;


        protected AbstractHandler() {
            this(KnowledgeLevel.DEFAULT);
        }


        protected AbstractHandler(KnowledgeLevel knowledgeLevel) {
            this.knowledgeLevel = knowledgeLevel;
        }


        public ScheduleAgent.KnowledgeLevel getKnowledgeLevel() {
            return knowledgeLevel;
        }


        public AclMessage getMessage() {
            return message;
        }


        public void setMessage(AclMessage message) {
            this.message = message;
        }
    }

    public static class KnowledgeLevel {
        private int level;
        private final String label;
        public static final KnowledgeLevel EXPERT = new KnowledgeLevel(10, "expert");
        public static final KnowledgeLevel DEFAULT = new KnowledgeLevel(5, "default");
        public static final KnowledgeLevel ROOKIE = new KnowledgeLevel(0, "rookie");


        KnowledgeLevel(int value, String label) {
            this.level = value;
            this.label = label;
        }


        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            //noinspection SimplifiableIfStatement
            if (object == null || getClass() != object.getClass()) {
                return false;
            }

            return level == ((KnowledgeLevel)object).level;
        }


        @Override
        public int hashCode() {
            return level;
        }


        @Override
        public String toString() {
            return label;
        }


        public int getLevel() {
            return level;
        }
    }

    private class ContractNetAdapter implements ContractNetParticipant.Handler {
        public AclMessage prepareResponse(AclMessage cfp) throws NotUnderstoodException, RefuseException {
            try {
                ScheduleContract contract = (ScheduleContract)cfp.getContentObject();
                handler.setMessage(cfp);

                if (!handler.acceptContract(contract)) {
                    throw new RefuseException("refuse-contract");
                }
                AclMessage propose = cfp.createReply(AclMessage.Performative.PROPOSE);
                propose.setContent(Integer.toString(handler.getKnowledgeLevel().getLevel()));
                return propose;
            }
            catch (RefuseException e) {
                throw e;
            }
            catch (ClassCastException e) {
                LOG.error("Erreur interne : un contrat était attendu (ClassCastException)", e);
                throw new NotUnderstoodException("ClassCastException");
            }
            catch (Throwable e) {
                LOG.error("Erreur lors de l'appel '" + handler.getClass().getSimpleName()
                          + ".acceptContract(...)' : " + cfp.getContentObject(), e);
                throw new NotUnderstoodException(e.getClass().getName() + " - " + e.getLocalizedMessage());
            }
        }


        public void handleRejectProposal(AclMessage cfp, AclMessage propose, AclMessage rejectProposal) {
        }


        public AclMessage prepareResultNotification(AclMessage cfp, AclMessage propose, AclMessage accept)
              throws FailureException {

            try {
                ScheduleContract contract = (ScheduleContract)cfp.getContentObject();
                handler.setMessage(cfp);

                AclMessage result = accept.createReply(AclMessage.Performative.INFORM);
                JobRequest nextRequest = handler.createNextRequest(contract);
                nextRequest.initializeFromPreviousRequest(contract.getRequest());
                result.setContentObject(nextRequest);
                return result;
            }
            catch (Throwable e) {
                LOG.error("Erreur lors du createNextRequest : " + cfp.getContentObject(), e);
                throw new FailureException(e.getClass().getName() + " - " + e.getLocalizedMessage());
            }
        }


        public void handleOutOfSequence(AclMessage cfp, AclMessage propose, AclMessage outOfSequenceMsg) {
        }
    }
}
