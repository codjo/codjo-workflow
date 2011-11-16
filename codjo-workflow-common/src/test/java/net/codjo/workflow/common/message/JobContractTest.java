package net.codjo.workflow.common.message;
import junit.framework.TestCase;
/**
 *
 */
public class JobContractTest extends TestCase {
    public void test_serializable() throws Exception {
        assertNotNull(TestUtil.serialize(createContract()));
    }


    private JobContract createContract() {
        return new JobContract(new JobRequest());
    }
}
