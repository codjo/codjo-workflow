package net.codjo.workflow.server.handler;
import net.codjo.agent.Agent;
import net.codjo.mad.server.plugin.HandlerExecutor.HandlerExecutorCommand;
import net.codjo.test.common.LogString;
/**
 *
 */
public class HandlerExecutorCommandMock implements HandlerExecutorCommand {
    private final LogString log;


    public HandlerExecutorCommandMock() {
        this(new LogString());
    }


    public HandlerExecutorCommandMock(LogString log) {
        this.log = log;
    }


    public void setResultSenderAgent(Agent agent) {
        log.call("setResultSenderAgent", agent);
    }


    public void execute() throws Exception {
        log.call("execute");
    }
}
