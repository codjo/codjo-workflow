package net.codjo.workflow.common.message;
import java.sql.Date;
import java.text.SimpleDateFormat;
import junit.framework.TestCase;
import org.junit.Test;
/**
 *
 */
public class DateUtilTest extends TestCase {
    @Test
    public void test_computeSqlDateFromPeriod() throws Exception {
        Date periodAsDate = DateUtil.computeSqlDateFromPeriod("07");
        String periodAsString = DateUtil.computeStringDateFromPeriod("07");

        assertEquals(new SimpleDateFormat("yyyy-MM-dd HH:mm:SS").format(periodAsDate.getTime()), periodAsString);
    }
}
