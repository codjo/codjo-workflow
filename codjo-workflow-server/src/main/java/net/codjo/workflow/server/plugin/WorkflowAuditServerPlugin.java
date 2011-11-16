package net.codjo.workflow.server.plugin;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.ContainerConfiguration;
import net.codjo.agent.DFService;
import net.codjo.plugin.server.ServerPlugin;
import net.codjo.workflow.common.message.PurgeAuditJobRequest;
import net.codjo.workflow.server.api.JobAgent;
import net.codjo.workflow.server.api.JobAgent.MODE;
import net.codjo.workflow.server.api.ResourcesManagerAgent;
import net.codjo.workflow.server.api.ResourcesManagerAgent.AgentFactory;
import net.codjo.workflow.server.audit.DiscriminentStringifier;
import net.codjo.workflow.server.audit.JdbcAuditDao;
import net.codjo.workflow.server.audit.Stringifier;
import java.util.Map;
/**
 *
 */
public final class WorkflowAuditServerPlugin implements ServerPlugin {
    public static final String PURGE_AUDIT_JOB_TYPE = PurgeAuditJobRequest.PURGE_AUDIT_JOB_TYPE;
    private final WorkflowServerPlugin workflowServerPlugin;


    public WorkflowAuditServerPlugin(WorkflowServerPlugin workflowServerPlugin) {
        this.workflowServerPlugin = workflowServerPlugin;
    }


    public void initContainer(ContainerConfiguration containerConfiguration) throws Exception {
        Map<String, Stringifier> discriminentStringifiers =
              workflowServerPlugin.getConfiguration().getDiscriminentStringifiers();
        workflowServerPlugin.getConfiguration().setAuditDao(
              new JdbcAuditDao(new DiscriminentStringifier(discriminentStringifiers)));
    }


    public void start(AgentContainer agentContainer) throws Exception {
        AgentFactory purgeAuditAgentFactory = new AgentFactory() {
            public JobAgent create() throws Exception {
                return createAuditAgent(MODE.DELEGATE);
            }
        };

        ResourcesManagerAgent purgeDRH = new ResourcesManagerAgent(
              purgeAuditAgentFactory,
              DFService.createAgentDescription(WorkflowAuditServerPlugin.PURGE_AUDIT_JOB_TYPE));
        agentContainer.acceptNewAgent("purge-audit-drh-agent", purgeDRH).start();

        agentContainer
              .acceptNewAgent(PurgeAuditAgent.AGENT_NAME, createAuditAgent(MODE.NOT_DELEGATE))
              .start();
    }


    private PurgeAuditAgent createAuditAgent(MODE mode) {
        return new PurgeAuditAgent(workflowServerPlugin.getConfiguration().getAuditDao(), mode);
    }


    public void stop() throws Exception {
    }
}
