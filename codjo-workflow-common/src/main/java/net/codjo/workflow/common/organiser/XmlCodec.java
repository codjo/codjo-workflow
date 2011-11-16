package net.codjo.workflow.common.organiser;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.organiser.Job.State;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.util.Arrays;
import java.util.List;
/**
 *
 */
public class XmlCodec {
    private final XStream xStream;


    public XmlCodec() {
        xStream = createXStream();
    }


    public String jobsToXml(List<? extends Job> jobs) {
        return xStream.toXML(jobs);
    }


    public String jobsToXml(Job... jobs) {
        return jobsToXml(Arrays.asList(jobs));
    }


    public <T extends Job> List<T> xmlToJobs(String xml) {
        //noinspection unchecked
        return (List<T>)xStream.fromXML(xml);
    }


    public String jobEventToXml(JobEvent jobEvent) {
        return xStream.toXML(jobEvent);
    }


    public JobEvent xmlToJobEvent(String xml) {
        return (JobEvent)xStream.fromXML(xml);
    }


    private static XStream createXStream() {
        XStream xStream = new XStream(new DomDriver());
        xStream.alias("jobs", List.class);
        xStream.useAttributeFor("type", String.class);
        xStream.useAttributeFor("initiator", String.class);
        xStream.useAttributeFor("table", String.class);
        xStream.useAttributeFor("state", State.class);
        xStream.registerConverter(new StateConverter());
        return xStream;
    }


    private static class StateConverter implements SingleValueConverter {

        public String toString(Object obj) {
            return obj.toString();
        }


        public Object fromString(String str) {
            for (State state : State.values()) {
                if (state.toString().equals(str)) {
                    return state;
                }
            }
            return null;
        }


        public boolean canConvert(Class type) {
            return State.class.equals(type);
        }
    }
}
