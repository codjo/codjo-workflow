package net.codjo.workflow.gui.task;
import net.codjo.agent.AclMessage.Performative;
import static net.codjo.agent.MessageTemplate.and;
import static net.codjo.agent.MessageTemplate.matchPerformative;
import static net.codjo.agent.MessageTemplate.matchProtocol;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.agent.protocol.SubscribeProtocol;
import net.codjo.agent.test.AgentAssert;
import net.codjo.agent.test.Story;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.Service;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.gui.task.TaskManagerAgent.Callback;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
/**
 *
 */
public class TaskManagerAgentTest {
    private Story story = new Story();
    private LogString log = new LogString();
    private CallbackMock callbackMock = new CallbackMock(log);


    @Before
    public void setUp() throws Exception {
        story.doSetUp();
    }


    @After
    public void tearDown() throws Exception {
        story.doTearDown();
    }


    @Test
    public void test_noOrganiserService() throws Exception {
        story.record()
              .startAgent("taskManagerAgent", new TaskManagerAgent(callbackMock));
        story.record()
              .assertNotContainsAgent("taskManagerAgent");

        story.execute();
    }


    @Test
    public void test_nominal() throws Exception {
        story.record()
              .startTester("organiserAgent").registerToDF(Service.ORGANISER_SERVICE).then()
              .receiveMessage(and(matchPerformative(Performative.QUERY),
                                  matchProtocol(RequestProtocol.QUERY)))
              .replyWith(Performative.INFORM,
                         "<jobs>"
                         + "   <net.codjo.workflow.common.organiser.JobMock type=\"import\" state=\"RUNNING\"/>"
                         + "   <net.codjo.workflow.common.organiser.JobMock type=\"broadcast\" state=\"NEW\"/>"
                         + "</jobs>").then()
              .receiveMessage(and(matchPerformative(Performative.SUBSCRIBE),
                                  matchProtocol(SubscribeProtocol.ID)))

              .replyWith(Performative.INFORM,
                         "<jobs>"
                         + "   <net.codjo.workflow.common.organiser.JobMock type=\"broadcast\" state=\"WAITING\"/>"
                         + "   <net.codjo.workflow.common.organiser.JobMock type=\"segmentation\" state=\"NEW\"/>"
                         + "</jobs>")
              .replyWith(Performative.INFORM,
                         "<jobs>"
                         + "   <net.codjo.workflow.common.organiser.JobMock type=\"import\" state=\"DONE\"/>"
                         + "</jobs>");
        story.record()
              .assertNumberOfAgentWithService(1, Service.ORGANISER_SERVICE);

        story.record()
              .startAgent("taskManagerAgent", new TaskManagerAgent(callbackMock));

        story.record()
              .addAssert(AgentAssert.log(log, "jobReceived(Job(import, RUNNING)), "
                                              + "jobReceived(Job(broadcast, NEW)), "
                                              + "jobReceived(Job(broadcast, WAITING)), "
                                              + "jobReceived(Job(segmentation, NEW)), "
                                              + "jobReceived(Job(import, DONE))"));

        story.execute();
    }


    @Test
    public void test_unSubscribe() throws Exception {
        story.record()
              .startTester("organiserAgent").registerToDF(Service.ORGANISER_SERVICE).then()
              .receiveMessage(and(matchPerformative(Performative.QUERY),
                                  matchProtocol(RequestProtocol.QUERY)))
              .replyWith(Performative.INFORM,
                         "<jobs>"
                         + "   <net.codjo.workflow.common.organiser.JobMock type=\"import\" state=\"RUNNING\"/>"
                         + "   <net.codjo.workflow.common.organiser.JobMock type=\"broadcast\" state=\"NEW\"/>"
                         + "</jobs>").then()
              .receiveMessage(and(matchPerformative(Performative.SUBSCRIBE),
                                  matchProtocol(SubscribeProtocol.ID)))

              .then()
              .receiveMessage(and(matchPerformative(Performative.CANCEL),
                                  matchProtocol(SubscribeProtocol.ID)))

              .replyWith(Performative.INFORM,
                         "<jobs>"
                         + "   <net.codjo.workflow.common.organiser.JobMock type=\"broadcast\" state=\"WAITING\"/>"
                         + "   <net.codjo.workflow.common.organiser.JobMock type=\"segmentation\" state=\"NEW\"/>"
                         + "</jobs>")
              .replyWith(Performative.INFORM,
                         "<jobs>"
                         + "   <net.codjo.workflow.common.organiser.JobMock type=\"import\" state=\"DONE\"/>"
                         + "</jobs>");
        story.record()
              .assertNumberOfAgentWithService(1, Service.ORGANISER_SERVICE);

        story.record()
              .startAgent("taskManagerAgent", new TaskManagerAgent(callbackMock));

        story.record().addAssert(AgentAssert.logAndClear(log, "jobReceived(Job(import, RUNNING)), "
                                                              + "jobReceived(Job(broadcast, NEW))"));

        story.record().killAgent("taskManagerAgent");

        story.record()
              .addAssert(AgentAssert.log(log, ""));

        story.execute();
    }


    private static class CallbackMock implements Callback {
        private LogString log;


        private CallbackMock(LogString log) {
            this.log = log;
        }


        public void jobReceived(Job job) {
            log.call("jobReceived", job);
        }
    }
}
