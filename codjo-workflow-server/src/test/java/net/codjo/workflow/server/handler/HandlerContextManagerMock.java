package net.codjo.workflow.server.handler;
import net.codjo.agent.UserId;
import net.codjo.mad.server.plugin.HandlerExecutor.HandlerExecutorCommand;
import net.codjo.test.common.LogString;
/**
 *
 */
public class HandlerContextManagerMock extends DefaultHandlerContextManager {
    private LogString log;


    public HandlerContextManagerMock(LogString log) {
        this.log = log;
    }


    @Override
    public synchronized HandlerExecutorCommand getHandlerExecutorCommand(UserId initiator, String requestId) {
        log.call("getHandlerExecutorCommand", initiator, requestId);
        return super.getHandlerExecutorCommand(initiator, requestId);
    }


    @Override
    public synchronized void setHandlerExecutorCommand(UserId initiator,
                                                       String requestId,
                                                       HandlerExecutorCommand handlerExecutorCommand) {
        log.call("setHandlerExecutorCommand",
                 initiator,
                 handlerExecutorCommand.getClass().getSimpleName());
        super.setHandlerExecutorCommand(initiator, requestId, handlerExecutorCommand);
    }


    @Override
    public synchronized void cleanUserContext(UserId userId) {
        log.call("cleanUserContext", userId);
        super.cleanUserContext(userId);
    }
}
