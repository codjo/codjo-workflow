package net.codjo.workflow.common.organiser;
import net.codjo.workflow.common.organiser.Job.State;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class JobTest {

    @Test
    public void test_constructor() throws Exception {
        Job job1 = new Job("myId", "test");
        job1.setDescription("my description");
        job1.setInitiator("my initiator");
        job1.setUserId("myLogin");

        assertSame(job1.getState(), Job.State.NEW);
        assertEquals("myId", job1.getId());
        assertEquals("test", job1.getType());

        job1.getArgs().put("key1", "value1");

        Job job2 = new Job(job1);
        assertSame(job2.getState(), Job.State.NEW);
        assertEquals("myId", job2.getId());
        assertEquals("test", job2.getType());
        assertEquals("value1", job2.getArgs().get("key1"));
        assertEquals("my description", job2.getDescription());
        assertEquals("my initiator", job2.getInitiator());
        assertEquals("myLogin", job2.getUserId());
    }


    @Test
    public void test_isFinished() throws Exception {
        Job job = new Job("myId", "test");
        assertFalse(job.isFinished());

        job.setState(State.NEW);
        assertFalse(job.isFinished());

        job.setState(State.WAITING);
        assertFalse(job.isFinished());

        job.setState(State.RUNNING);
        assertFalse(job.isFinished());

        job.setState(State.DONE);
        assertTrue(job.isFinished());

        job = new Job("myId", "test");
        job.setState(State.RUNNING);
        assertFalse(job.isFinished());
        job.setState(State.FAILURE);
        assertTrue(job.isFinished());

        job = new Job("myId", "test");
        job.setState(State.REJECTED);
        assertTrue(job.isFinished());
    }


    @Test
    public void test_setStateNominal() throws Exception {
        Job job = new Job("myId", "test");
        job.setState(State.NEW);
        job.setState(State.WAITING);
        job.setState(State.RUNNING);
        job.setState(State.DONE);

        job = new Job("myId", "test");
        job.setState(State.NEW);
        job.setState(State.WAITING);
        job.setState(State.RUNNING);
        job.setState(State.FAILURE);

        job = new Job("myId", "test");
        job.setState(State.NEW);
        job.setState(State.REJECTED);
    }


    @Test
    public void test_setStateNewToUnauthorized() throws Exception {
        assertTransitionFailed(State.NEW, State.DONE);
        assertTransitionFailed(State.NEW, State.FAILURE);
    }


    @Test
    public void test_setStateWaitingToUnauthorized() throws Exception {
        assertTransitionFailed(State.WAITING, State.REJECTED);
        assertTransitionFailed(State.WAITING, State.DONE);
        assertTransitionFailed(State.WAITING, State.FAILURE);
    }


    @Test
    public void test_setStateRunningToUnauthorized() throws Exception {
        assertTransitionFailed(State.RUNNING, State.NEW);
        assertTransitionFailed(State.RUNNING, State.REJECTED);
        assertTransitionFailed(State.RUNNING, State.WAITING);
    }


    @Test
    public void test_setStateDoneToUnauthorized() throws Exception {
        assertTransitionFailed(State.DONE, State.NEW);
        assertTransitionFailed(State.DONE, State.REJECTED);
        assertTransitionFailed(State.DONE, State.WAITING);
        assertTransitionFailed(State.DONE, State.RUNNING);
        assertTransitionFailed(State.DONE, State.FAILURE);
    }


    @Test
    public void test_setStateFailureToUnauthorized() throws Exception {
        assertTransitionFailed(State.FAILURE, State.NEW);
        assertTransitionFailed(State.FAILURE, State.REJECTED);
        assertTransitionFailed(State.FAILURE, State.WAITING);
        assertTransitionFailed(State.FAILURE, State.RUNNING);
        assertTransitionFailed(State.FAILURE, State.DONE);
    }


    private void assertTransitionFailed(State stateFrom, State stateTo) {
        Job job = JobMock.create("myId", "test", stateFrom);
        try {
            job.setState(stateTo);
            fail();
        }
        catch (JobStateException e) {
            assertEquals(String.format("Unable to change the state from %s to %s",
                                       stateFrom.name(),
                                       stateTo.name()), e.getMessage());
        }
    }
}
