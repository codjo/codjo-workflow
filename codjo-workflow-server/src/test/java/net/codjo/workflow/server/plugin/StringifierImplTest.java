package net.codjo.workflow.server.plugin;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.server.audit.Stringifier;
import java.util.Map;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;
/**
 *
 */
public class StringifierImplTest {
    private WorkflowServerPlugin workflowServerPlugin = new WorkflowServerPlugin();
    private StringifierImpl stringifier;


    @Before
    public void setUp() {
        stringifier = new StringifierImpl("test") {
            public String toString(JobRequest jobRequest) {
                return "test";
            }
        };
    }


    @Test
    public void test_install() throws Exception {
        stringifier.install(null);

        stringifier.install(workflowServerPlugin);

        Map<String, Stringifier> stringifiers =
              workflowServerPlugin.getConfiguration().getDiscriminentStringifiers();
        assertSame(stringifier, stringifiers.get("test"));
    }
}
