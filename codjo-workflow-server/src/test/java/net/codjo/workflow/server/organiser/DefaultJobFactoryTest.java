package net.codjo.workflow.server.organiser;
import net.codjo.agent.UserId;
import static net.codjo.test.common.matcher.JUnitMatchers.*;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.JobMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Test;

public class DefaultJobFactoryTest {
    private DefaultJobFactory jobBuilder = new DefaultJobFactory();
    private UserId userId = UserId.createId("loginTest", "passwordTest");


    @Test
    public void test_noHandler() throws Exception {
        assertEquals("test", jobBuilder.createJob(new JobRequest("test"), userId).getType());
        assertEquals("default", jobBuilder.createJob(new JobRequest("default"), userId).getType());
    }


    @Test
    public void test_noHandler_args() throws Exception {
        Arguments arguments = new Arguments();
        arguments.put("key1", "value1");
        arguments.put("key2", "value2");

        Job job = jobBuilder.createJob(new JobRequest("test", arguments), userId);

        assertEquals("test", job.getType());
        assertEquals("value1", job.getArgs().get("key1"));
        assertEquals("value2", job.getArgs().get("key2"));
    }


    @Test
    public void test_createJob() throws Exception {
        Job broadcastJob = new JobMock();
        jobBuilder.register(new JobBuilderMock("broadcast", broadcastJob, userId), 0);
        Job importJob = new JobMock();
        jobBuilder.register(new JobBuilderMock("import", importJob, userId), 0);

        assertSame(importJob, jobBuilder.createJob(new JobRequest("import"), userId));
        assertSame(broadcastJob, jobBuilder.createJob(new JobRequest("broadcast"), userId));
    }


    @Test
    public void test_createJob_highPriority() throws Exception {
        Job job1 = new JobMock();
        jobBuilder.register(new JobBuilderMock("test", job1, userId), 100);
        Job job2 = new JobMock();
        jobBuilder.register(new JobBuilderMock("test", job2, userId), 0);

        assertSame(job1, jobBuilder.createJob(new JobRequest("test"), userId));
    }


    @Test
    public void test_createJob_error() throws Exception {
        jobBuilder.register(new JobBuilder() {
            @Override
            public Job createJob(JobRequest jobRequest, UserId userId) {
                throw new RuntimeException("Erreur pour le test !!!");
            }
        }, 0);

        try {
            jobBuilder.createJob(new JobRequest("import"), null);
            fail();
        }
        catch (JobBuilderException e) {
            assertThat(e.getMessage(), containsString("Erreur pour le test !!!"));
        }
    }
}
