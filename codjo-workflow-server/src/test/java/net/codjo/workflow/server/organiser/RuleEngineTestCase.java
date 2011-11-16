package net.codjo.workflow.server.organiser;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.Job.State;
import net.codjo.workflow.server.handler.HandlerJob;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;
/**
 *
 */
public class RuleEngineTestCase {
    private final Map<Object, Job> jobs = new HashMap<Object, Job>();
    private final RuleEngine engine = new RuleEngine();


    protected void receiveNewJob(String id, String type) {
        receiveNewJob(id, new Job(id, type));
    }


    protected void receiveNewJob(String id, JobBuilder builder) {
        receiveNewJob(id, builder.toJob());
    }


    protected void receiveNewJob(String id, Job job) {
        jobs.put(id, job);
        engine.insert(job);
    }


    protected Job receiveNewJob(String type) {
        return receiveNewJob(jobBuilder(type).toJob());
    }


    protected Job receiveNewJob(JobBuilder builder) {
        return receiveNewJob(builder.toJob());
    }


    protected Job receiveNewJob(Job job) {
        jobs.put(job.getId(), job);
        engine.insert(job);
        return job;
    }


    protected void jobDone(String id) {
        Job job = jobs.get(id);
        if (job == null) {
            throw new RuntimeException(String.format("Le job %s n'est pas connu !!!", id));
        }
        jobDone(job);
    }


    protected void jobDone(Job job) {
        job.setState(State.DONE);
        engine.retract(job);
    }


    protected void jobFailed(String id) {
        Job job = jobs.get(id);
        if (job == null) {
            throw new RuntimeException(String.format("Le job %s n'est pas connu !!!", id));
        }
        jobFailed(job);
    }


    protected void jobFailed(Job job) {
        job.setState(State.FAILURE);
        engine.retract(job);
    }


    public static void assertJobRunning(Job job) {
        assertSame(String.format("%s.getState()", job.getId()), State.RUNNING, job.getState());
    }


    protected void assertJobRunning(String... ids) {
        for (String id : ids) {
            assertJobRunning(jobs.get(id));
        }
    }


    public static void assertJobWaiting(Job job) {
        assertSame(String.format("%s.getState()", job.getId()), State.WAITING, job.getState());
    }


    protected void assertJobWaiting(String... ids) {
        for (String id : ids) {
            assertJobWaiting(jobs.get(id));
        }
    }


    protected void assertNoJobWaiting() {
        for (Job job : jobs.values()) {
            assertNotSame(String.format("%s.getState()==WAITING", job.getId()),
                          State.WAITING,
                          job.getState());
        }
    }


    public static void assertJobRejected(Job job) {
        assertSame(State.REJECTED, job.getState());
    }


    protected static JobBuilder jobBuilder(String type) {
        return new JobBuilder(type);
    }


    @Test
    public void test_handlers() throws Exception {
        Job handler1 = receiveNewJob(new HandlerJob(jobBuilder("handler").setUserId("userId1").toJob()));
        Job handler2 = receiveNewJob(new HandlerJob(jobBuilder("handler").setUserId("userId1").toJob()));
        Job handler3 = receiveNewJob(new HandlerJob(jobBuilder("handler").setUserId("userId2").toJob()));
        Job otherJob = receiveNewJob(jobBuilder("not_handler").toJob());

        assertJobRunning(handler1);
        assertJobWaiting(handler2);
        assertJobRunning(handler3);
        assertJobRunning(otherJob);

        jobFailed(handler1);
        jobDone(otherJob);

        assertJobRunning(handler2);
        assertJobRunning(handler3);
    }


    @Before
    public void setUp() throws Exception {
        doSetup(engine);
        engine.start();
    }


    protected void doSetup(RuleEngine ruleEngine) {
    }


    protected static class JobBuilder {
        private Job job;


        protected JobBuilder(String type) {
            job = new Job(Math.random(), type);
        }


        public JobBuilder setUserId(String userId) {
            job.setUserId(userId);
            return this;
        }


        public JobBuilder setDate(Date date) {
            job.setDate(date);
            return this;
        }


        public JobBuilder setErrorMessage(String errorMessage) {
            job.setErrorMessage(errorMessage);
            return this;
        }


        public JobBuilder setTable(String table) {
            job.setTable(table);
            return this;
        }


        public JobBuilder setDescription(String description) {
            job.setDescription(description);
            return this;
        }


        public JobBuilder setInitiator(String initiator) {
            job.setInitiator(initiator);
            return this;
        }


        public Job toJob() {
            return job;
        }
    }
}
