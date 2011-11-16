package net.codjo.workflow.server.plugin;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.DateUtil;
import net.codjo.workflow.common.message.JobRequest;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
/**
 *
 */
public class PurgeAuditStringifierTest {
    private PurgeAuditStringifier stringifier = new PurgeAuditStringifier();


    @Test
    public void test_toString() throws Exception {
        Arguments arguments = new Arguments("period", "7");

        String expected = "Log < " + getBeforeDate("7");
        assertEquals(expected, stringifier.toString(new JobRequest("import", arguments)));
    }


    private String getBeforeDate(String period) {
        return PurgeAuditStringifier.DATE_FORMAT.format(DateUtil.computeSqlDateFromPeriod(period));
    }
}
