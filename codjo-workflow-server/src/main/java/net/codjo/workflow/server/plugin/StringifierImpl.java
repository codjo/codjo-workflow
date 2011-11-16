package net.codjo.workflow.server.plugin;
import net.codjo.workflow.server.audit.Stringifier;
/**
 *
 */
public abstract class StringifierImpl implements Stringifier {
    private final String requestType;


    protected StringifierImpl(String requestType) {
        this.requestType = requestType;
    }


    public void install(WorkflowServerPlugin workflowServerPlugin) {
        if (workflowServerPlugin != null) {
            workflowServerPlugin.getConfiguration().setDiscriminentStringifier(requestType, this);
        }
    }
}
