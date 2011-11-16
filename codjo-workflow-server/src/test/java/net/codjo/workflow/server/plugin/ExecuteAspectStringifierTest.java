package net.codjo.workflow.server.plugin;
import net.codjo.aspect.JoinPoint;
import net.codjo.mad.server.handler.XmlCodec;
import net.codjo.mad.server.handler.aspect.AspectBranchId;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobRequest;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
/**
 *
 */
public class ExecuteAspectStringifierTest {
    private ExecuteAspectStringifier stringifier = new ExecuteAspectStringifier();


    @Test
    public void test_toString() throws Exception {
        Arguments arguments = new Arguments("aspect-branch-id",
                                            XmlCodec.toXml(new AspectBranchId(new JoinPoint(), "my-aspect")));

        assertEquals("my-aspect", stringifier.toString(new JobRequest("execute-aspect", arguments)));
    }
}
