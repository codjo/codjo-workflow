package net.codjo.workflow.common.schedule;
/**
 *
 */
public class WorkflowConfiguration {
    private static final int MINUTE = 60 * 1000;
    static final int DEFAULT_TIMEOUT = 30 * MINUTE;
    static final int DEFAULT_REPLY_TIMEOUT = 4 * MINUTE;
    private long defaultTimeout = DEFAULT_TIMEOUT;
    private long defaultReplyTimeout = DEFAULT_REPLY_TIMEOUT;


    public long getDefaultTimeout() {
        return defaultTimeout;
    }


    public void setDefaultTimeout(long defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }


    public long getDefaultReplyTimeout() {
        return defaultReplyTimeout;
    }


    public void setDefaultReplyTimeout(long defaultReplyTimeout) {
        this.defaultReplyTimeout = defaultReplyTimeout;
    }
}
