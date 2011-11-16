package net.codjo.workflow.server.aspect;
import net.codjo.agent.Agent;
import net.codjo.agent.Aid;
import net.codjo.agent.ServiceHelper;
import static net.codjo.agent.test.AgentAssert.log;
import net.codjo.aspect.Aspect;
import net.codjo.aspect.AspectContext;
import net.codjo.aspect.AspectException;
import net.codjo.aspect.AspectManager;
import net.codjo.aspect.JoinPoint;
import net.codjo.mad.server.handler.AspectLauncher;
import net.codjo.mad.server.handler.aspect.AspectBranchId;
import net.codjo.security.server.api.SecurityService;
import net.codjo.security.server.api.SecurityServiceHelperMock;
import net.codjo.sql.server.JdbcServiceUtilMock;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.message.JobAudit.Status;
import net.codjo.workflow.common.message.JobAudit.Type;
import net.codjo.workflow.common.message.JobRequest;
import static net.codjo.workflow.server.api.JobAgent.MODE.DELEGATE;
import static net.codjo.workflow.server.api.JobAgent.MODE.NOT_DELEGATE;
import net.codjo.workflow.server.api.WorkflowTestCase;
import org.exolab.castor.jdo.JDO;
/**
 *
 */
public class AspectExecutorAgentTest extends WorkflowTestCase {
    @SuppressWarnings({"StaticNonFinalField"})
    // Pour l'aspect
    private static LogString aspectLog = null;
    private JoinPoint joinPoint = new JoinPoint(JoinPoint.CALL_AFTER,
                                                "handler.execute",
                                                "selectAllPeriod",
                                                true);
    private static final String ASPECT_ID = "MyAspect";


    public void test_aspectExecution() throws Exception {
        AspectManager aspectManager = new AspectManager();
        aspectManager.addAspect(ASPECT_ID, new JoinPoint[]{joinPoint}, MyAspect.class);

        story.installService(MySecurityService.class);

        story.record()
              .startAgent("ze-executor",
                          new AspectExecutorAgent(new JdbcServiceUtilMock(new LogString()),
                                                  createAspectLauncher(aspectManager),
                                                  NOT_DELEGATE));

        story.record().assertNumberOfAgentWithService(1, ExecuteAspectRequest.JOB_ID);

        story.record().startTester("a-schedule-leader")
              .sendMessage(createJobRequestMessage(executeAspect(ASPECT_ID), new Aid("ze-executor")))
              .then()
              .receiveMessage(containsAudit(Type.PRE, Status.OK))
              .then()
              .receiveMessage(containsAudit(Type.POST, Status.OK));

        story.execute();

        log.assertContent("MyAspect.setUp(), MyAspect.run(), MyAspect.cleanUp()");
    }


    public void test_aspectExecution_failure() throws Exception {

        AspectManager aspectManager = new AspectManager();
        aspectManager.addAspect(ASPECT_ID, new JoinPoint[]{joinPoint}, MyFailingAspect.class);

        story.installService(MySecurityService.class);

        story.record()
              .startAgent("ze-executor",
                          new AspectExecutorAgent(new JdbcServiceUtilMock(new LogString()),
                                                  createAspectLauncher(aspectManager),
                                                  NOT_DELEGATE));

        story.record().assertNumberOfAgentWithService(1, ExecuteAspectRequest.JOB_ID);

        story.record().startTester("a-schedule-leader")
              .sendMessage(createJobRequestMessage(executeAspect(ASPECT_ID), new Aid("ze-executor")))
              .then()
              .receiveMessage(containsAudit(Type.PRE, Status.OK))
              .then()
              .receiveMessage(containsAudit(Type.POST, Status.ERROR))
              .assertReceivedMessage(hasAuditErrorMessage("I have failed:"
                                                          + " Erreur lors de l'exécution de l'aspect "
                                                          + "'AspectBranchId{JoinPoint{after, handler.execute(selectAllPeriod), fork = true}, aspectId='MyAspect'}'"
                                                          + " en mode fork."));

        story.execute();
    }


    public void test_aspectExecution_inDelegateMode() throws Exception {
        AspectManager aspectManager = new AspectManager();
        aspectManager.addAspect(ASPECT_ID, new JoinPoint[]{joinPoint}, MyAspect.class);

        story.installService(MySecurityService.class);

        story.record()
              .startAgent("ze-executor",
                          new AspectExecutorAgent(new JdbcServiceUtilMock(new LogString()),
                                                  createAspectLauncher(aspectManager),
                                                  DELEGATE));

        story.record().startTester("a-schedule-leader")
              .sendMessage(createJobRequestMessage(executeAspect(ASPECT_ID), new Aid("ze-executor")))
              .then()
              .receiveMessage(containsAudit(Type.PRE, Status.OK))
              .then()
              .receiveMessage(containsAudit(Type.POST, Status.OK));

        story.record().addAssert(log(log, "MyAspect.setUp(), MyAspect.run(), MyAspect.cleanUp()"));

        story.record().assertNumberOfAgentWithService(0, ExecuteAspectRequest.JOB_ID);

        story.execute();
    }


    private AspectLauncher createAspectLauncher(AspectManager aspectManager) {
        return new AspectLauncher(aspectManager,
                                  new JDO(),
                                  null);
    }


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        aspectLog = new LogString(ASPECT_ID, log);
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        aspectLog = null;
    }


    private JobRequest executeAspect(String aspectId) {
        ExecuteAspectRequest aspectRequest = new ExecuteAspectRequest();

        aspectRequest.setAspectBranchId(new AspectBranchId(joinPoint, aspectId));
        aspectRequest.setAspectContext(new AspectContext());

        return aspectRequest.toRequest();
    }


    public static class MySecurityService extends SecurityService {

        public ServiceHelper getServiceHelper(Agent agent) {
            return new SecurityServiceHelperMock();
        }
    }
    public static class MyAspect implements Aspect {

        public void setUp(AspectContext context, JoinPoint joinPoint) throws AspectException {
            aspectLog.call("setUp");
        }


        public void run(AspectContext context) throws AspectException {
            aspectLog.call("run");
        }


        public void cleanUp(AspectContext context) throws AspectException {
            aspectLog.call("cleanUp");
        }
    }
    public static class MyFailingAspect implements Aspect {

        public void setUp(AspectContext context, JoinPoint joinPoint) throws AspectException {
        }


        public void run(AspectContext context) throws AspectException {
            throw new AspectException("I have failed");
        }


        public void cleanUp(AspectContext context) throws AspectException {
        }
    }
}
