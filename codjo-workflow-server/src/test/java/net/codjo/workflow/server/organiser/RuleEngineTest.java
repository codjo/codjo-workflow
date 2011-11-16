package net.codjo.workflow.server.organiser;
import net.codjo.test.common.PathUtil;
import net.codjo.test.common.matcher.JUnitMatchers;
import net.codjo.workflow.common.organiser.Job;
import static net.codjo.workflow.common.organiser.Job.State;
import net.codjo.workflow.common.organiser.JobMock;
import net.codjo.workflow.server.handler.HandlerJob;
import static net.codjo.workflow.server.organiser.RuleEngineTestCase.assertJobRunning;
import static net.codjo.workflow.server.organiser.RuleEngineTestCase.assertJobWaiting;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.Description;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.matchers.TypeSafeMatcher;

public class RuleEngineTest {
    private RuleEngine engine;
    private JobMock job1 = JobMock.create("job1", "mock", State.NEW);
    private JobMock job2 = JobMock.create("job2", "mock", State.NEW);
    private JobMock job3 = JobMock.create("job3", "mock", State.NEW);


    @Before
    public void setUp() {
        engine = new RuleEngine();
    }


    @Test
    public void test_checkRuleFiles() throws Exception {
        engine.addRulesFile(getClass().getResource("invalid.drl"));

        try {
            engine.checkRuleFiles();
            fail();
        }
        catch (Exception e) {
            assertEquals("Erreur lors du chargement du fichier de règles 'invalid.drl' !!!",
                         e.getLocalizedMessage());
        }
    }


    @Test
    public void test_defaultRulesFile() throws Exception {
        engine.start();

        assertEquals(State.NEW, job1.getState());
        assertEquals(State.NEW, job2.getState());
        assertEquals(State.NEW, job3.getState());

        engine.insert(job1);
        engine.insert(job2);
        engine.insert(job3);

        assertJobRunning(job1);
        assertJobWaiting(job2);
        assertJobWaiting(job3);

        engine.retract(job1);

        assertJobRunning(job2);
        assertJobWaiting(job3);
    }


    @Test
    public void test_defaultRulesFile_handler() throws Exception {
        engine.start();

        Job handler1ForBob = createHandlerJob("handler1ForBob", "bob");
        Job handler2ForBob = createHandlerJob("handler2ForBob", "bob");
        Job job1ForBob = createJob("job1ForBob", "dummyJob", "bob");
        Job handler1ForRebecca = createHandlerJob("handler1ForRebecca", "rebecca");
        Job job1ForRebecca = createJob("job1ForRebecca", "dummyJob", "rebecca");

        engine.insert(handler1ForBob);
        engine.insert(handler2ForBob);
        engine.insert(job1ForBob);
        engine.insert(handler1ForRebecca);
        engine.insert(job1ForRebecca);

        assertEquals(State.RUNNING, handler1ForBob.getState());
        assertEquals(State.WAITING, handler2ForBob.getState());
        assertEquals(State.RUNNING, job1ForBob.getState());
        assertEquals(State.RUNNING, handler1ForRebecca.getState());
        assertEquals(State.WAITING, job1ForRebecca.getState());
    }


    @Test
    public void test_addRuleFiles() throws Exception {
        engine.addRulesFile(createRulesFile("nominal.drl"));
        engine.addRulesFile(createRulesFile("onlyOneJobRunning.drl"));

        Assert.assertArrayEquals(new URL[]{createRulesFile("nominal.drl").toURL(),
                                           createRulesFile("onlyOneJobRunning.drl").toURL()},
                                 engine.getRuleFiles().toArray());
    }


    @Test
    public void test_nominal() throws Exception {
        engine.addRulesFile(createRulesFile("nominal.drl"));
        engine.start();

        assertEquals(State.NEW, job1.getState());
        assertEquals(State.NEW, job2.getState());

        engine.insert(job1);
        engine.insert(job2);

        assertEquals(State.RUNNING, job1.getState());
        assertEquals(State.RUNNING, job2.getState());
    }


    @Test
    public void test_args() throws Exception {
        job2.getArgs().put("nothing", "another value");
        job2.getArgs().put("something", "THE Value");
        job3.getArgs().put("nothing", "some value");
        engine.addRulesFile(createRulesFile("args.drl"));
        engine.start();

        engine.insert(job1);
        engine.insert(job2);
        engine.insert(job3);

        assertEquals(State.NEW, job1.getState());
        assertEquals(State.RUNNING, job2.getState());
        assertEquals(State.NEW, job3.getState());
    }


    @Test
    public void test_getAllJobs() throws Exception {
        engine.start();

        assertTrue(engine.getAllJobs().isEmpty());
    }


    @Test
    public void test_getAllJobs_notStarted() throws Exception {
        try {
            engine.getAllJobs();
            JUnitMatchers.fail();
        }
        catch (RuntimeException e) {
            assertEquals("Le moteur d'inférence doit être démarré", e.getMessage());
        }
    }


    @Test
    public void test_retry() throws Exception {
        engine.addRulesFile(createRulesFile("onlyOneJobRunning.drl"));
        engine.start();

        engine.insert(job1);
        assertSame(State.RUNNING, job1.getState());

        engine.insert(job2);
        assertSame(State.WAITING, job2.getState());

        engine.insert(job3);
        assertSame(State.WAITING, job3.getState());

        engine.retract(job1);

        assertSame(State.RUNNING, job2.getState());
        assertSame(State.WAITING, job3.getState());
    }


    @Test
    public void test_insert() throws Exception {
        engine.addRulesFile(createRulesFile("onlyOneJobRunning.drl"));
        engine.start();

        engine.insert(job1);
        engine.insert(job2);

        assertSame(State.RUNNING, job1.getState());
        assertSame(State.WAITING, job2.getState());
    }


    @Test
    public void test_insert_notStarted() throws Exception {
        try {
            engine.insert(job1);
            JUnitMatchers.fail();
        }
        catch (RuntimeException e) {
            assertEquals("Le moteur d'inférence doit être démarré", e.getMessage());
        }
    }


    @Test
    public void test_retract() throws Exception {
        engine.start();
        engine.insert(job1);
        engine.insert(job2);

        engine.retract(job1);
        engine.retract(job1);

        assertThat(engine.getAllJobs(), ListMatcher.hasAllItems(job2));
    }


    @Test
    public void test_retract_notStarted() throws Exception {
        try {
            engine.retract(job1);
            JUnitMatchers.fail();
        }
        catch (RuntimeException e) {
            assertEquals("Le moteur d'inférence doit être démarré", e.getMessage());
        }
    }


    @Test
    public void test_retract_nothing() throws Exception {
        engine.start();
        engine.insert(job2);

        engine.retract(job1);

        assertThat(engine.getAllJobs(), ListMatcher.hasAllItems(job2));
    }


    @Test
    public void test_getRunningJobs() throws Exception {
        engine.addRulesFile(createRulesFile("onlyTwoJobRunning.drl"));
        engine.start();

        engine.insert(job1);
        engine.insert(job2);
        engine.insert(job3);

        assertThat(engine.getRunningJobs(), ListMatcher.hasAllItems(job1, job2));
    }


    @Test
    public void test_jobThrowsException() throws Exception {
        engine.addRulesFile(createRulesFile("jobThrowsException.drl"));
        engine.start();

        try {
            engine.insert(job2);
            fail();
        }
        catch (Exception e) {
            assertEquals("java.lang.RuntimeException: Exception in job !!!", e.getMessage());
        }
    }


    private JobMock createJob(String requestId, String requestType, String initiator) {
        JobMock job1ForBob = createJob(requestId, requestType);
        job1ForBob.setInitiator(initiator);
        return job1ForBob;
    }


    private JobMock createJob(String requestId, String requestType) {
        return JobMock.create(requestId, requestType, State.NEW);
    }


    private HandlerJob createHandlerJob(String requestId, String userId) {
        HandlerJob handler1ForBob = new HandlerJob(createJob(requestId, "handler"));
        handler1ForBob.setUserId(userId);
        return handler1ForBob;
    }


    private File createRulesFile(String filePath) {
        return PathUtil.find(this.getClass(), filePath);
    }


    private static class ListMatcher extends TypeSafeMatcher<List<Job>> {
        private List<Job> jobs;


        private ListMatcher(Job... items) {
            jobs = Arrays.asList(items);
        }


        public static ListMatcher hasAllItems(Job... items) {
            return new ListMatcher(items);
        }


        @Override
        public boolean matchesSafely(List<Job> item) {
            return jobs.size() == item.size() && jobs.containsAll(item);
        }


        public void describeTo(Description description) {
            description.appendValue(jobs);
        }
    }
}
