/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
/**
 * Classe de test de {@link JobRequestWrapper}.
 */
public class JobRequestWrapperTest extends JobRequestWrapperTestCase {
    public void test_constructWithoutJobRequest() {
        JobRequestWrapper wrapper = createWrapper(null);
        assertNotNull(wrapper.toRequest());
    }


    protected String getJobRequestType() {
        return "test-type";
    }


    protected JobRequestWrapper createWrapper(JobRequest jobRequest) {
        return new JobRequestWrapper(getJobRequestType(), jobRequest);
    }
}
