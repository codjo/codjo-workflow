package net.codjo.workflow.server.audit;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobRequest;
import java.util.Map;
public class DiscriminentStringifier {
    private final Map<String, Stringifier> stringifiers;


    public DiscriminentStringifier(Map<String, Stringifier> stringifiers) {
        this.stringifiers = stringifiers;
    }


    public String getDiscriminent(JobRequest request) {
        String encodedArguments = encodeArguments(request.getArguments());
        String discriminent = null;
        Stringifier stringifier = stringifiers.get(request.getType());
        if (stringifier != null) {
            discriminent = stringifier.toString(request);
        }
        else if (encodedArguments != null) {
            discriminent = encodedArguments.replaceAll("\n", " ");
            discriminent = discriminent.substring(0, Math.min(150, discriminent.length()));
        }
        return discriminent;
    }


    private String encodeArguments(Arguments arguments) {
        if (arguments == null) {
            return null;
        }
        return arguments.encode().trim();
    }
}