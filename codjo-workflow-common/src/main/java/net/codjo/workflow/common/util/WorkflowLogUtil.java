package net.codjo.workflow.common.util;
import net.codjo.workflow.common.message.JobRequest;
import org.apache.log4j.Logger;

public class WorkflowLogUtil {
    private WorkflowLogUtil() {
    }


    public static void logIfNeeded(Logger logger, JobRequest jobRequest, String message) {
        if (jobRequest.isLoggable()) {
            logger.info(message);
        }
        else {
            logger.debug(message);
        }
    }
}
