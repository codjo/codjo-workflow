package net.codjo.workflow.common.organiser;
import java.util.Date;

public class JobMock extends Job {

    @Deprecated
    public JobMock() {
        this("mock");
    }


    private JobMock(String type) {
        super(new Date(), type);
    }


    private JobMock(String id, String type) {
        super(id, type);
    }


    @Override
    public String toString() {
        if (getState() == State.FAILURE) {
            return String.format("Job(%s, %s, %s)", getType(), getState(), getErrorMessage());
        }
        return String.format("Job(%s, %s)", getType(), getState());
    }


    public void throwsException() {
        throw new RuntimeException("Exception in job !!!");
    }


    public static JobMock create(State state) {
        JobMock jobMock = new JobMock("anytype");
        jobMock.updateState(state);
        return jobMock;
    }


    public static JobMock create(String type, State state) {
        JobMock jobMock = new JobMock(type);
        jobMock.updateState(state);
        return jobMock;
    }


    public static JobMock create(String id, String type, State state) {
        JobMock jobMock = new JobMock(id, type);
        jobMock.updateState(state);
        return jobMock;
    }


    private void updateState(State state) {
        switch (state) {
            case NEW:
                setState(State.NEW);
                break;
            case REJECTED:
                setState(State.REJECTED);
                break;
            case WAITING:
                setState(State.WAITING);
                break;
            case RUNNING:
                setState(State.RUNNING);
                break;
            case DONE:
                setState(State.RUNNING);
                setState(State.DONE);
                break;
            case FAILURE:
                setState(State.RUNNING);
                setState(State.FAILURE);
                break;
        }
    }
}
