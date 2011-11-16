package net.codjo.workflow.server.api;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.agent.DFService;
import net.codjo.agent.DFService.AgentDescription;
import net.codjo.agent.protocol.ContractNetParticipant;
import net.codjo.agent.protocol.FailureException;
import net.codjo.agent.protocol.NotUnderstoodException;
import net.codjo.agent.protocol.RefuseException;
import net.codjo.agent.util.IdUtil;
import net.codjo.workflow.common.message.JobContract;
import net.codjo.workflow.common.message.JobContractResultOntology;
import net.codjo.workflow.common.protocol.JobProtocolParticipant.SkillLevel;
import org.apache.log4j.Logger;
/**
 *
 */
public class ResourcesManagerAgent extends Agent {
    private static final Logger LOGGER = Logger.getLogger(ResourcesManagerAgent.class);
    private AgentFactory agentFactory;
    private AgentDescription agentDescription;


    public ResourcesManagerAgent(AgentFactory agentFactory, AgentDescription agentDescription) {
        this.agentFactory = agentFactory;
        this.agentDescription = agentDescription;
    }


    @Override
    protected void setup() {
        addBehaviour(new ContractNetParticipant(this, new MyCFPAdapter()));
        try {
            DFService.register(this, agentDescription);
        }
        catch (Exception exception) {
            LOGGER.error("Impossible de s'enregistrer auprès du DF " + getClass(), exception);
            die();
        }
    }


    @Override
    protected void tearDown() {
        try {
            DFService.deregister(this);
        }
        catch (DFService.DFServiceException exception) {
            LOGGER.error("Impossible de s'enlever auprès du DF " + getClass(), exception);
        }
    }


    private class MyCFPAdapter implements ContractNetParticipant.Handler {
        public AclMessage prepareResponse(AclMessage cfp) throws NotUnderstoodException, RefuseException {
            try {
                AclMessage propose = cfp.createReply(AclMessage.Performative.PROPOSE);
                propose.setContent(Integer.toString(SkillLevel.UNSKILLED.getLevel()));
                return propose;
            }
            catch (Throwable e) {
                LOGGER.error("Erreur lors de l'appel '"
                             + getClass().getSimpleName()
                             + ".acceptContract(...)' : " + cfp.getContent(), e);
                throw new NotUnderstoodException(e.getClass().getName() + " - " + e.getLocalizedMessage());
            }
        }


        public void handleRejectProposal(AclMessage cfp, AclMessage propose, AclMessage rejectProposal) {
        }


        public AclMessage prepareResultNotification(AclMessage cfp, AclMessage propose, AclMessage accept)
              throws FailureException {
            try {
                JobAgent agent = agentFactory.create();
                String nickName = String.format("%s-delegate-agent-%s",
                                                ((JobContract)cfp.getContentObject()).getRequest().getType(),
                                                IdUtil.createUniqueId(agent));

                getAgentContainer().acceptNewAgent(nickName, agent).start();
                AclMessage result = accept.createReply(AclMessage.Performative.INFORM);
                JobContractResultOntology.acceptAndDelegate(result, nickName);

                return result;
            }
            catch (Exception e) {
                throw new FailureException("Delegation en erreur : " + e.getLocalizedMessage());
            }
        }


        public void handleOutOfSequence(AclMessage cfp, AclMessage propose, AclMessage outOfSequenceMsg) {
        }
    }

    public static interface AgentFactory {
        JobAgent create() throws Exception;
    }
}
