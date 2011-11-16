package net.codjo.workflow.server.handler;
import net.codjo.agent.UserId;
import net.codjo.mad.server.plugin.HandlerExecutor.HandlerExecutorCommand;
/**
 *
 */
public interface HandlerContextManager {

    HandlerExecutorCommand getHandlerExecutorCommand(UserId initiator, String requestId);


    void setHandlerExecutorCommand(UserId initiator,
                                   String requestId,
                                   HandlerExecutorCommand handlerExecutorCommand);
}
