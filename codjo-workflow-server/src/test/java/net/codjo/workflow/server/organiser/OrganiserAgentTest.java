package net.codjo.workflow.server.organiser;
import net.codjo.agent.AclMessage;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Agent;
import net.codjo.agent.Aid;
import net.codjo.agent.DFService;
import net.codjo.agent.MessageTemplate;
import net.codjo.agent.MessageTemplate.MatchExpression;
import static net.codjo.agent.MessageTemplate.and;
import static net.codjo.agent.MessageTemplate.matchContent;
import static net.codjo.agent.MessageTemplate.matchPerformative;
import static net.codjo.agent.MessageTemplate.matchWith;
import net.codjo.agent.UserId;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.agent.protocol.SubscribeProtocol;
import static net.codjo.agent.test.AgentAssert.log;
import static net.codjo.agent.test.AgentAssert.logAndClear;
import static net.codjo.agent.test.MessageBuilder.message;
import net.codjo.agent.test.OneShotStep;
import net.codjo.agent.test.Step;
import net.codjo.agent.test.Story;
import net.codjo.agent.test.StoryPart;
import net.codjo.agent.test.SystemMock;
import net.codjo.agent.test.TesterAgentRecorder;
import net.codjo.test.common.LogString;
import net.codjo.test.common.fixture.DirectoryFixture;
import net.codjo.util.file.FileUtil;
import net.codjo.workflow.common.Service;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobAudit.Anomaly;
import net.codjo.workflow.common.message.JobAudit.Type;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.Job.State;
import net.codjo.workflow.common.organiser.JobMock;
import net.codjo.workflow.common.organiser.XmlCodec;
import net.codjo.workflow.common.protocol.JobProtocol;
import net.codjo.workflow.server.leader.JobLeaderSubscribeHandler;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import junit.framework.AssertionFailedError;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.SimpleLayout;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class OrganiserAgentTest {
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String ORGANISER_AGENT = "organiser-agent-bidon";
    private static final String JOB_LEADER_AGENT = "job-leader-agent-bidon";
    private static final String SCHEDULE_LEADER_AGENT = "schedule-leader-agent-bidon";

    private Story story = new Story();
    private LogString log = new LogString();
    private OrganiserAgent organiserAgent;
    private RuleEngineMock ruleEngineMock;
    private LogString jobLeaderSubscriptionLog;
    private List<String> jobFilterMock = new ArrayList<String>();
    private DirectoryFixture directoryFixture = DirectoryFixture.newTemporaryDirectoryFixture();
    private File logFile;
    private FileAppender appender;


    @Before
    public void setUp() throws Exception {
        directoryFixture.doSetUp();

        logFile = new File(directoryFixture, "log.txt");
        appender = new FileAppender(new SimpleLayout(), logFile.getPath());

        ruleEngineMock = new RuleEngineMock(log);
        jobLeaderSubscriptionLog = new LogString();
        jobFilterMock.clear();
        organiserAgent = new OrganiserAgent(ruleEngineMock,
                                            new JobFactoryMock(log),
                                            jobFilterMock,
                                            new XmlCodecMock(),
                                            1,
                                            new SubscribeParticipantHandlerMock(jobLeaderSubscriptionLog));

        OrganiserAgent.LOGGER.addAppender(appender);
        OrganiserAgent.LOGGER.setLevel(Level.INFO);

        story.doSetUp();
    }


    @After
    public void tearDown() throws Exception {
        story.doTearDown();
        OrganiserAgent.LOGGER.removeAppender(appender);
        appender.close();
        directoryFixture.doTearDown();
    }


    @Test
    public void test_register() throws Exception {
        story.record().startAgent(ORGANISER_AGENT, organiserAgent);

        story.record().assertAgentWithService(new String[]{ORGANISER_AGENT}, Service.ORGANISER_SERVICE);

        story.execute();
    }


    @Test
    public void test_transferRequestToJobLeader() throws Exception {
        ruleEngineMock.mockWillBeRunning("type");

        story.record().startAgent(ORGANISER_AGENT, organiserAgent);

        story.record().mock(jobLeader()).then()
              .receiveMessage()
              .assertReceivedMessage(and(matchPerformative(Performative.REQUEST),
                                         matchWith(new MatchExpression() {
                                             public boolean match(AclMessage aclMessage) {
                                                 Iterator replyToIter = aclMessage.getAllReplyTo();
                                                 Aid replier1 = (Aid)replyToIter.next();
                                                 Aid replier2 = (Aid)replyToIter.next();

                                                 return checkSchedulerAndOrganiser(replier1, replier2);
                                             }
                                         })));

        story.record().mock(scheduleLeader()).then()
              .sendMessage(createJobRequestMessage("requestId", "type"));

        story.execute();
    }


    @Test
    public void test_runningJobRequest() throws Exception {
        story.record().startAgent(ORGANISER_AGENT, organiserAgent);

        story.record()
              .addAssert(logAndClear(log, "start()"));

        story.record().mock(jobLeader()).then()
              .receiveMessage();

        story.record().mock(scheduleLeader())
              .submitRunningJob("requestId", "import");

        story.record()
              .addAssert(logAndClear(log, "createJob(import, loginTest, passwordTest), "
                                          + "insert(import)"));

        story.execute();
    }


    @Test
    public void test_logRunningJobsOnlyIfNeeded() throws Exception {
        story.record().startAgent(ORGANISER_AGENT, organiserAgent);

        story.record()
              .addAssert(logAndClear(log, "start()"));

        story.record().mock(jobLeader()).then()
              .receiveMessage()
              .then()
              .receiveMessage();

        story.record().mock(scheduleLeader()).then()
              .play(new StoryPart() {
                  public void record(TesterAgentRecorder recorder) {
                      ruleEngineMock.mockWillBeRunning("handler");
                      AclMessage handlerMessage = createJobRequestMessage("01", "handler", false);
                      handlerMessage.setConversationId("handler");
                      recorder.sendMessage(handlerMessage);
                  }
              })
              .then()
              .play(new StoryPart() {
                  public void record(TesterAgentRecorder recorder) {
                      ruleEngineMock.mockWillBeRunning("import");
                      AclMessage importMessage = createJobRequestMessage("02", "import", true);
                      importMessage.setConversationId("import");
                      recorder.sendMessage(importMessage);
                  }
              });

        story.record()
              .addAssert(logAndClear(log, "createJob(handler, loginTest, passwordTest), "
                                          + "insert(handler), "
                                          + "createJob(import, loginTest, passwordTest), "
                                          + "insert(import)"));

        story.execute();

        assertEquals("INFO - (import) Réception de la requete" + NEW_LINE
                     + "INFO - (import) Transfert de la requete" + NEW_LINE,
                     FileUtil.loadContent(logFile));
    }


    @Test
    public void test_logRejectedJobsOnlyIfNeeded() throws Exception {
        story.record().startAgent(ORGANISER_AGENT, organiserAgent);

        story.record()
              .addAssert(logAndClear(log, "start()"));

        story.record().mock(jobLeader());

        story.record().mock(scheduleLeader()).then()
              .play(new StoryPart() {
                  public void record(TesterAgentRecorder recorder) {
                      ruleEngineMock.mockWillBeRejected("handler");
                      AclMessage handlerMessage = createJobRequestMessage("01", "handler", false);
                      handlerMessage.setConversationId("handler");
                      recorder.sendMessage(handlerMessage);
                  }
              })
              .then()
              .play(new StoryPart() {
                  public void record(TesterAgentRecorder recorder) {
                      ruleEngineMock.mockWillBeRejected("import");
                      AclMessage importMessage = createJobRequestMessage("02", "import", true);
                      importMessage.setConversationId("import");
                      recorder.sendMessage(importMessage);
                  }
              });

        story.record()
              .addAssert(logAndClear(log, "createJob(handler, loginTest, passwordTest), "
                                          + "insert(handler), "
                                          + "retract(handler), "
                                          + "createJob(import, loginTest, passwordTest), "
                                          + "insert(import), "
                                          + "retract(import)"));

        story.execute();

        assertEquals(0, ruleEngineMock.getRejectedJobs().size());
        assertEquals("INFO - (import) Réception de la requete" + NEW_LINE
                     + "INFO - (import) Rejet de la requete" + NEW_LINE,
                     FileUtil.loadContent(logFile));
    }


    @Test
    public void test_waitingJobRequest() throws Exception {
        story.record().startAgent(ORGANISER_AGENT, organiserAgent);

        story.record().mock(jobLeader()).then()
              .receiveMessage(matchWith(new MatchExpression() {
                  public boolean match(AclMessage aclMessage) {
                      return "02".equals(((JobRequest)aclMessage.getContentObject()).getId());
                  }
              }));

        story.record()
              .addAssert(logAndClear(log, "start()"));

        story.record().mock(scheduleLeader())
              .submitWaitingJob("01", "import")
              .submitRunningJob("02", "broadcast");

        story.record()
              .addAssert(logAndClear(log, "createJob(import, loginTest, passwordTest), "
                                          + "insert(import), "
                                          + "createJob(broadcast, loginTest, passwordTest), "
                                          + "insert(broadcast)"));

        story.execute();
    }


    @Test
    public void test_jobRequestFinishedRunning() throws Exception {
        story.record().startAgent(ORGANISER_AGENT, organiserAgent);
        story.record()
              .addAssert(logAndClear(log, "start()"));

        story.record().mock(scheduleLeader())
              .waitForJobLeader()
              .submitRunningJob("01", "import");

        story.record().mock(jobLeader())
              .assertLogAndClear(log, "createJob(import, loginTest, passwordTest), "
                                      + "insert(import)").then()
              .sendMessage(createJobAuditMessage("01", Type.PRE)).then()
              .play(assertLogAndClear("")).then()
              .sendMessage(createJobAuditMessage("01", Type.MID)).then()
              .play(assertLogAndClear("")).then()
              .sendMessage(createJobAuditMessage("01", Type.MID)).then()
              .play(assertLogAndClear("")).then()
              .sendMessage(createJobAuditMessage("01", Type.POST)).then()
              .play(assertLogAndClear("retract(import)"));

        story.execute();
    }


    @Test
    public void test_jobRequestHasError() throws Exception {
        story.record().startAgent(ORGANISER_AGENT, organiserAgent);
        story.record()
              .addAssert(logAndClear(log, "start()"));

        story.record().mock(scheduleLeader())
              .waitForJobLeader()
              .submitRunningJob("01", "import")
              .submitRunningJob("02", "segmentation");

        story.record().mock(jobLeader())
              .assertLogAndClear(log, "createJob(import, loginTest, passwordTest), "
                                      + "insert(import), "
                                      + "createJob(segmentation, loginTest, passwordTest), "
                                      + "insert(segmentation)").then()
              .sendMessage(createJobAuditMessage("01", Type.PRE, "Error in import !!!")).then()
              .play(assertLogAndClear("retract(import)")).then()
              .sendMessage(createJobAuditMessage("02", Type.PRE)).then()
              .play(assertLogAndClear("")).then()
              .sendMessage(createJobAuditMessage("02", Type.MID, "Error in segmentation !!!")).then()
              .play(assertLogAndClear("retract(segmentation)"));

        story.execute();
    }


    @Test
    public void test_retry() throws Exception {
        story.record().startAgent(ORGANISER_AGENT, organiserAgent);

        story.record().mock(jobLeader()).then()
              .receiveMessage().then()
              .play(synchronizeOrganiser()).then()
              .sendMessage(createJobAuditMessage("01", Type.POST)).then()
              .receiveMessage();

        story.record().mock(scheduleLeader())
              .submitRunningJob("01", "import")
              .submitWaitingJob("02", "segmentation");

        story.execute();
    }


    @Test
    public void test_doNotTransferNonWorkflowRequest() throws Exception {
        ruleEngineMock.mockWillBeRunning("import");

        story.record().startAgent(ORGANISER_AGENT, organiserAgent);

        story.record().mock(jobLeader()).then()
              .receiveMessage(matchWith(new MatchExpression() {
                  public boolean match(AclMessage aclMessage) {
                      return "01".equals((((JobRequest)aclMessage.getContentObject()).getId()));
                  }
              }));

        story.record().mock(scheduleLeader()).then()
              .sendMessage(Performative.REQUEST, "myOwnProtocol", new Aid(ORGANISER_AGENT), "DoIt")
              .then()
              .sendMessage(createJobRequestMessage("01", "import"));

        story.execute();
    }


    @Test
    public void test_createJobFailure() throws Exception {
        ruleEngineMock.mockWillBeRunning("import");
        organiserAgent = new OrganiserAgent(ruleEngineMock, new JobFactory() {
            public Job createJob(JobRequest request, UserId userId) throws JobBuilderException {
                throw new JobBuilderException("Erreur !!!", null);
            }
        }, null, new XmlCodec(), 10, new JobLeaderSubscribeHandler());

        story.record().startAgent(ORGANISER_AGENT, organiserAgent);
        story.record().addAssert(logAndClear(log, "start()"));

        story.record().mock(jobLeader());

        story.record().mock(scheduleLeader()).then()
              .sendMessage(createJobRequestMessage("requestId", "import")).then()
              .receiveMessage(matchWith(new MatchExpression() {
                  public boolean match(AclMessage aclMessage) {
                      JobAudit jobAudit = (JobAudit)aclMessage.getContentObject();
                      return jobAudit.hasError()
                             && "Impossible de traiter la requête.".equals(jobAudit.getErrorMessage())
                             && jobAudit.getType() == Type.PRE;
                  }
              }));

        story.record().addAssert(log(log, ""));

        story.execute();
    }


    @Test
    public void test_createJobRejected() throws Exception {
        ruleEngineMock.mockWillBeRejected("import");

        story.record().startAgent(ORGANISER_AGENT, organiserAgent);
        story.record().addAssert(logAndClear(log, "start()"));

        story.record().mock(jobLeader());

        story.record().mock(scheduleLeader()).then()
              .sendMessage(createJobRequestMessage("requestId", "import")).then()
              .receiveMessage(matchWith(new MatchExpression() {
                  public boolean match(AclMessage aclMessage) {
                      JobAudit jobAudit = (JobAudit)aclMessage.getContentObject();
                      return jobAudit.hasError()
                             && "La requête a été rejetée.".equals(jobAudit.getErrorMessage())
                             && jobAudit.getType() == Type.PRE;
                  }
              }));

        story.record().addAssert(log(log, "createJob(import, loginTest, passwordTest), "
                                          + "insert(import), "
                                          + "retract(import)"));

        story.execute();
    }


    @Test
    public void test_getAllJobs() throws Exception {
        ruleEngineMock.insert(JobMock.create("import", State.NEW));
        ruleEngineMock.insert(JobMock.create("segmentation", State.NEW));

        story.record().startAgent(ORGANISER_AGENT, organiserAgent);
        story.record().addAssert(logAndClear(log, "insert(import), insert(segmentation), start()"));

        story.record()
              .startTester("taskManager")
              .send(message(Performative.QUERY)
                    .usingProtocol(RequestProtocol.QUERY)
                    .to(ORGANISER_AGENT)
                    .withContent("allJobs"))
              .then().receiveMessage(matchContent("[Job(import, WAITING), Job(segmentation, WAITING)]"));

        story.record().addAssert(log(log, "getAllJobs()"));

        story.execute();
    }


    @Test
    public void test_getAllJobs_doneJobs() throws Exception {
        story.record().startAgent(ORGANISER_AGENT, organiserAgent);
        story.record().addAssert(logAndClear(log, "start()"));

        story.record().mock(scheduleLeader())
              .waitForJobLeader()
              .submitRunningJob("import", "import")
              .submitRunningJob("import2", "import2")
              .submitWaitingJob("segmentation", "segmentation");

        story.record().mock(jobLeader())
              .then().receiveMessage(jobRequest("import"))
              .then().sendMessage(createJobAuditMessage("import", Type.POST))
              .then().receiveMessage(jobRequest("import2"))
              .then().sendMessage(createJobAuditMessage("import2", Type.POST));

        story.record().addAssert(log(log, Pattern.compile(".*retract\\(import2\\).*")));

        story.record()
              .startTester("taskManager")
              .send(message(Performative.QUERY)
                    .usingProtocol(RequestProtocol.QUERY)
                    .to(ORGANISER_AGENT)
                    .withContent("allJobs"))
              .then().receiveMessage(matchContent("[Job(import2, DONE), Job(segmentation, WAITING)]"));

        story.execute();
    }


    @Test
    public void test_getAllJobs_filter() throws Exception {
        jobFilterMock.add("handler");

        story.record().startAgent(ORGANISER_AGENT, organiserAgent);
        story.record().addAssert(logAndClear(log, "start()"));

        story.record().mock(scheduleLeader())
              .waitForJobLeader()
              .submitRunningJob("import", "import")
              .submitRunningJob("handler", "handler");

        story.record().mock(jobLeader())
              .then().receiveMessage(jobRequest("import"))
              .then().sendMessage(createJobAuditMessage("import", Type.POST))
              .then().receiveMessage(jobRequest("handler"))
              .then().sendMessage(createJobAuditMessage("handler", Type.POST));

        story.record().addAssert(log(log, Pattern.compile(".*retract\\(handler\\).*")));

        story.record().startTester("taskManager")
              .send(message(Performative.QUERY)
                    .usingProtocol(RequestProtocol.QUERY)
                    .to(ORGANISER_AGENT)
                    .withContent("allJobs"))
              .then().receiveMessage(matchContent("[Job(import, DONE)]"));

        story.execute();
    }


    @Test
    public void test_notification() throws Exception {
        final String filteredType = "handlerType";
        jobFilterMock.add(filteredType);

        story.record().startAgent(ORGANISER_AGENT, organiserAgent);
        story.record().addAssert(logAndClear(log, "start()"));

        story.record()
              .startTester("subscriber-agent")
              .sendMessage(message(Performative.SUBSCRIBE)
                    .usingProtocol(SubscribeProtocol.ID)
                    .to(ORGANISER_AGENT).get()).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(import, NEW)]"))).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(import, RUNNING)]"))).then()

              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(segmentation, NEW)]"))).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(segmentation, WAITING)]"))).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(broadcast, NEW)]"))).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(broadcast, WAITING)]"))).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent(
                                        "[Job(segmentation, RUNNING), Job(broadcast, RUNNING), Job(import, DONE)]")));

        ruleEngineMock.retractJobWillTrigger("import", "segmentation", "broadcast");
        ruleEngineMock.mockWillBeRunning(filteredType);

        story.record().mock(jobLeader())
              .assertLogAndClear(log, "createJob(handlerType, loginTest, passwordTest), "
                                      + "insert(handlerType), "
                                      + "createJob(import, loginTest, passwordTest), "
                                      + "insert(import), "
                                      + "createJob(segmentation, loginTest, passwordTest), "
                                      + "insert(segmentation), "
                                      + "createJob(handlerType, loginTest, passwordTest), "
                                      + "insert(handlerType), "
                                      + "createJob(broadcast, loginTest, passwordTest), "
                                      + "insert(broadcast)").then()
              .sendMessage(createJobAuditMessage("importId", Type.POST)).then()
              .receiveMessage(matchWith(new MatchExpression() {
                  public boolean match(AclMessage aclMessage) {
                      return "segmentationId".equals(((JobRequest)aclMessage.getContentObject()).getId());
                  }
              }))
              .then()
              .receiveMessage(matchWith(new MatchExpression() {
                  public boolean match(AclMessage aclMessage) {
                      return "broadcastId".equals(((JobRequest)aclMessage.getContentObject()).getId());
                  }
              }));

        story.record().mock(scheduleLeader())
              .submitRunningJob("nimporteQuoi", filteredType)
              .submitRunningJob("importId", "import")
              .submitWaitingJob("segmentationId", "segmentation")
              .submitWaitingJob("selectNothing", filteredType)
              .submitWaitingJob("broadcastId", "broadcast");

        story.execute();
    }


    @Test
    public void test_notificationError() throws Exception {
        story.record().startAgent(ORGANISER_AGENT, organiserAgent);
        story.record()
              .addAssert(logAndClear(log, "start()"));

        story.record()
              .startTester("subscriber-agent")
              .sendMessage(message(Performative.SUBSCRIBE)
                    .usingProtocol(SubscribeProtocol.ID)
                    .to(ORGANISER_AGENT).get()).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(import, NEW)]"))).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(import, RUNNING)]"))).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(import, FAILURE, Error !!!)]")));

        story.record().mock(jobLeader())
              .assertLogAndClear(log, "createJob(import, loginTest, passwordTest), insert(import)").then()
              .sendMessage(createJobAuditMessage("importId", Type.PRE, "Error !!!")).then();

        story.record().mock(scheduleLeader())
              .submitRunningJob("importId", "import");

        story.execute();
    }


    @Test
    public void test_informRejectedJobRequest() throws Exception {
        jobFilterMock.add("handler");
        story.record().startAgent(ORGANISER_AGENT, organiserAgent);
        story.record()
              .addAssert(logAndClear(log, "start()"));

        story.record().mock(jobLeader());

        story.record()
              .startTester("subscriber-agent")
              .sendMessage(message(Performative.SUBSCRIBE)
                    .usingProtocol(SubscribeProtocol.ID)
                    .to(ORGANISER_AGENT).get()).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(import, NEW)]"))).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(import, WAITING)]"))).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(import, REJECTED)]")));

        story.record().mock(scheduleLeader())
              .submitWaitingJob("importId", "import")
              .submitRunningJob("handlerId", "handler")
              .recorder.receiveMessage(jobAudit("La requête a été rejetée."));

        ruleEngineMock.insertJobWillReject("handler", "import");

        story.execute();
    }


    @Test
    public void test_informNewJobRequest() throws Exception {
        story.record().startAgent(ORGANISER_AGENT, organiserAgent);
        story.record()
              .addAssert(logAndClear(log, "start()"));

        story.record().mock(jobLeader());

        story.record().mock(scheduleLeader())
              .submitRunningJob("importId", "import")
              .submitRunningJob("segmentationId", "segmentation");

        story.record()
              .addAssert(log(jobLeaderSubscriptionLog,
                             "sendNotification(importId), sendNotification(segmentationId)"));

        story.execute();
    }


    @Test
    public void test_handleNewRequest_ruleEngineFailure() throws Exception {
        ruleEngineMock.mockWillThrowException("import");

        story.record().startAgent(ORGANISER_AGENT, organiserAgent);
        story.record()
              .addAssert(logAndClear(log, "start()"));

        story.record()
              .startTester("subscriber-agent")
              .sendMessage(message(Performative.SUBSCRIBE)
                    .usingProtocol(SubscribeProtocol.ID)
                    .to(ORGANISER_AGENT).get()).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(import, NEW)]"))).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(import, WAITING)]"))).then()

              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(segmentation, NEW)]"))).then()
              .receiveMessage(and(matchPerformative(Performative.INFORM),
                                  matchContent("[Job(segmentation, RUNNING)]")));

        story.record().mock(jobLeader());

        story.record().mock(scheduleLeader())
              .submitWaitingJob("importId", "import")
              .receiveAuditError("exception with import job !!!")
              .submitRunningJob("segmentationId", "segmentation");

        story.record()
              .addAssert(log(jobLeaderSubscriptionLog,
                             "sendNotification(importId), sendNotification(segmentationId)"));

        story.execute();
    }


    private MessageTemplate jobRequest(final String requestType) {
        return matchWith(
              new MatchExpression() {
                  public boolean match(AclMessage aclMessage) {
                      JobRequest jobRequest = (JobRequest)aclMessage.getContentObject();
                      return requestType.equals(jobRequest.getType());
                  }
              });
    }


    private MessageTemplate jobAudit(final String errorMessage, final String cause) {
        return matchWith(
              new MatchExpression() {
                  public boolean match(AclMessage aclMessage) {
                      JobAudit jobAudit = (JobAudit)aclMessage.getContentObject();
                      return jobAudit.getType() == Type.PRE
                             && jobAudit.hasError()
                             && errorMessage.equals(jobAudit.getErrorMessage())
                             && jobAudit.getError().getDescription().contains(cause);
                  }
              });
    }


    private MessageTemplate jobAudit(final String errorMessage) {
        return matchWith(
              new MatchExpression() {
                  public boolean match(AclMessage aclMessage) {
                      JobAudit jobAudit = (JobAudit)aclMessage.getContentObject();
                      return jobAudit.getType() == Type.PRE
                             && jobAudit.hasError()
                             && errorMessage.equals(jobAudit.getErrorMessage());
                  }
              });
    }


    private StoryPart assertLogAndClear(final String expected) {
        return new StoryPart() {
            public void record(TesterAgentRecorder recorder) {
                recorder.perform(new Step() {
                    public void run(Agent agent) throws Exception {
                    }


                    public boolean done() {
                        try {
                            log.assertAndClear(expected);
                            return true;
                        }
                        catch (AssertionError e) {
                            return false;
                        }
                    }
                });
            }
        };
    }


    private StoryPart synchronizeOrganiser() {
        return new StoryPart() {
            public void record(TesterAgentRecorder recorder) {
                recorder.perform(new Step() {
                    public void run(Agent agent) throws Exception {
                    }


                    public boolean done() {
                        return log.getContent()
                              .startsWith("start(), "
                                          + "createJob(import, loginTest, passwordTest), insert(import), "
                                          + "createJob(segmentation, loginTest, passwordTest), insert(segmentation)");
                    }
                });
                recorder.perform(new OneShotStep() {
                    public void run(Agent agent) throws Exception {
                        ruleEngineMock.mockWillBeRunning("segmentation");
                    }
                });
            }
        };
    }


    private boolean checkSchedulerAndOrganiser(Aid firstReplyTo, Aid secondReplyTo) {
        return (SCHEDULE_LEADER_AGENT.equals(firstReplyTo.getLocalName())
                && ORGANISER_AGENT.equals(secondReplyTo.getLocalName()))
               || (SCHEDULE_LEADER_AGENT.equals(secondReplyTo.getLocalName())
                   && ORGANISER_AGENT.equals(firstReplyTo.getLocalName()));
    }


    private AclMessage createJobRequestMessage(String requestId, String type, boolean loggable) {
        JobRequest jobRequest = new JobRequest(type);
        jobRequest.setId(requestId);
        jobRequest.setLoggable(loggable);
        AclMessage requestMessage = new AclMessage(AclMessage.Performative.REQUEST);
        requestMessage.setProtocol(JobProtocol.ID);
        requestMessage.addReceiver(new Aid(ORGANISER_AGENT));
        requestMessage.setContentObject(jobRequest);
        requestMessage.encodeUserId(UserId.createId("loginTest", "passwordTest"));
        return requestMessage;
    }


    private AclMessage createJobRequestMessage(String requestId, String type) {
        return createJobRequestMessage(requestId, type, true);
    }


    private static AclMessage createJobAuditMessage(String requestId, Type type) {
        return createJobAuditMessage(requestId, type, null);
    }


    private static AclMessage createJobAuditMessage(String requestId, Type type, String errorMessage) {
        AclMessage statusMessage = new AclMessage(AclMessage.Performative.INFORM);
        statusMessage.setProtocol(JobProtocol.ID);
        statusMessage.addReceiver(new Aid(ORGANISER_AGENT));
        statusMessage.addReceiver(new Aid(SCHEDULE_LEADER_AGENT));
        JobAudit audit = new JobAudit(type);
        audit.setRequestId(requestId);
        if (errorMessage != null) {
            audit.setError(new Anomaly(errorMessage));
        }
        statusMessage.setContentObject(audit);
        return statusMessage;
    }


    private ScheduleLeaderSystemMock scheduleLeader() {
        return new ScheduleLeaderSystemMock();
    }


    private LeaderSystemMock jobLeader() {
        return new LeaderSystemMock(JOB_LEADER_AGENT, Service.JOB_LEADER_SERVICE);
    }


    private static class LeaderSystemMock<T extends LeaderSystemMock> extends SystemMock {
        protected TesterAgentRecorder recorder;
        private String agentName;
        private String serviceName;


        private LeaderSystemMock(String agentName, String serviceName) {
            this.agentName = agentName;
            this.serviceName = serviceName;
        }


        public TesterAgentRecorder then() {
            return recorder;
        }


        @Override
        protected void record(Story story) {
            recorder = story.record().startTester(agentName);
            recorder.registerToDF(serviceName);
            story.record()
                  .assertAgentWithService(new String[]{agentName}, serviceName);
        }


        public LeaderSystemMock<T> assertLogAndClear(final LogString log, final String expected) {

            recorder.perform(new Step() {
                public void run(Agent agent) throws Exception {
                }


                public boolean done() {
                    try {
                        log.assertAndClear(expected);
                        return true;
                    }
                    catch (AssertionFailedError e) {
                        return false;
                    }
                }
            });
            return this;
        }
    }

    private class ScheduleLeaderSystemMock extends LeaderSystemMock<ScheduleLeaderSystemMock> {

        private ScheduleLeaderSystemMock() {
            super(SCHEDULE_LEADER_AGENT, SCHEDULE_LEADER_AGENT);
        }


        ScheduleLeaderSystemMock submitRunningJob(final String requestId, final String type) {
            recorder.play(new StoryPart() {
                public void record(TesterAgentRecorder recorder) {
                    ruleEngineMock.mockWillBeRunning(type);
                    recorder.sendMessage(createJobRequestMessage(requestId, type));
                }
            });
            return this;
        }


        ScheduleLeaderSystemMock submitWaitingJob(final String requestId, final String type) {
            recorder.play(new StoryPart() {
                public void record(TesterAgentRecorder recorder) {
                    recorder.sendMessage(createJobRequestMessage(requestId, type));
                }
            });
            return this;
        }


        ScheduleLeaderSystemMock receiveAuditError(final String cause) {
            recorder.play(new StoryPart() {
                public void record(TesterAgentRecorder recorder) {
                    recorder.receiveMessage(jobAudit("Impossible de traiter la requête.", cause));
                }
            });
            return this;
        }


        ScheduleLeaderSystemMock waitForJobLeader() {
            recorder.perform(new Step() {
                private Agent agent;


                public void run(Agent someAgent) throws Exception {
                    this.agent = someAgent;
                }


                public boolean done() {
                    try {
                        DFService.searchFirstAgentWithService(agent, Service.JOB_LEADER_SERVICE);
                    }
                    catch (Exception e) {
                        return false;
                    }
                    return true;
                }
            });
            return this;
        }
    }

    private static class JobFactoryMock implements JobFactory {
        private LogString log;


        private JobFactoryMock(LogString log) {
            this.log = log;
        }


        public JobMock createJob(JobRequest request, UserId userId) {
            log.call("createJob", request.getType(), userId.getLogin(), userId.getPassword());
            return JobMock.create(request.getType(), State.NEW);
        }
    }

    private class XmlCodecMock extends XmlCodec {

        @Override
        public String jobsToXml(List<? extends Job> jobs) {
            return jobs.toString();
        }


        @Override
        public String jobEventToXml(JobEvent jobEvent) {
            return String.format("JobEvent(%s)", jobEvent.getRequest().getId());
        }
    }

    private static class SubscribeParticipantHandlerMock extends JobLeaderSubscribeHandler {
        private final LogString log;


        private SubscribeParticipantHandlerMock(LogString log) {
            this.log = log;
        }


        @Override
        public void sendNotification(JobEvent jobEvent) {
            if (jobEvent.isAudit()) {
                JobAudit audit = jobEvent.getAudit();
                log.call("sendNotification", audit.getRequestId(), audit.getErrorMessage());
            } else {
                log.call("sendNotification", jobEvent.getRequest().getId());
            }
        }
    }
}
