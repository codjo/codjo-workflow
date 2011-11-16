package net.codjo.workflow.common.organiser;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Job {
    private final Object id;
    private final String type;
    private State state = State.NEW;
    private final Map<String, String> args = new TreeMap<String, String>();
    private String table;
    private String description;
    private String initiator;
    private String userId;
    private Date date;
    private String errorMessage;

    private static final List<StateTransition> ALLOWED_STATE_TRANSITIONS;


    static {
        ALLOWED_STATE_TRANSITIONS = new ArrayList<StateTransition>();
        ALLOWED_STATE_TRANSITIONS.add(new StateTransition(State.NEW, State.REJECTED));
        ALLOWED_STATE_TRANSITIONS.add(new StateTransition(State.NEW, State.WAITING));
        ALLOWED_STATE_TRANSITIONS.add(new StateTransition(State.NEW, State.RUNNING));
        ALLOWED_STATE_TRANSITIONS.add(new StateTransition(State.WAITING, State.NEW));
        ALLOWED_STATE_TRANSITIONS.add(new StateTransition(State.WAITING, State.RUNNING));
        ALLOWED_STATE_TRANSITIONS.add(new StateTransition(State.RUNNING, State.DONE));
        ALLOWED_STATE_TRANSITIONS.add(new StateTransition(State.RUNNING, State.FAILURE));
    }


    public Job(Object id, String type) {
        this.id = id;
        this.type = type;
    }


    protected Job(Job job) {
        this(job.id, job.type);
        this.state = job.state;
        this.args.putAll(job.args);
        this.table = job.table;
        this.description = job.description;
        this.initiator = job.initiator;
        this.userId = job.userId;
        this.date = job.date;
    }


    public Object getId() {
        return id;
    }


    public String getType() {
        return type;
    }


    public State getState() {
        return state;
    }


    public void setState(State state) {
        if (this.state != state) {
            if (ALLOWED_STATE_TRANSITIONS.contains(new StateTransition(this.state, state))) {
                this.state = state;
            } else {
                throw new JobStateException(String.format("Unable to change the state from %s to %s",
                                                          this.state,
                                                          state));
            }
        }
    }


    public Map<String, String> getArgs() {
        return args;
    }


    public String getTable() {
        return table;
    }


    public void setTable(String table) {
        this.table = table;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public String getInitiator() {
        return initiator;
    }


    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }


    public String getUserId() {
        return userId;
    }


    public void setUserId(String userId) {
        this.userId = userId;
    }


    public Date getDate() {
        return date;
    }


    public void setDate(Date date) {
        this.date = date;
    }


    public String getErrorMessage() {
        return errorMessage;
    }


    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }


    public boolean isFinished() {
        return State.DONE == getState()
               || State.FAILURE == getState()
               || State.REJECTED == getState();
    }


    public enum State {
        NEW,
        REJECTED,
        WAITING,
        RUNNING,
        DONE,
        FAILURE;
    }

    static class StateTransition {
        private final State source;
        private final State target;


        StateTransition(State source, State target) {
            this.source = source;
            this.target = target;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StateTransition that = (StateTransition)o;
            return (source == that.source && target == that.target);
        }


        @Override
        public int hashCode() {
            int result = source != null ? source.hashCode() : 0;
            result = 31 * result + (target != null ? target.hashCode() : 0);
            return result;
        }
    }
}
