package net.codjo.workflow.common.util;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.agent.DFService;
import net.codjo.agent.protocol.ContractNetInitiator;
import net.codjo.agent.protocol.ContractNetProtocol;
import java.util.Date;
import java.util.List;
/**
 *
 */
public abstract class SelectBestProposalHandler extends ContractNetInitiator.AbstractHandler {
    private static final int CFP_TIMEOUT = 30000;


    public void selectBestProposal(String service, AclMessage cfp, Agent agent) {
        try {
            cfp.setProtocol(ContractNetProtocol.ID);
            cfp.setReplyByDate(new Date(System.currentTimeMillis() + CFP_TIMEOUT));

            DFService.AgentDescription[] agentDescriptions = DFService.searchForService(agent, service);

            if (agentDescriptions.length == 0) {
                handleNoBestProposal();
                return;
            }
            for (DFService.AgentDescription agentDescription : agentDescriptions) {
                cfp.addReceiver(agentDescription.getAID());
            }

            agent.addBehaviour(new ContractNetInitiator(agent, this, cfp));
        }
        catch (Exception e) {
            handleTechnicalError(e);
        }
    }


    public abstract void handleBestProposal(AclMessage aclMessage);


    public abstract void handleNoBestProposal();


    public abstract void handleTechnicalError(Exception error);


    @Override
    public final void handleAllResponses(List<AclMessage> responses,
                                         ContractNetInitiator.Acceptances acceptances) {
        int trustLevel = -1;
        AclMessage bestProposal = null;

        for (AclMessage response : responses) {
            if (AclMessage.Performative.PROPOSE.equals(response.getPerformative())) {
                Integer responseTrustLevel = Integer.decode(response.getContent());
                if (trustLevel < responseTrustLevel) {
                    if (bestProposal != null) {
                        acceptances.rejectProposal(bestProposal);
                    }

                    bestProposal = response;
                    trustLevel = responseTrustLevel;
                }
                else {
                    acceptances.rejectProposal(response);
                }
            }
        }
        if (bestProposal == null) {
            handleNoBestProposal();
        }
        else {
            acceptances.acceptProposal(bestProposal);
        }
    }


    @Override
    public final void handleInform(AclMessage aclMessage) {
        handleBestProposal(aclMessage);
    }
}
