/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.server.leader;
import net.codjo.agent.AclMessage;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Aid;
import net.codjo.agent.DFService;
import net.codjo.agent.MessageTemplate;
import net.codjo.agent.MessageTemplate.MatchExpression;
import net.codjo.agent.protocol.SubscribeProtocol;
import net.codjo.agent.test.AgentAssert;
import static net.codjo.agent.test.AgentAssert.logAndClear;
import static net.codjo.agent.test.MessageBuilder.message;
import net.codjo.agent.test.Semaphore;
import net.codjo.agent.test.StoryPart;
import net.codjo.agent.test.TesterAgentRecorder;
import net.codjo.test.common.LogString;
import net.codjo.test.common.fixture.DirectoryFixture;
import net.codjo.util.file.FileUtil;
import net.codjo.workflow.common.Service;
import net.codjo.workflow.common.message.JobAudit;
import static net.codjo.workflow.common.message.JobAudit.Status;
import static net.codjo.workflow.common.message.JobAudit.Type;
import net.codjo.workflow.common.message.JobContract;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobException;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.protocol.JobProtocolParticipant;
import net.codjo.workflow.server.api.JobAgent;
import net.codjo.workflow.server.api.WorkflowTestCase;
import net.codjo.workflow.server.audit.AuditDaoMock;
import java.io.File;
import java.util.Date;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.SimpleLayout;
/**
 * Classe de test de {@link JobLeaderAgent}.
 *
 * @noinspection OverlyCoupledClass
 */
public class JobLeaderAgentTest extends WorkflowTestCase {
    private static final String NEW_LINE = System.getProperty("line.separator");

    private static final String IMPORT_NOT_FOUND_MESSAGE
          = "Impossible de trouver un agent capable de réaliser la requête import";
    private static final String IMPORT_TYPE = "import";
    private static final String JOB_LEADER = "job-leader-agent-bidon";


    public void test_execute_unavailableAgent() throws Exception {
        story.record().startAgent(JOB_LEADER, createLeader());

        story.record().startTester("initiator")
              .sendMessage(createJobRequestMessage(new JobRequest(IMPORT_TYPE), new Aid(JOB_LEADER)))
              .then()
              .receiveMessage()
              .assertReceivedMessage(containsAudit(Type.PRE, Status.ERROR, IMPORT_NOT_FOUND_MESSAGE));

        story.execute();
    }


    public void test_execute_jobFailure() throws Exception {
        JobProtocolParticipantMock agent = new JobProtocolParticipantMock(log);
        agent.mockExecuteFailure(new JobException("echec d'import"));

        story.record().startAgent("job-agent", createJobAgent(agent));
        story.record().assertNumberOfAgentWithService(1, IMPORT_TYPE);

        story.record().startAgent(JOB_LEADER, createLeader());

        story.record().startTester("initiator")
              .sendMessage(createJobRequestMessage(new JobRequest(IMPORT_TYPE), new Aid(JOB_LEADER)))
              .then()
              .receiveMessage(containsAudit(Type.PRE, Status.OK))
              .then()
              .receiveMessage(containsAudit(Type.MID, Status.OK))
              .then()
              .receiveMessage(containsAudit(Type.POST, Status.ERROR, "echec d'import"));

        story.execute();
    }


    public void test_execute_TwoJobs() throws Exception {
        story.record().startAgent("job-agent-1", createJobAgent(new LogString("agent", log)));
        story.record().assertNumberOfAgentWithService(1, IMPORT_TYPE);

        story.record().startAgent(JOB_LEADER, createLeader());

        story.record().startTester("initiator")
              .play(new LaunchSuccessfulImport())
              .then()
              .play(new LaunchSuccessfulImport());

        story.record().addAssert(AgentAssert.logAndClear(log, "agent.executeJob(import, uid:user_dev)"
                                                              + ", agent.executeJob(import, uid:user_dev)"));
        story.execute();
    }


    public void test_logOnlyIfNeeded() throws Exception {
        DirectoryFixture directoryFixture = DirectoryFixture.newTemporaryDirectoryFixture();
        directoryFixture.doSetUp();

        File logFile = new File(directoryFixture, "log.txt");
        FileAppender appender = new FileAppender(new SimpleLayout(), logFile.getPath());

        JobAgent jobAgent = createJobAgent(new LogString("agent", log));
        story.record().startAgent("job-agent-1", jobAgent);
        story.record().assertNumberOfAgentWithService(1, IMPORT_TYPE);

        JobLeaderAgent jobLeader = createLeader();
        JobLeaderAgent.LOGGER.addAppender(appender);
        JobLeaderAgent.LOGGER.setLevel(Level.INFO);

        story.record().startAgent(JOB_LEADER, jobLeader);

        AclMessage loggableMessage = createJobRequestMessage(createJobRequest(true), new Aid(JOB_LEADER));
        AclMessage notLoggableMessage = createJobRequestMessage(createJobRequest(false), new Aid(JOB_LEADER));

        story.record().startTester("initiator")
              .play(new LaunchSuccessfulImport(notLoggableMessage))
              .then()
              .play(new LaunchSuccessfulImport(loggableMessage));

        story.record().addAssert(AgentAssert.logAndClear(log, "agent.executeJob(import, uid:user_dev)"
                                                              + ", agent.executeJob(import, uid:user_dev)"));
        story.execute();

        assertEquals("INFO - Activation de l'ecoute de nouvelle requete" + NEW_LINE
                     + "INFO - (" + loggableMessage.getConversationId()
                     + ") Reception par job-lead-for-initiator" + NEW_LINE
                     + "INFO - Le jobAgent '( agent-identifier :name "
                     + jobAgent.getAID().getName()
                     + " )' est recruté pour la tache import par job-lead-for-initiator"
                     + NEW_LINE
                     + "INFO - Mise en place du proxy job-lead-for-initiator" + NEW_LINE,
                     FileUtil.loadContent(logFile));

        JobLeaderAgent.LOGGER.removeAppender(appender);
        appender.close();
        directoryFixture.doTearDown();
    }


    public void test_selectExpertAgent() throws Exception {
        JobProtocolParticipantMock expert = new JobProtocolParticipantMock(new LogString("expert", log));
        expert.mockGetSkillLevel(JobProtocolParticipant.SkillLevel.EXPERT);

        story.record().startAgent("job-agent-1", createJobAgent(log));
        story.record().startAgent("job-agent-2", createJobAgent(log));
        story.record().startAgent("job-agent-expert", createJobAgent(expert));
        story.record().assertNumberOfAgentWithService(3, IMPORT_TYPE);

        story.record().startAgent(JOB_LEADER, createLeader());

        story.record().startTester("initiator").play(new LaunchSuccessfulImport());

        story.record().addAssert(AgentAssert.logAndClear(log, "expert.executeJob(import, uid:user_dev)"));

        story.execute();
    }


    public void test_listener_receiveEvent() throws Exception {
        story.record().startAgent("job-agent-1", createJobAgent(new LogString()));
        story.record().assertNumberOfAgentWithService(1, IMPORT_TYPE);

        story.record().startAgent(JOB_LEADER, createLeader());
        story.record().assertAgentWithService(new String[]{JOB_LEADER}, Service.JOB_LEADER_SERVICE);

        story.record().startTester("spy-agent")
              .send(message(Performative.SUBSCRIBE).usingProtocol(SubscribeProtocol.ID).to(JOB_LEADER)).then()
              .receiveMessage(matchAudit(Type.PRE)).then()
              .receiveMessage(matchAudit(Type.MID)).then()
              .receiveMessage(matchAudit(Type.POST));

        story.record().startTester("initiator").play(new LaunchSuccessfulImport());

        story.execute();
    }


    private MessageTemplate matchAudit(final Type type) {
        return MessageTemplate.matchWith(new MatchExpression() {
            public boolean match(AclMessage aclMessage) {
                JobEvent jobEvent = (JobEvent)aclMessage.getContentObject();
                return jobEvent.getAudit().getType() == type;
            }
        });
    }


    public void test_execute_parallelTasks() throws Exception {
        final Semaphore blockFirstJob = new Semaphore();
        JobProtocolParticipantMock job1 = new JobProtocolParticipantMock(new LogString("job1", log)) {
            @Override
            protected void executeJob(JobRequest request) throws JobException {
                super.executeJob(request);
                blockFirstJob.acquire();
                agentLog.info("end of execute");
            }
        };
        job1.mockGetSkillLevel(JobProtocolParticipant.SkillLevel.EXPERT);

        story.record().startAgent("job-agent-1", createJobAgent(job1));
        story.record().startAgent("job-agent-2", createJobAgent(new LogString("job2", log)));
        story.record().assertNumberOfAgentWithService(2, IMPORT_TYPE);

        story.record().startAgent(JOB_LEADER, createLeader());

        story.record().startTester("initiator-1").play(new LaunchSuccessfulImport());

        story.record().addAssert(logAndClear(log, "job1.executeJob(import, uid:user_dev)"));

        story.record().startTester("initiator-2").play(new LaunchSuccessfulImport())
              .then()
              .release(blockFirstJob);

        story.record().addAssert(logAndClear(log, "job2.executeJob(import, uid:user_dev)"
                                                  + ", job1.end of execute"));

        story.execute();
    }


    public void test_execute_replyTimeoutFailure() throws Exception {
        story.record().startAgent(JOB_LEADER, createLeader());

        AclMessage oldMessage = createJobRequestMessage(new JobRequest("faire-du-minitel"),
                                                        new Aid(JOB_LEADER));
        oldMessage.setReplyByDate(new Date(10));

        story.record().startTester("papy-initiator")
              .sendMessage(oldMessage)
              .then()
              .receiveMessage()
              .assertReceivedMessage(containsAudit(Type.PRE, Status.ERROR))
              .assertReceivedMessage(
                    hasAuditErrorMessage("Abandon de la requete car le delai d'attente a ete trop long."));

        story.execute();
    }


    private JobLeaderAgent createLeader() {
        return new JobLeaderAgent(
              new AuditDaoMock(new LogString()),
              new JobLeaderSubscribeHandler());
    }


    private JobAgent createJobAgent(LogString logString) {
        return createJobAgent(new JobProtocolParticipantMock(logString));
    }


    private JobAgent createJobAgent(JobProtocolParticipant participant) {
        DFService.AgentDescription description = new DFService.AgentDescription();
        description.addService(new DFService.ServiceDescription(IMPORT_TYPE, "standard-import"));
        return createJobAgent(participant, IMPORT_TYPE);
    }


    private JobAgent createJobAgent(JobProtocolParticipant participant, String type) {
        DFService.AgentDescription description = new DFService.AgentDescription();
        description.addService(new DFService.ServiceDescription(type, "standard-" + type));
        return new JobAgent(participant, description);
    }


    private JobRequest createJobRequest(boolean loggable) {
        JobRequest jobRequest = new JobRequest(IMPORT_TYPE);
        jobRequest.setLoggable(loggable);
        return jobRequest;
    }


    private static class JobProtocolParticipantMock extends JobProtocolParticipant {
        protected LogString agentLog;
        private SkillLevel mockedSkill = super.getSkillLevel();
        private JobException failure;


        JobProtocolParticipantMock(LogString log) {
            this.agentLog = log;
        }


        @Override
        protected SkillLevel getSkillLevel() {
            return mockedSkill;
        }


        @Override
        protected boolean acceptContract(JobContract contract) {
            return super.acceptContract(contract);
        }


        @Override
        protected void executeJob(JobRequest request) throws JobException {
            agentLog.call("executeJob", request.getType(),
                          "uid:" + getRequestMessage().decodeUserId().getLogin());
            sendAudit(new JobAudit(Type.MID));
            if (failure != null) {
                throw failure;
            }
        }


        public JobProtocolParticipantMock mockGetSkillLevel(SkillLevel value) {
            mockedSkill = value;
            return this;
        }


        public void mockExecuteFailure(JobException error) {
            this.failure = error;
        }
    }
    private class LaunchSuccessfulImport implements StoryPart {
        private AclMessage requestMessage;


        LaunchSuccessfulImport() {
            this(createJobRequestMessage(createJobRequest(true), new Aid(JOB_LEADER)));
        }


        LaunchSuccessfulImport(AclMessage requestMessage) {
            this.requestMessage = requestMessage;
        }


        public void record(TesterAgentRecorder recorder) {
            recorder.sendMessage(requestMessage)
                  .then()
                  .receiveMessage()
                  .assertReceivedMessage(containsAudit(Type.PRE, Status.OK))
                  .then()
                  .receiveMessage()
                  .assertReceivedMessage(containsAudit(Type.MID, Status.OK))
                  .then()
                  .receiveMessage()
                  .assertReceivedMessage(containsAudit(Type.POST, Status.OK));
        }
    }
}
