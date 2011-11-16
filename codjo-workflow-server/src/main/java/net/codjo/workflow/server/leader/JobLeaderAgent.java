/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.server.leader;
import net.codjo.agent.Agent;
import net.codjo.agent.DFService;
import net.codjo.agent.DFService.DFServiceException;
import net.codjo.agent.MessageTemplate;
import net.codjo.agent.protocol.SubscribeFailureBehaviour;
import net.codjo.agent.protocol.SubscribeParticipant;
import net.codjo.agent.protocol.SubscribeProtocol;
import net.codjo.workflow.common.Service;
import net.codjo.workflow.server.audit.AuditDao;
import org.apache.log4j.Logger;
/**
 * Agent organisant l'execution de 'Job' dans le système multi-agents. Cet agent est unique sur la plateforme.
 * Il participe à deux protocoles :
 *
 * <ol>
 *
 * <li> le protocole 'fipa-subscribe' (en tant que 'Participant') diffuse l'ensemble des évènements liés à
 * l'exécution de 'Job', </li>
 *
 * <li> le protocole 'workflow-job-protocol' (en tant que proxy) réceptionne des 'Job' (via des JobRequest en
 * tant que 'Participant') et délègue leur exécution (en tant que 'Initiator') à des JobAgent. </li>
 *
 * </ol>
 *
 * @see net.codjo.workflow.common.protocol.JobProtocol
 */
public class JobLeaderAgent extends Agent {
    static final Logger LOGGER = Logger.getLogger(JobLeaderAgent.class);

    private final AuditDao auditDao;
    private final JobLeaderSubscribeHandler subscription;


    public JobLeaderAgent(AuditDao auditDao, JobLeaderSubscribeHandler subscription) {
        this.auditDao = auditDao;
        this.subscription = subscription;
    }


    @Override
    protected void setup() {
        try {
            DFService.register(this, DFService.createAgentDescription(Service.JOB_LEADER_SERVICE));
        }
        catch (DFServiceException exception) {
            LOGGER.error("Impossible de s'enregistrer auprès du DF " + getClass(), exception);
            die();
        }

        startSubscriptionMechanism();
        addBehaviour(new JobLeaderBehaviour(auditDao, subscription, LOGGER));
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


    private void startSubscriptionMechanism() {
        MessageTemplate subscribeTemplate = MessageTemplate.matchProtocol(SubscribeProtocol.ID);
        addBehaviour(new SubscribeParticipant(this, subscription, subscribeTemplate));
        addBehaviour(new SubscribeFailureBehaviour(subscription));
    }
}
