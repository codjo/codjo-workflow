package net.codjo.workflow.server.handler;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.UserId;
import net.codjo.agent.util.IdUtil;
import net.codjo.mad.server.handler.XMLUtils;
import net.codjo.mad.server.handler.aspect.QueryManagerBuilder;
import net.codjo.mad.server.plugin.HandlerExecutor;
import net.codjo.mad.server.plugin.HandlerExecutorFactory;
import net.codjo.workflow.common.message.HandlerJobRequest;
import net.codjo.workflow.common.schedule.ScheduleLauncher;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
/**
 *
 */
public class WorkflowHandlerExecutorFactory implements HandlerExecutorFactory {
    private final HandlerContextManager handlerContextManager;
    private AgentContainer agentContainer;


    public WorkflowHandlerExecutorFactory(HandlerContextManager defaultHandlerContextManager) {
        this.handlerContextManager = defaultHandlerContextManager;
    }


    public void setAgentContainer(AgentContainer agentContainer) {
        this.agentContainer = agentContainer;
    }


    public HandlerExecutor create(UserId userId) throws Exception {
        return new WorkflowHandlerExecutor(userId);
    }


    private class WorkflowHandlerExecutor implements HandlerExecutor {
        private final UserId userId;
        private final QueryManagerBuilder queryManagerBuilder = new QueryManagerBuilder();


        private WorkflowHandlerExecutor(UserId userId) {
            this.userId = userId;
        }


        public void execute(String xmlRequests, HandlerExecutorCommand handlerExecutorCommand)
              throws Exception {
            HandlerJobRequest handlerJobRequest = createJobRequest(xmlRequests);
            handlerContextManager.setHandlerExecutorCommand(userId,
                                                            handlerJobRequest.getId(),
                                                            handlerExecutorCommand);
            ScheduleLauncher launcher = new ScheduleLauncher(userId);
            launcher.executeWorkflow(agentContainer, handlerJobRequest.toRequest());
        }


        private HandlerJobRequest createJobRequest(String xmlRequests)
              throws IOException, SAXException, ParserConfigurationException {
            HandlerJobRequest request = new HandlerJobRequest();
            request.setId(IdUtil.createUniqueId(this));
            request.setHandlerIds(queryManagerBuilder.build(XMLUtils.parse(xmlRequests)).getHandlerIdList());
            request.setXmlContent(xmlRequests);
            return request;
        }
    }
}
