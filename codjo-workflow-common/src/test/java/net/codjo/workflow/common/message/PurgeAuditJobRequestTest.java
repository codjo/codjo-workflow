package net.codjo.workflow.common.message;
/**
 *
 */
public class PurgeAuditJobRequestTest extends JobRequestWrapperTestCase {

    public void test_createFromJobRequest() throws Exception {
        Arguments arguments = new Arguments();
        arguments.put(PurgeAuditJobRequest.PERIOD, "7");

        PurgeAuditJobRequest purgeAuditJobRequest = new PurgeAuditJobRequest(new JobRequest("", arguments));

        assertEquals(arguments, purgeAuditJobRequest.toRequest().getArguments());
        assertEquals(DateUtil.computeSqlDateFromPeriod("7"), purgeAuditJobRequest.getBeforeDate());
    }


    @Override
    protected String getJobRequestType() {
        return PurgeAuditJobRequest.PURGE_AUDIT_JOB_TYPE;
    }


    @Override
    protected JobRequestWrapper createWrapper(JobRequest jobRequest) {
        return new PurgeAuditJobRequest(jobRequest);
    }
}
