package net.codjo.workflow.server.organiser;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.server.audit.DiscriminentStringifier;
/**
 *
 */
public class DiscriminentStringifierMock extends DiscriminentStringifier {
    private String discriminent;


    public DiscriminentStringifierMock() {
        super(null);
    }


    @Override
    public String getDiscriminent(JobRequest request) {
        return discriminent;
    }


    @SuppressWarnings({"ParameterHidesMemberVariable"})
    public void mock(String discriminent) {
        this.discriminent = discriminent;
    }
}
