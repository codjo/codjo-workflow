package net.codjo.workflow.server.aspect;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.UserId;
import net.codjo.aspect.AspectContext;
import net.codjo.aspect.AspectException;
import net.codjo.mad.server.handler.AspectBranchLauncher;
import net.codjo.mad.server.handler.AspectBranchLauncherFactory;
import net.codjo.mad.server.handler.aspect.AspectBranchId;
import net.codjo.mad.server.handler.aspect.Keys;
import net.codjo.security.common.api.User;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.schedule.ScheduleLauncher;
import net.codjo.workflow.common.schedule.ScheduleLauncher.ExecuteType;
/**
 *
 */
public class WorkflowAspectBranchLauncherFactory implements AspectBranchLauncherFactory {
    private AgentContainer agentContainer;


    public void setAgentContainer(AgentContainer agentContainer) {
        this.agentContainer = agentContainer;
    }


    public AspectBranchLauncher create() {
        return new AspectBranchLauncher() {
            public void run(AspectBranchId branchId, AspectContext context) throws AspectException {
                ScheduleLauncher launcher = new ScheduleLauncher(getUserId(context),
                                                                 (String)context.get(Keys.USER_NAME));
                launcher.setExecuteType(ExecuteType.ASYNCHRONOUS);

                try {
                    launcher.executeWorkflow(agentContainer, createJobRequest(branchId, context));
                }
                catch (Exception e) {
                    throw new AspectException("Erreur lors de l'exécution des aspects en mode fork "
                                              + "sur '" + branchId + "'", e);
                }
            }


            private JobRequest createJobRequest(AspectBranchId branchId, AspectContext context) {
                ExecuteAspectRequest request = new ExecuteAspectRequest();
                request.setAspectBranchId(branchId);
                request.setAspectContext(context);
                return request.toRequest();
            }


            private UserId getUserId(AspectContext context) {
                User user = (User)context.get(Keys.USER);
                return user.getId();
            }
        };
    }
}
