package net.codjo.workflow.common.batch;
import net.codjo.agent.UserId;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.agent.test.Story;
import net.codjo.plugin.common.CommandLineArguments;
import static net.codjo.workflow.common.util.WorkflowSystem.workFlowSystem;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractWorkflowBatchPluginTestCase {
    protected static final UserId USER_ID = UserId.createId("polo", "secret");

    protected Story story = new Story();
    protected AbstractWorkflowBatchPlugin plugin;


    @Test
    public void test_execute() throws Exception {
        story.record().mock(workFlowSystem())
              .simulateJob(getExpectedLogContentAfterExecute())
              .forUser(USER_ID);

        story.record().addAction(new AgentContainerFixture.Runnable() {

            public void run() throws Exception {
                plugin.start(story.getContainer());
                plugin.execute(USER_ID, buildCommandLineArguments());
            }
        });

        story.execute();
    }


    @Test
    public void test_configuration() {
        Assert.assertNotNull(plugin.getConfiguration().getWorkflowConfiguration());
    }


    @Test
    public void test_doNothing() throws Exception {
        plugin.initContainer(null);
        plugin.stop();
    }


    @Test
    public void test_getType() {
        Assert.assertEquals(getExpectedType(), plugin.getType());
    }


    @Before
    public void setUp() throws Exception {
        story.doSetUp();
        plugin = createPlugin();
    }


    @After
    public void tearDown() throws Exception {
        story.doTearDown();
    }


    protected abstract String getExpectedLogContentAfterExecute();


    protected abstract CommandLineArguments buildCommandLineArguments();


    protected abstract String getExpectedType();


    protected abstract AbstractWorkflowBatchPlugin createPlugin();
}
