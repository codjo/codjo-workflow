package net.codjo.workflow.gui.plugin;
import net.codjo.agent.AgentContainerMock;
import net.codjo.agent.ContainerConfiguration;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.mad.gui.base.GuiConfigurationMock;
import net.codjo.test.common.LogString;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
/**
 *
 */
public class WorkflowGuiPluginTest {
    private LogString log = new LogString();
    private WorkflowGuiPlugin plugin = new WorkflowGuiPlugin();
    private AgentContainerFixture agentContainer = new AgentContainerFixture();


    @Before
    public void setUp() {
        agentContainer.doSetUp();
    }


    @After
    public void tearDown() throws Exception {
        agentContainer.doTearDown();
    }


    @Test
    public void test_init() throws Exception {

        plugin.initGui(new GuiConfigurationMock(log));

        log.assertContent("registerAction(WorkflowGuiPlugin, ConsoleAction, ConsoleAction)",
                          "addToStatusBar(ButtonBuilder)");
    }


    @Test
    public void test_start() throws Exception {
        agentContainer = new AgentContainerFixture();

        ContainerConfiguration configuration = new ContainerConfiguration();
        configuration.setParameter("login", "user_login");
        plugin.initContainer(configuration);
        plugin.start(new AgentContainerMock(log));

        log.assertContent(Pattern.compile("acceptNewAgent\\(taskManager-user_login.*\\)"));
    }
}
