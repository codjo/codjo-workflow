package net.codjo.workflow.common.organiser;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
/**
 *
 */
class JobEventMock extends JobEvent {

    JobEventMock(String requestId, String date) throws ParseException {
        super(new JobRequest());
        getRequest().setId(requestId);
        getRequest().setDate(new SimpleDateFormat("yyyy-MM-dd").parse(date));
    }
}
