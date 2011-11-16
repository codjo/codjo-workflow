/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.schedule;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.AgentController;
import net.codjo.agent.Aid;
import net.codjo.agent.BadControllerException;
import net.codjo.agent.ContainerFailureException;
import net.codjo.agent.DFService;
import net.codjo.agent.DFService.DFServiceException;
import net.codjo.agent.UserId;
import net.codjo.agent.util.IdUtil;
import net.codjo.workflow.common.Service;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.ScheduleContract;
import net.codjo.workflow.common.protocol.JobProtocolInitiator;
import net.codjo.workflow.common.subscribe.JobEventHandler;
import net.codjo.workflow.common.subscribe.ProtocolErrorEvent;
import net.codjo.workflow.common.util.SelectBestProposalHandler;
/**
 * Agent demandant l'execution de 'Job' dans le système multi-agents. Il participe au protocole
 * 'workflow-job-protocol' en tant que 'Initiator', il envoie sa demande au JobLeaderAgent.
 *
 * @see net.codjo.workflow.common.protocol.JobProtocolInitiator
 */
public class ScheduleLeaderAgent extends Agent {
    public static final String WORKFLOW_SCHEDULE_SERVICE = "workflow-sequence";
    private static final String SCHEDULE_LEADER_NAME_PART = "-leader-";
    private final JobRequest initialRequest;
    private final JobEventHandler eventHandler;
    private final UserId userId;
    private final WorkflowConfiguration workflowConfiguration;
    private final Object mutex = new Object();
    private ScheduleContract lastContract;
    private boolean isDead = false;


    public ScheduleLeaderAgent(JobRequest jobRequest,
                               JobEventHandler jobEventHandler,
                               UserId userId,
                               WorkflowConfiguration workflowConfiguration) {
        this.initialRequest = jobRequest;
        this.eventHandler = jobEventHandler;
        this.userId = userId;
        this.workflowConfiguration = workflowConfiguration;
    }


    public String createNickName(String workName) {
        StringBuilder leaderAgentId = new StringBuilder(workName).append(SCHEDULE_LEADER_NAME_PART);
        leaderAgentId.append(IdUtil.createUniqueId(this));
        return leaderAgentId.toString();
    }


    @Override
    protected void setup() {
        startJobProtocol(initialRequest);
    }


    @Override
    protected void tearDown() {
        synchronized (mutex) {
            isDead = true;
            mutex.notifyAll();
        }
    }


    public static void waitUntilFinished(String nickName,
                                         ScheduleLeaderAgent agent,
                                         AgentContainer container,
                                         WorkflowConfiguration workflowConfiguration)
          throws ContainerFailureException {
        AgentController agentController = container.acceptNewAgent(nickName, agent);
        agentController.start();
        agent.waitForDeath(workflowConfiguration.getDefaultTimeout());

        if (!isDead(agent)) {
            silentKill(agentController);
        }
    }


    private void startJobProtocol(JobRequest request) {
        Aid organiserAid;
        try {
            organiserAid = DFService.searchFirstAgentWithService(this, Service.ORGANISER_SERVICE);
        }
        catch (DFServiceException e) {
            throw new RuntimeException("Impossible de trouver un agent de type " + Service.ORGANISER_SERVICE);
        }

        addBehaviour(new MyJobProtocolInitiator(request,
                                                organiserAid,
                                                userId,
                                                workflowConfiguration.getDefaultReplyTimeout()));
        eventHandler.receive(new JobEvent(request));
    }


    private static boolean isDead(ScheduleLeaderAgent agent) {
        return agent.isDead;
    }


    private static void silentKill(AgentController agentController) {
        try {
            agentController.kill();
        }
        catch (BadControllerException e) {
            ;
        }
    }


    private void waitForDeath(long timeout) {
        synchronized (mutex) {
            if (isDead) {
                return;
            }
            try {
                mutex.wait(timeout);
            }
            catch (InterruptedException e) {
                ;
            }
        }
    }


    private class MyJobProtocolInitiator extends JobProtocolInitiator {

        MyJobProtocolInitiator(JobRequest jobRequest, Aid aid, UserId userId, long replyTimeout) {
            super(jobRequest, aid, userId, replyTimeout);
        }


        @Override
        protected void handleInform(AclMessage inform) {
            JobAudit audit = (JobAudit)inform.getContentObject();
            eventHandler.receive(new JobEvent(audit));

            if (hasError(audit)) {
                getAgent().die();
            }
            else if (JobAudit.Type.POST == audit.getType()) {

                AclMessage cfp = new AclMessage(AclMessage.Performative.CFP);
                cfp.setContentObject(createScheduleNextRequestContract(audit));
                cfp.encodeUserId(inform.decodeUserId());

                new DetermineNextStepHandler().selectBestProposal(WORKFLOW_SCHEDULE_SERVICE, cfp, getAgent());
            }
        }


        @Override
        protected void handleFailure(AclMessage failure) {
            eventHandler.receiveError(new ProtocolErrorEvent(ProtocolErrorEvent.Type.FAILURE, failure));
            getAgent().die();
        }


        private boolean hasError(JobAudit audit) {
            return audit.hasError() && (JobAudit.Type.MID != audit.getType());
        }


        private ScheduleContract createScheduleNextRequestContract(JobAudit audit) {
            ScheduleContract contract = new ScheduleContract(getJobRequest(), audit);
            contract.setPreviousContract(lastContract);
            lastContract = contract;
            return contract;
        }
    }

    private class DetermineNextStepHandler extends SelectBestProposalHandler {

        @Override
        public void handleNoBestProposal() {
            ScheduleLeaderAgent.this.die();
        }


        @Override
        public void handleTechnicalError(Exception error) {
            ScheduleLeaderAgent.this.die();
        }


        @Override
        public void handleBestProposal(AclMessage aclMessage) {
            startJobProtocol((JobRequest)aclMessage.getContentObject());
        }
    }
}
