package net.codjo.workflow.server.plugin;
import net.codjo.agent.ContainerConfigurationMock;
import net.codjo.agent.UserId;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.mad.server.plugin.MadServerPluginMock;
import net.codjo.plugin.common.session.SessionManager;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.Service;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.server.aspect.ExecuteAspectRequest;
import net.codjo.workflow.server.audit.Stringifier;
import net.codjo.workflow.server.handler.HandlerContextManagerMock;
import net.codjo.workflow.server.handler.WorkflowHandlerExecutorFactory;
import net.codjo.workflow.server.organiser.DefaultJobFactory;
import net.codjo.workflow.server.organiser.JobBuilder;
import net.codjo.workflow.server.organiser.RuleEngineMock;
import java.io.File;
import static java.lang.Integer.MIN_VALUE;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class WorkflowServerPluginTest {
    private WorkflowServerPlugin plugin;
    private LogString log = new LogString();
    private AgentContainerFixture fixture = new AgentContainerFixture();
    private SessionManager sessionManager = new SessionManager();
    private HandlerContextManagerMock handlerContextManager = new HandlerContextManagerMock(log);
    private RuleEngineMock ruleEngineMock = new RuleEngineMock(log);


    @Before
    public void setUp() throws Exception {
        fixture.doSetUp();
        plugin = new WorkflowServerPlugin(new JobFactoryMock(),
                                          ruleEngineMock,
                                          new WorkflowHandlerExecutorFactory(handlerContextManager),
                                          handlerContextManager,
                                          sessionManager);
        plugin.getConfiguration().enableHandlerExecution(true);
    }


    @After
    public void tearDown() throws Exception {
        fixture.doTearDown();
    }


    @Test
    public void test_other() throws Exception {
        plugin.initContainer(new ContainerConfigurationMock(log));
        plugin.stop();
        log.assertContent("");
    }


    @Test
    public void test_start() throws Exception {
        plugin = new WorkflowServerPlugin();
        plugin.start(fixture.getContainer());
        fixture.assertNumberOfAgentWithService(1, Service.JOB_LEADER_SERVICE);
        fixture.assertNumberOfAgentWithService(1, Service.ORGANISER_SERVICE);
        fixture.assertNumberOfAgentWithService(0, ExecuteAspectRequest.JOB_ID);
    }


    @Test
    public void test_start_withMadServerPlugin() throws Exception {
        plugin = new WorkflowServerPlugin(new MadServerPluginMock(), new SessionManager());
        plugin.start(fixture.getContainer());
        fixture.assertNumberOfAgentWithService(3, ExecuteAspectRequest.JOB_ID);
    }


    @Test
    public void test_start_invalidRuleFile() throws Exception {
        ruleEngineMock.addRulesFile(new File("je n'existe pas"));

        try {
            plugin.start(fixture.getContainer());
        }
        catch (Exception e) {
            assertEquals("Erreur lors du chargement du fichier de règles 'je n'existe pas' !!!",
                         e.getLocalizedMessage());
        }
    }


    @Test
    public void test_configBuilder() throws Exception {
        plugin.start(fixture.getContainer());

        plugin.getConfiguration().registerJobBuilder(new JobBuilder());
        plugin.getConfiguration().registerJobBuilder(new JobBuilder(), 5);

        log.assertContent(
              "register(class net.codjo.workflow.server.organiser.DescriptionJobBuilder, " + MIN_VALUE + ")",
              "register(class net.codjo.workflow.server.handler.HandlerJobBuilder, 0)",
              "register(class net.codjo.workflow.server.organiser.JobBuilder, 0)",
              "register(class net.codjo.workflow.server.organiser.JobBuilder, 5)");
    }


    @Test
    public void test_addRulesFile() throws Exception {
        File ruleFile1 = new File("file1");
        File ruleFile2 = new File("file2");
        plugin.getConfiguration().addRulesFile(ruleFile1);
        plugin.getConfiguration().addRulesFile(ruleFile2);

        log.assertContent(String.format("addRulesFile(%s)", ruleFile1.toURL()),
                          String.format("addRulesFile(%s)", ruleFile2.toURL()));
    }


    @Test
    public void test_stringifierConfiguration() throws Exception {
        Stringifier specialStringifier = new Stringifier() {
            public String toString(JobRequest jobRequest) {
                return "yes";
            }
        };
        plugin.getConfiguration().setDiscriminentStringifier("specialJob", specialStringifier);

        Map<String, Stringifier> stringifiers = plugin.getConfiguration().getDiscriminentStringifiers();
        assertEquals(3, stringifiers.size());
        assertNull(stringifiers.get("aJob"));
        assertEquals(specialStringifier, stringifiers.get("specialJob"));
    }


    @Test
    public void test_cleanHandlerContextManager() throws Exception {
        plugin.start(fixture.getContainer());

        UserId userId = UserId.createId("myLogin", "myPassword");
        sessionManager.stopSession(userId);

        log.assertContent(Pattern.compile(String.format(".*cleanUserContext\\(%s\\).*", userId)));
    }


    private class JobFactoryMock extends DefaultJobFactory {

        @Override
        public void register(JobBuilder newBuilder, int priority) {
            log.call("register", newBuilder.getClass(), priority);
        }
    }
}
