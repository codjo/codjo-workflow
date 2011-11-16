package net.codjo.workflow.server.api;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Aid;
import net.codjo.agent.ContainerFailureException;
import net.codjo.agent.test.Story;
import net.codjo.workflow.common.message.JobRequest;
/**
 *
 */
public abstract class JobAgentTestCase extends WorkflowTestCase {
    private static final String JOB_AGENT_AID = "job-agent";
    private JobAgentStoryRecorder recorder;


    protected abstract JobAgent createJobAgent() throws Exception;


    protected abstract String getServiceType() throws Exception;


    public void test_agentDescription() throws Exception {
        story.record().startAgent(JOB_AGENT_AID, createJobAgent());

        story.record()
              .assertAgentWithService(new String[]{JOB_AGENT_AID}, getServiceType());

        story.execute();
    }


    public JobAgentStoryRecorder record() {
        story.record();
        return recorder;
    }


    protected void executeStory() throws ContainerFailureException {
        story.execute();
    }


    @Override
    protected final void setUp() throws Exception {
        super.setUp();
        recorder = new JobAgentStoryRecorder();
        doSetUp();
    }


    protected void doSetUp() throws Exception {
    }


    @Override
    protected final void tearDown() throws Exception {
        super.tearDown();
        doTearDown();
    }


    protected void doTearDown() throws Exception {
    }


    protected AclMessage createJobRequestMessage(JobRequest jobRequest) {
        Aid receiver = new Aid(JOB_AGENT_AID);
        return createJobRequestMessage(jobRequest, receiver);
    }


    public class JobAgentStoryRecorder extends Story.StoryRecorder {

        protected JobAgentStoryRecorder() {
            super(story);
        }


        public void startJobAgent(JobAgent scheduleAgent) {
            startAgent(JOB_AGENT_AID, scheduleAgent);
        }
    }
}
