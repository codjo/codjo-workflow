/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.protocol;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Behaviour;
import net.codjo.agent.JadeWrapper;
import net.codjo.agent.MessageTemplate;
import net.codjo.agent.behaviour.CyclicBehaviour;
import net.codjo.agent.behaviour.OneShotBehaviour;
import net.codjo.agent.protocol.ContractNetParticipant;
import net.codjo.agent.protocol.FailureException;
import net.codjo.agent.protocol.NotUnderstoodException;
import net.codjo.agent.protocol.RefuseException;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobAudit.Type;
import net.codjo.workflow.common.message.JobContract;
import net.codjo.workflow.common.message.JobContractResultOntology;
import net.codjo.workflow.common.message.JobException;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.util.ParticipantUtil;
import net.codjo.workflow.common.util.WorkflowLogUtil;
import jade.core.behaviours.ThreadedBehaviourFactory;
import org.apache.log4j.Logger;
/**
 * Behaviour 'Participant' dans le protocole job.
 *
 * @see JobProtocol
 */
public class JobProtocolParticipant extends CyclicBehaviour {
    protected final Logger logger = Logger.getLogger(getClass());
    private ParticipantUtil participantUtil = new ParticipantUtil(this);
    private MessageTemplate requestTemplate;
    private State state = State.INITIALIZE_CFP;
    private Behaviour executeJobBehaviour = new DefaultExecuteJobBehaviour();
    private ThreadedBehaviourFactory behaviourFactory = new ThreadedBehaviourFactory();


    public JobProtocolParticipant() {
        requestTemplate = ParticipantUtil.createRequestTemplate();
    }


    protected SkillLevel getSkillLevel() {
        return SkillLevel.DEFAULT;
    }


    protected boolean acceptContract(JobContract contract) {
        return state == State.WAIT_FOR_REQUEST;
    }


    public void setRequestMessage(AclMessage requestMessage) {
        participantUtil.setRequestMessage(requestMessage);
        logRequest();
    }


    public AclMessage getRequestMessage() {
        return participantUtil.getRequestMessage();
    }


    public JobRequest getRequest() {
        return participantUtil.getJobRequest();
    }


    public void setExecuteJobBehaviour(Behaviour executeJobBehaviour) {
        if (executeJobBehaviour == null) {
            throw new IllegalArgumentException("Behaviour null !");
        }
        this.executeJobBehaviour = executeJobBehaviour;
    }


    public Behaviour getExecuteJobBehaviour() {
        return executeJobBehaviour;
    }


    public void declareJobDone() {
        declareJobDone(null);
    }


    public void declareJobDone(JobException failure) {
        state = State.SEND_POST;
        try {
            handlePOST(participantUtil.getJobRequest(), failure);
        }
        finally {
            state = State.WAIT_FOR_REQUEST;
        }
    }


    @SuppressWarnings({"UnusedDeclaration"})
    protected JobAudit createPreAudit(JobRequest request) {
        return new JobAudit(Type.PRE);
    }


    @SuppressWarnings({"UnusedDeclaration"})
    protected JobAudit createPostAudit(JobRequest request, JobException failure) {
        JobAudit audit = new JobAudit(Type.POST);
        if (failure != null) {
            audit.setError(new JobAudit.Anomaly(failure.getMessage(), failure));
        }
        return audit;
    }


    protected void handlePRE(JobRequest request) {
        sendAudit(createPreAudit(request));
    }


    protected void handlePOST(JobRequest request, JobException failure) {
        sendAudit(createPostAudit(request, failure));
    }


    protected void handleREFUSE(AclMessage message) {
        AclMessage refuse = message.createReply(AclMessage.Performative.REFUSE);
        getAgent().send(refuse);
    }


    public void sendAudit(JobAudit audit) {
        logAudit(audit);

        if (audit.hasError() && state == State.SEND_PRE) {
            state = State.WAIT_FOR_REQUEST;
        }
        participantUtil.sendAudit(audit);
    }


    protected void executeJob(JobRequest request) throws JobException {
    }


    @Override
    protected final void action() {
        switch (state) {
            case INITIALIZE_CFP:
                getAgent().addBehaviour(new ContractNetParticipant(getAgent(), new MyCFPAdapter()));
                state = State.WAIT_FOR_REQUEST;
                break;
            case WAIT_FOR_REQUEST:
                participantUtil.setRequestMessage(null);
                receiveRequest();
                break;
            case SEND_PRE:
                handlePRE(participantUtil.getJobRequest());
                if (State.SEND_PRE == state) {
                    state = State.POST_JOB;
                }
                break;
            case POST_JOB:
                state = State.JOB_RUNNING;
                addExecuteJobBehaviour();
                break;
            case JOB_RUNNING:
                AclMessage message = getAgent().receive(requestTemplate);
                if (message == null) {
                    block();
                    return;
                }
                handleREFUSE(message);
                break;
            case SEND_POST:
                break;
        }
    }


    private void addExecuteJobBehaviour() {
        JadeWrapper.unwrapp(getAgent())
              .addBehaviour(behaviourFactory.wrap(JadeWrapper.unwrapp(executeJobBehaviour)));
    }


    private void receiveRequest() {
        AclMessage message = getAgent().receive(requestTemplate);

        if (message == null) {
            block();
            return;
        }
        setRequestMessage(message);
        state = State.SEND_PRE;
    }


    private void logRequest() {
        if (!logger.isInfoEnabled()) {
            return;
        }
        JobRequest jobToExecute = participantUtil.getJobRequest();

        if (jobToExecute == null) {
            logger.error("Requête 'non recue'");
            return;
        }
        String arguments = "";
        if (jobToExecute.getArguments() != null) {
            arguments = " - " + jobToExecute.getArguments().encode().trim();
        }
        WorkflowLogUtil.logIfNeeded(logger, jobToExecute,
                                    String.format("(%s) Requête de %s%s",
                                                  jobToExecute.getId(),
                                                  jobToExecute.getInitiatorLogin(),
                                                  arguments));
    }


    private void logAudit(JobAudit audit) {
        if (!logger.isInfoEnabled()) {
            return;
        }

        String arguments = null;
        if (audit.getArguments() != null) {
            arguments = audit.getArguments().encode().trim();
            if (arguments.length() > 128) {
                arguments = arguments.substring(0, 125) + "...";
            }
        }
        String msg = String.format("(%s) Audit %s%s",
                                   participantUtil.getJobRequest().getId(),
                                   audit.getType(),
                                   arguments == null ? "" : " / " + arguments);
        if (audit.hasError()) {
            logger.error(msg + " - " + audit.getErrorMessage());
        }
        else if (audit.getType() != Type.MID) {
            WorkflowLogUtil.logIfNeeded(logger, participantUtil.getJobRequest(), msg);
        }
        else {
            logger.debug(msg);
        }
    }


    /**
     * Behaviour de Job par défaut qui délègue l'exécution du travail à la méthode {@link
     * JobProtocolParticipant#executeJobBehaviour}.
     */
    private class DefaultExecuteJobBehaviour extends OneShotBehaviour {
        @Override
        protected void action() {
            try {
                executeJob(participantUtil.getJobRequest());
                declareJobDone();
            }
            catch (JobException jobException) {
                declareJobDone(jobException);
            }
            catch (Throwable throwable) {
                //noinspection ThrowableInstanceNeverThrown
                declareJobDone(new JobException(throwable.getMessage(), throwable));
            }
        }
    }

    /**
     * Enum décrivant le cycle du participant.
     */
    private static enum State {
        INITIALIZE_CFP,
        WAIT_FOR_REQUEST,
        SEND_PRE,
        SEND_POST,
        POST_JOB,
        JOB_RUNNING
    }

    /**
     * Enum décrivant le niveau de compétence de l'agent.
     */
    public static enum SkillLevel {
        EXPERT(15),
        DEFAULT(10),
        ROOKIE(5),
        UNSKILLED(0);
        private int level;


        SkillLevel(int level) {
            this.level = level;
        }


        public int getLevel() {
            return level;
        }
    }

    private class MyCFPAdapter implements ContractNetParticipant.Handler {
        public AclMessage prepareResponse(AclMessage cfp) throws
                                                          NotUnderstoodException, RefuseException {
            try {
                JobContract contract = (JobContract)cfp.getContentObject();
                if (!acceptContract(contract)) {
                    throw new RefuseException("refuse-contract");
                }
                AclMessage propose = cfp.createReply(AclMessage.Performative.PROPOSE);
                propose.setContent(Integer.toString(getSkillLevel().getLevel()));
                return propose;
            }
            catch (RefuseException e) {
                throw e;
            }
            catch (ClassCastException e) {
                logger.error("Erreur interne : un contrat était attendu (ClassCastException)", e);
                throw new NotUnderstoodException("ClassCastException");
            }
            catch (Throwable e) {
                logger.error("Erreur lors de l'appel '"
                             + JobProtocolParticipant.this.getClass().getSimpleName()
                             + ".acceptContract(...)' : " + cfp.getContent(), e);
                throw new NotUnderstoodException(e.getClass().getName() + " - " + e.getLocalizedMessage());
            }
        }


        public void handleRejectProposal(AclMessage cfp, AclMessage propose, AclMessage rejectProposal) {
        }


        public AclMessage prepareResultNotification(AclMessage cfp, AclMessage propose, AclMessage accept)
              throws FailureException {
            AclMessage result = accept.createReply(AclMessage.Performative.INFORM);
            JobContractResultOntology.accept(result);
            return result;
        }


        public void handleOutOfSequence(AclMessage cfp, AclMessage propose, AclMessage outOfSequenceMsg) {
        }
    }
}


