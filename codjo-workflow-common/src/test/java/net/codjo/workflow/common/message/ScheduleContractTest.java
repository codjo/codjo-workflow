package net.codjo.workflow.common.message;
import junit.framework.TestCase;
/**
 *
 */
public class ScheduleContractTest extends TestCase {
    public void test_serializable() throws Exception {
        ScheduleContract contract = createContract();

        ScheduleContract deserializedContract =
              (ScheduleContract)TestUtil.toObject(TestUtil.toByteArray(contract));

        assertNotNull(deserializedContract);
    }


    public void test_content() throws Exception {
        JobRequest jobRequest = new JobRequest();
        JobAudit postJobAudit = new JobAudit();

        ScheduleContract contract = new ScheduleContract(jobRequest, postJobAudit);

        assertSame(jobRequest, contract.getRequest());
        assertSame(postJobAudit, contract.getPostAudit());
    }


    public void test_previousContract() throws Exception {
        ScheduleContract previousContract = createContract();
        ScheduleContract contract = createContract();

        assertNull(contract.getPreviousContract());

        contract.setPreviousContract(previousContract);

        assertSame(previousContract, contract.getPreviousContract());
    }


    private ScheduleContract createContract() {
        return new ScheduleContract(new JobRequest(), new JobAudit());
    }
}
