package net.codjo.workflow.common.batch;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.ContainerFailureException;
import net.codjo.agent.UserId;
import net.codjo.plugin.batch.AbstractBatchPlugin;
import net.codjo.plugin.batch.BatchCore;
import net.codjo.plugin.batch.BatchException;
import net.codjo.plugin.common.CommandLineArguments;
import net.codjo.workflow.common.message.JobRequestWrapper;
import net.codjo.workflow.common.schedule.ScheduleLauncher;
import org.apache.log4j.Logger;

public abstract class AbstractWorkflowBatchPlugin extends AbstractBatchPlugin {
    private static final Logger LOG = Logger.getLogger(AbstractWorkflowBatchPlugin.class);

    private WorkflowBatchPluginConfiguration configuration = new WorkflowBatchPluginConfiguration();
    private AgentContainer container;


    protected abstract JobRequestWrapper createRequest(CommandLineArguments arguments);


    @Override
    public void start(AgentContainer agentContainer) throws Exception {
        container = agentContainer;
    }


    public AgentContainer getContainer() {
        return container;
    }


    public WorkflowBatchPluginConfiguration getConfiguration() {
        return configuration;
    }


    @Override
    public void execute(UserId userId, CommandLineArguments arguments)
          throws ContainerFailureException, BatchException {
        JobRequestWrapper request = createRequest(arguments);

        LOG.info("Arguments : " + request.getArguments().encode());

        ScheduleLauncher launcher = new ScheduleLauncher(userId,
                                                         arguments.getArgument(BatchCore.BATCH_INITIATOR));
        launcher.setWorkflowConfiguration(getConfiguration().getWorkflowConfiguration());
        launcher.executeWorkflow(getContainer(), request.toRequest());
    }
}
