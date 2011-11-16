package net.codjo.workflow.server.organiser;
import net.codjo.agent.UserId;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.Job.State;
import net.codjo.workflow.common.organiser.JobMock;
import java.util.Date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class JobBuilderTest {
    private JobBuilder jobBuilder;
    private UserId userId = UserId.createId("loginTest", "passwordTest");
    private JobRequest jobRequest = new JobRequest("test");


    @Before
    public void setUp() {
        jobBuilder = new JobBuilder();
        jobBuilder.setPriority(Integer.MIN_VALUE);
    }


    @Test
    public void test_handleJobRequest() throws Exception {
        JobBuilder rootBuilder = jobBuilder;

        long beforeCreate = System.currentTimeMillis();
        Job job = rootBuilder.createJob(jobRequest, userId);
        long afterCreate = System.currentTimeMillis();
        assertEquals("test", job.getType());
        assertEquals("loginTest", job.getInitiator());
        assertEquals(userId.encode(), job.getUserId());
        assertTrue(job.getDate().compareTo(new Date(beforeCreate)) >= 0);
        assertTrue(job.getDate().compareTo(new Date(afterCreate)) <= 0);

        JobMock job1 = JobMock.create("job1", State.NEW);
        JobBuilderMock handler1Priority100 = new JobBuilderMock(job1, 100);
        rootBuilder = rootBuilder.add(handler1Priority100);

        assertEquals(job1, rootBuilder.createJob(jobRequest, userId));

        JobMock job2 = JobMock.create("job2", State.NEW);
        JobBuilderMock handler2Priority100 = new JobBuilderMock(job2, 100);
        rootBuilder = rootBuilder.add(handler2Priority100);

        assertEquals(job2, rootBuilder.createJob(jobRequest, userId));

        JobMock job3 = JobMock.create("job3", State.NEW);
        JobBuilderMock handler3Priority50 = new JobBuilderMock(job3, 50);
        rootBuilder = rootBuilder.add(handler3Priority50);

        assertEquals(job2, rootBuilder.createJob(jobRequest, userId));

        JobMock job4 = JobMock.create("job4", State.NEW);
        JobBuilderMock handler4Priority101 = new JobBuilderMock(job4, 101);
        rootBuilder = rootBuilder.add(handler4Priority101);

        assertEquals(job4, rootBuilder.createJob(jobRequest, userId));
    }


    @Test
    public void test_abstractJobBuilder_handleJobRequest() throws Exception {
        final JobMock jobMock = JobMock.create("test", State.NEW);
        final JobRequest jobRequestMock = new JobRequest("test");

        AbstractJobBuilder jobRequestHandler = new AbstractJobBuilder() {
            @Override
            public boolean accept(JobRequest jobRequest) {
                return jobRequestMock.equals(jobRequest);
            }


            @Override
            public Job createJob(JobRequest jobRequest, Job job, UserId userId) {
                assertSame(JobBuilderTest.this.userId, userId);
                return jobMock;
            }
        };

        assertNotNull(jobRequestHandler.createJob(new JobRequest(), userId));
        assertEquals(jobMock, jobRequestHandler.createJob(jobRequestMock, userId));
    }


    private class JobBuilderMock extends JobBuilder {
        private JobMock jobMock;


        private JobBuilderMock(JobMock jobMock, final int priority) {
            this.jobMock = jobMock;
            setPriority(priority);
        }


        @Override
        public Job createJob(JobRequest jobRequest, UserId userId) {
            return jobMock;
        }
    }
}
