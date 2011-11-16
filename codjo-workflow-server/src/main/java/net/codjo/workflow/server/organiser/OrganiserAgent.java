package net.codjo.workflow.server.organiser;
import net.codjo.agent.AclMessage;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Agent;
import net.codjo.agent.Aid;
import net.codjo.agent.DFService;
import net.codjo.agent.DFService.DFServiceException;
import static net.codjo.agent.MessageTemplate.and;
import static net.codjo.agent.MessageTemplate.matchPerformative;
import static net.codjo.agent.MessageTemplate.matchProtocol;
import net.codjo.agent.behaviour.CyclicBehaviour;
import net.codjo.agent.protocol.BasicQueryParticipantHandler;
import net.codjo.agent.protocol.DefaultSubscribeParticipantHandler;
import net.codjo.agent.protocol.RequestParticipant;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.agent.protocol.SubscribeFailureBehaviour;
import net.codjo.agent.protocol.SubscribeParticipant;
import net.codjo.agent.protocol.SubscribeProtocol;
import net.codjo.workflow.common.Service;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobAudit.Status;
import net.codjo.workflow.common.message.JobAudit.Type;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.Job.State;
import net.codjo.workflow.common.organiser.XmlCodec;
import net.codjo.workflow.common.protocol.JobProtocol;
import net.codjo.workflow.common.util.WorkflowLogUtil;
import net.codjo.workflow.server.leader.JobLeaderSubscribeHandler;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public class OrganiserAgent extends Agent {
    static final Logger LOGGER = Logger.getLogger(OrganiserAgent.class.getName());
    private static final int FORWARD_REQUEST_TIME_OFFSET = 10;

    private final RuleEngine ruleEngine;
    private final JobFactory jobFactory;
    private final List<String> jobFilter;
    private final XmlCodec xmlCodec;
    private final int maxFinishedJobs;
    private final JobLeaderSubscribeHandler jobLeaderSubscription;
    private final Map<Job, AclMessage> jobToMessage = new HashMap<Job, AclMessage>();
    private final Map<String, Job> requestIdToJob = new HashMap<String, Job>();
    private final List<Job> finishedJobs = new LinkedList<Job>();
    private final DefaultSubscribeParticipantHandler subscribeParticipantHandler
          = new DefaultSubscribeParticipantHandler();


    public OrganiserAgent(RuleEngine ruleEngine,
                          JobFactory jobFactory,
                          List<String> jobFilter,
                          XmlCodec xmlCodec,
                          int maxFinishedJobs,
                          JobLeaderSubscribeHandler jobLeaderSubscription) {
        this.ruleEngine = ruleEngine;
        this.jobFactory = jobFactory;
        this.jobFilter = jobFilter;
        this.xmlCodec = xmlCodec;
        this.maxFinishedJobs = maxFinishedJobs;
        this.jobLeaderSubscription = jobLeaderSubscription;
    }


    public String getAllJobs() {
        List<Job> allJobs = new ArrayList<Job>(finishedJobs);
        for (Job job : ruleEngine.getAllJobs()) {
            if (!jobFilter.contains(job.getType())) {
                allJobs.add(job);
            }
        }
        return xmlCodec.jobsToXml(allJobs);
    }


    @Override
    protected void setup() {
        try {
            DFService.register(this, DFService.createAgentDescription(Service.ORGANISER_SERVICE));
        }
        catch (Exception exception) {
            LOGGER.error("Impossible de s'enregistrer auprès du DF " + getClass(), exception);
            die();
            return;
        }

        try {
            ruleEngine.start();
        }
        catch (Exception e) {
            LOGGER.error("Impossible de démarrer le moteur de règle", e);
            die();
            return;
        }

        addBehaviour(new TransferRequestBehaviour());
        addBehaviour(new AuditListenerBehaviour());
        addBehaviour(new RequestParticipant(this,
                                            new BasicQueryParticipantHandler(this),
                                            and(matchProtocol(RequestProtocol.QUERY),
                                                matchPerformative(Performative.QUERY))));
        addBehaviour(new SubscribeParticipant(this,
                                              subscribeParticipantHandler,
                                              matchProtocol(SubscribeProtocol.ID)));
        addBehaviour(new SubscribeFailureBehaviour(subscribeParticipantHandler));
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


    private void handleNewRequest(AclMessage aclMessage) {
        JobRequest request = (JobRequest)aclMessage.getContentObject();
        jobLeaderSubscription.sendNotification(new JobEvent(request));
        Job job = null;
        try {
            job = jobFactory.createJob(request, aclMessage.decodeUserId());

            if (!jobFilter.contains(job.getType())) {
                subscribeParticipantHandler.sendInform(xmlCodec.jobsToXml(job));
            }

            ruleEngine.insert(job);

            jobToMessage.put(job, aclMessage);
        }
        catch (Exception e) {
            AclMessage response = aclMessage.createReply(Performative.REFUSE);
            response.setContentObject(JobAudit.createAudit(Type.PRE,
                                                           Status.ERROR,
                                                           "Impossible de traiter la requête.",
                                                           e));
            send(response);
        }
        finally {
            if (job != null) {
                fireJobModified(job);
            }
        }
    }


    private void handleRequestDone(JobAudit audit) {
        Job job = requestIdToJob.remove(audit.getRequestId());
        if (State.REJECTED != job.getState()) {
            if (audit.hasError()) {
                job.setErrorMessage(audit.getErrorMessage());
                job.setState(State.FAILURE);
            } else {
                job.setState(State.DONE);
            }
        }

        if (!jobFilter.contains(job.getType())) {
            finishedJobs.add(job);
            if (finishedJobs.size() > maxFinishedJobs) {
                finishedJobs.remove(0);
            }
        }

        ruleEngine.retract(job);

        fireJobModified(job);
    }


    private void fireJobModified(Job job) {
        List<Job> jobs = new ArrayList<Job>();
        for (Job runningJob : ruleEngine.getRunningJobs()) {
            AclMessage message = jobToMessage.remove(runningJob);
            if (message != null) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.SECOND, FORWARD_REQUEST_TIME_OFFSET);
                message.setReplyByDate(cal.getTime());

                JobRequest request = (JobRequest)message.getContentObject();
                requestIdToJob.put(request.getId(), runningJob);

                send(message);

                if (!jobFilter.contains(job.getType())) {
                    jobs.add(runningJob);
                }

                log(message, "Transfert de la requete");
            } //else job request was already transfered
        }
        for (Job rejectedJob : ruleEngine.getRejectedJobs()) {
            AclMessage message = jobToMessage.remove(rejectedJob);
            if (message != null) {
                if (!jobFilter.contains(rejectedJob.getType())) {
                    jobs.add(rejectedJob);
                }

                JobRequest request = (JobRequest)message.getContentObject();
                requestIdToJob.put(request.getId(), rejectedJob);
                JobAudit audit = JobAudit.createAudit(Type.PRE, Status.ERROR, "La requête a été rejetée.");
                audit.setRequestId(request.getId());
                AclMessage response = message.createReply(Performative.INFORM);
                response.setContentObject(audit);
                send(response);

                log(message, "Rejet de la requete");
            } //else job was already handled as rejected
        }

        if (job != null && !jobs.contains(job) && !jobFilter.contains(job.getType())) {
            jobs.add(job);
        }
        subscribeParticipantHandler.sendInform(xmlCodec.jobsToXml(jobs));
    }


    private void log(AclMessage aclMessage, String message) {
        if (LOGGER.isInfoEnabled()) {
            WorkflowLogUtil.logIfNeeded(LOGGER,
                                        (JobRequest)aclMessage.getContentObject(),
                                        String.format("(%s) %s", aclMessage.getConversationId(), message));
        }
    }


    private class TransferRequestBehaviour extends CyclicBehaviour {
        private Aid jobLeaderAid;


        @Override
        protected void action() {
            AclMessage message = receive(and(matchPerformative(Performative.REQUEST),
                                             matchProtocol(JobProtocol.ID)));
            if (message == null) {
                block();
                return;
            }

            log(message, "Réception de la requete");

            updateRecipients(message);
            handleNewRequest(message);
        }


        private void updateRecipients(AclMessage message) {
            if (jobLeaderAid == null) {
                try {
                    jobLeaderAid = DFService.searchFirstAgentWithService(getAgent(),
                                                                         Service.JOB_LEADER_SERVICE);
                }
                catch (DFServiceException e) {
                    throw new RuntimeException("Impossible de trouver un agent responsable des jobs", e);
                }
            }

            message.clearReceivers();
            message.addReceiver(jobLeaderAid);
            message.addReplyTo(message.getSender());
            message.addReplyTo(getAID());
        }
    }

    private class AuditListenerBehaviour extends CyclicBehaviour {

        @Override
        protected void action() {
            AclMessage message = receive(and(matchPerformative(Performative.INFORM),
                                             matchProtocol(JobProtocol.ID)));
            if (null == message) {
                block();
                return;
            }
            JobAudit audit = (JobAudit)message.getContentObject();
            if (audit.getType() != Type.MID && LOGGER.isDebugEnabled()) {
                LOGGER.debug("(" + audit.getRequestId() + ") reception audit " + audit.getType());
            }
            if (audit.hasError() || audit.getType() == Type.POST) {
                handleRequestDone(audit);
            }
        }
    }
}
