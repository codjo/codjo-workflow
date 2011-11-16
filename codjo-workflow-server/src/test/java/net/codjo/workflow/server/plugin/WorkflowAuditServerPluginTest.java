package net.codjo.workflow.server.plugin;
import net.codjo.agent.test.Story;
import net.codjo.workflow.common.message.PurgeAuditJobRequest;
import net.codjo.workflow.server.audit.AuditDao;
import net.codjo.workflow.server.audit.JdbcAuditDao;
import org.junit.After;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class WorkflowAuditServerPluginTest {
    private WorkflowServerPlugin plugin = new WorkflowServerPlugin();
    private Story story = new Story();


    @Before
    public void setUp() throws Exception {
        story.doSetUp();
    }


    @After
    public void tearDown() throws Exception {
        story.doTearDown();
    }


    @Test
    public void test_init() throws Exception {
        WorkflowAuditServerPlugin auditServerPlugin = new WorkflowAuditServerPlugin(plugin);

        assertSame(AuditDao.NULL, plugin.getConfiguration().getAuditDao());

        auditServerPlugin.initContainer(null);

        AuditDao actualAuditDao = plugin.getConfiguration().getAuditDao();
        assertNotSame(AuditDao.NULL, actualAuditDao);
        assertTrue(actualAuditDao instanceof JdbcAuditDao);
    }


    @Test
    public void test_start() throws Exception {
        final WorkflowAuditServerPlugin auditServerPlugin = new WorkflowAuditServerPlugin(plugin);

        story.record().addAction(new net.codjo.agent.test.AgentContainerFixture.Runnable() {
            public void run() throws Exception {
                auditServerPlugin.initContainer(null);
                auditServerPlugin.start(story.getContainer());
            }
        });

        story.record().assertNumberOfAgentWithService(2, PurgeAuditJobRequest.PURGE_AUDIT_JOB_TYPE);

        story.execute();
    }
}
