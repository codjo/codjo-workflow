package net.codjo.workflow.server.handler;
import net.codjo.agent.UserId;
import net.codjo.mad.server.plugin.HandlerExecutor.HandlerExecutorCommand;
import java.util.HashMap;
import java.util.Map;
/**
 *
 */
public class DefaultHandlerContextManager implements HandlerContextManager {
    private final Map<UserId, HandlerContext> contexts = new HashMap<UserId, HandlerContext>();


    public synchronized HandlerExecutorCommand getHandlerExecutorCommand(UserId initiator, String requestId) {
        return contexts.get(initiator).getReplyCommand(requestId);
    }


    public synchronized void setHandlerExecutorCommand(UserId initiator,
                                                       String requestId,
                                                       HandlerExecutorCommand handlerExecutorCommand) {
        DefaultHandlerContextManager.HandlerContext context = contexts.get(initiator);
        if (context == null) {
            context = new HandlerContext();
            contexts.put(initiator, context);
        }
        context.setReplyCommand(requestId, handlerExecutorCommand);
    }


    public synchronized void cleanUserContext(UserId initiator) {
        contexts.remove(initiator);
    }


    class HandlerContext {
        private final Map<String, HandlerExecutorCommand> commands
              = new HashMap<String, HandlerExecutorCommand>();


        public HandlerExecutorCommand getReplyCommand(String requestId) {
            return commands.get(requestId);
        }


        public HandlerExecutorCommand setReplyCommand(String requestId,
                                                      HandlerExecutorCommand handlerExecutorCommand) {
            return commands.put(requestId, handlerExecutorCommand);
        }
    }
}
