package net.codjo.workflow.server.leader;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.agent.Aid;
import net.codjo.agent.Behaviour;
import net.codjo.agent.MessageTemplate;
import net.codjo.agent.behaviour.CyclicBehaviour;
import net.codjo.agent.behaviour.OneShotBehaviour;
import net.codjo.workflow.common.message.JobAudit;
import static net.codjo.workflow.common.message.JobAudit.Status;
import static net.codjo.workflow.common.message.JobAudit.Type;
import static net.codjo.workflow.common.message.JobAudit.createAudit;
import net.codjo.workflow.common.message.JobContract;
import static net.codjo.workflow.common.message.JobContractResultOntology.extractDelegate;
import static net.codjo.workflow.common.message.JobContractResultOntology.isDelegate;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.util.ParticipantUtil;
import net.codjo.workflow.common.util.SelectBestProposalHandler;
import net.codjo.workflow.common.util.WorkflowLogUtil;
import net.codjo.workflow.server.audit.AuditDao;
import java.util.Date;
import org.apache.log4j.Logger;

class JobLeaderBehaviour extends OneShotBehaviour {
    private final AuditDao auditDao;
    private final JobLeaderSubscribeHandler subscription;
    private Logger logger;


    JobLeaderBehaviour(AuditDao auditDao, JobLeaderSubscribeHandler subscription, Logger logger) {
        this.auditDao = auditDao;
        this.subscription = subscription;
        this.logger = logger;
    }


    @Override
    protected void action() {
        logger.info("Activation de l'ecoute de nouvelle requete");
        getAgent().addBehaviour(new ListenRequestStep());
    }


    private class ListenRequestStep extends CyclicBehaviour {
        private MessageTemplate requestTemplate = ParticipantUtil.createRequestTemplate();


        @Override
        protected void action() {
            AclMessage message = getAgent().receive(requestTemplate);
            if (message == null) {
                block();
                return;
            }

            JobLeaderContext context = new JobLeaderContext(this, message);

            WorkflowLogUtil.logIfNeeded(logger, context.getJobRequest(),
                                        "(" + context.getRequestMessage().getConversationId()
                                        + ") Reception par " + context.getGroupName());

            context.sendRequestNotification();
            Date replyByDate = message.getReplyByDate();
            if (replyByDate != null && replyByDate.compareTo(new Date()) <= 0) {
                logger.warn("Abandon de la requete obsolete suivante : " + context.getJobRequest());
                context.sendAudit(
                      createAudit(Type.PRE,
                                  Status.ERROR,
                                  "Abandon de la requete car le delai d'attente a ete trop long."));
                return;
            }

            context.startRecruiting();
        }
    }

    private class RecruitStep extends SelectBestProposalHandler {
        private Agent agent;
        private JobLeaderContext context;


        private RecruitStep(Agent agent, JobLeaderContext context) {
            this.agent = agent;
            this.context = context;
        }


        @Override
        public void handleBestProposal(AclMessage proposal) {
            Aid recruitedAgent = proposal.getSender();

            if (isDelegate(proposal)) {
                recruitedAgent = new Aid(extractDelegate(proposal));
            }

            WorkflowLogUtil.logIfNeeded(
                  logger, context.getJobRequest(),
                  "Le jobAgent '" + recruitedAgent + "' est recruté pour la tache "
                  + context.getJobRequest().getType() + " par " + context.getGroupName());
            agent.addBehaviour(new ProxyStep(recruitedAgent, agent, context));
        }


        @Override
        public void handleNoBestProposal() {
            logger.warn(context.getGroupName() + " ne trouve pas d'agent pour " + context.getJobRequest()
                  .getType());
            context.recruitementFailed(
                  "Impossible de trouver un agent capable de réaliser la requête "
                  + context.getJobRequest().getType());
        }


        @Override
        public void handleTechnicalError(Exception error) {
            context.recruitementFailed(
                  "Erreur technique lors de la selection d'un JobAgent : "
                  + error.getLocalizedMessage(),
                  error);
        }
    }

    private class ProxyStep extends CyclicBehaviour {
        private final Aid recruitedAgent;
        private MessageTemplate proxyTemplate;
        private JobLeaderContext context;
        private int tryCount = 0;


        ProxyStep(Aid recruitedAgent, Agent agent, JobLeaderContext context) {
            this.recruitedAgent = recruitedAgent;
            this.context = context;
            setBehaviourName(context.getGroupName());
            WorkflowLogUtil.logIfNeeded(logger, context.getJobRequest(),
                                        "Mise en place du proxy " + getBehaviourName());
            // forward request
            AclMessage messageToJobAgent = context.getRequestMessage().createReply();
            messageToJobAgent.setContentObject(context.getJobRequest());
            messageToJobAgent.clearReceivers();
            messageToJobAgent.addReceiver(recruitedAgent);
            messageToJobAgent.setReplyByDate(context.getRequestMessage().getReplyByDate());
            agent.send(messageToJobAgent);

            proxyTemplate = context.createListenParticipantTemplate();
        }


        @Override
        protected void action() {
            AclMessage message = getAgent().receive(proxyTemplate);

            if (message == null) {
                block();
                return;
            }

            if (AclMessage.Performative.REFUSE == message.getPerformative()) {
                getAgent().removeBehaviour(this);
                if (++tryCount >= 10) {
                    logger.warn("Le jobAgent '" + recruitedAgent + "' refuse son contrat. "
                                + "On se met en mode echec (trop de tentative)");
                    context.recruitementFailed(
                          "Les agents capable de realiser la tache se mette systematiquement en "
                          + "refus d'execution (serveur probablement en surcharge)");
                }
                else {
                    logger.warn("Le jobAgent '" + recruitedAgent + "' refuse son contrat. "
                                + "On retente un recrutement : tentative " + tryCount);
                    context.startRecruiting();
                }
                return;
            }

            JobAudit audit = (JobAudit)message.getContentObject();
            if (logger.isDebugEnabled()) {
                logMessage(message, audit);
            }
            context.sendAudit(audit);

            if (audit.hasError() || Type.POST == audit.getType()) {
                getAgent().removeBehaviour(this);
            }
        }


        private void logMessage(AclMessage message, JobAudit audit) {
            logger.debug(context.getGroupName() + " forward l'audit " + audit
                         + "\n       \t convId -> " + message.getConversationId()
                         + "\n       \t from -> " + message.getSender()
                         + "\n       \t to -> " + context.getRequestMessage().getSender());
        }
    }

    class JobLeaderContext {
        private final ParticipantUtil participantUtil;
        private final String groupName;


        JobLeaderContext(Behaviour behaviour, AclMessage requestMessage) {
            this.participantUtil = new ParticipantUtil(behaviour, requestMessage);
            this.groupName = "job-lead-for-" + getRequestMessage().getSender().getLocalName();
        }


        public String getGroupName() {
            return groupName;
        }


        public JobRequest getJobRequest() {
            return participantUtil.getJobRequest();
        }


        public AclMessage getRequestMessage() {
            return participantUtil.getRequestMessage();
        }


        public MessageTemplate createListenParticipantTemplate() {
            return participantUtil.createListenParticipantTemplate();
        }


        public void startRecruiting() {
            AclMessage cfp = new AclMessage(AclMessage.Performative.CFP);
            cfp.setContentObject(new JobContract(getJobRequest()));

            new RecruitStep(getAgent(), this)
                  .selectBestProposal(getJobRequest().getType(), cfp, getAgent());
        }


        public void recruitementFailed(String errorMessage) {
            recruitementFailed(errorMessage, null);
        }


        public void recruitementFailed(String errorMessage, Throwable error) {
            logger.warn(errorMessage, error);
            sendAudit(createAudit(JobAudit.Type.PRE, JobAudit.Status.ERROR, errorMessage, error));
        }


        public void sendRequestNotification() {
            JobRequest request = getJobRequest();

            try {
                auditDao.saveRequest(getAgent(), getRequestMessage(), request);
            }
            catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde de la requête '" + request + "'", e);
            }
        }


        public void sendAudit(JobAudit audit) {
            participantUtil.sendAudit(audit);

            if (audit.getType() != Type.MID) {
                try {
                    auditDao.saveAudit(getAgent(), getRequestMessage(), audit);
                }
                catch (Exception e) {
                    logger.error("Erreur lors de la sauvegarde de l'audit '" + audit + "'", e);
                }
            }
            subscription.sendNotification(new JobEvent(audit));
        }
    }
}
