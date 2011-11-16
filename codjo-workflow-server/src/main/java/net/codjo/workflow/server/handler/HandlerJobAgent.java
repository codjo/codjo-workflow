package net.codjo.workflow.server.handler;
import net.codjo.agent.DFService.AgentDescription;
import net.codjo.agent.DFService.ServiceDescription;
import net.codjo.agent.UserId;
import net.codjo.mad.server.plugin.HandlerExecutor.HandlerExecutorCommand;
import net.codjo.workflow.common.message.HandlerJobRequest;
import net.codjo.workflow.common.message.JobException;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.protocol.JobProtocolParticipant;
import net.codjo.workflow.server.api.JobAgent;
/**
 *
 */
public class HandlerJobAgent extends JobAgent {
    public static final AgentDescription HANDLER_AGENT_DESCRIPTION =
          new AgentDescription(new ServiceDescription(HandlerJobRequest.HANDLER_JOB_TYPE,
                                                      "worflow-"
                                                      + HandlerJobRequest.HANDLER_JOB_TYPE
                                                      + "-service"));


    public HandlerJobAgent(HandlerContextManager handlerContextManager, MODE mode) {
        super(new HandlerProtocolParticipant(handlerContextManager), HANDLER_AGENT_DESCRIPTION, mode);
    }


    private static class HandlerProtocolParticipant extends JobProtocolParticipant {
        private final HandlerContextManager handlerContextManager;


        HandlerProtocolParticipant(HandlerContextManager handlerContextManager) {
            this.handlerContextManager = handlerContextManager;
        }


        @Override
        protected void executeJob(JobRequest request) throws JobException {
            UserId iniator = getRequestMessage().decodeUserId();
            HandlerJobRequest handlerJobRequest = new HandlerJobRequest(request);
            String requestId = handlerJobRequest.getId();
            HandlerExecutorCommand command
                  = handlerContextManager.getHandlerExecutorCommand(iniator, requestId);
            try {
                command.execute();
            }
            catch (Exception e) {
                throw new JobException(String.format("Erreur lors de l'exécution du handler %s.", requestId),
                                       e);
            }
        }
    }
}
