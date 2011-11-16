package net.codjo.workflow.server.handler;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Agent;
import static net.codjo.agent.test.AgentAssert.log;
import net.codjo.agent.test.OneShotStep;
import net.codjo.mad.server.plugin.HandlerExecutor;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobAudit.Type;
import net.codjo.workflow.common.util.ReplyWithJobAudit;
import net.codjo.workflow.common.util.WorkflowSystem;
import net.codjo.workflow.server.api.WorkflowTestCase;
import java.util.regex.Pattern;
/**
 *
 */
public class WorkflowHandlerExecutorFactoryTest extends WorkflowTestCase {

    public void test_create() throws Exception {
        final HandlerContextManager handlerContextManager = new HandlerContextManagerMock(log);
        final String xmlContent = "<requests>"
                                  + "    <audit>"
                                  + "        <user>user_dev</user>"
                                  + "    </audit>"
                                  + "    <select request_id=\"1\">"
                                  + "        <id>selectQuery</id>"
                                  + "    </select>"
                                  + "    <update request_id=\"2\">"
                                  + "        <id>updateQuery</id>"
                                  + "    </update>"
                                  + "</requests>";

        story.record().mock(WorkflowSystem.workFlowSystem())
              .simulateJob(String.format(
                    "job<handler>(handler-ids=[selectQuery, updateQuery], xml-content=%s)",
                    xmlContent),
                           new ReplyWithJobAudit(Performative.INFORM,
                                                 Type.POST,
                                                 new Arguments("result", "<result/>")));

        story.record().startTester("dummy")
              .perform(new OneShotStep() {
                  public void run(Agent agent) throws Exception {
                      WorkflowHandlerExecutorFactory factory
                            = new WorkflowHandlerExecutorFactory(handlerContextManager);
                      factory.setAgentContainer(story.getContainer());
                      HandlerExecutorCommandMock handlerExecutorCommand = new HandlerExecutorCommandMock(log);
                      HandlerExecutor handlerExecutor = factory.create(userId);
                      handlerExecutor.execute(xmlContent, handlerExecutorCommand);
                  }
              });

        story.record()
              .addAssert(log(log, Pattern.compile(
                    String.format(
                          "setHandlerExecutorCommand\\(%s, %s\\)",
                          userId,
                          "HandlerExecutorCommandMock"))));

        story.execute();
    }
}
