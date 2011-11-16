package net.codjo.workflow.server.plugin;
import net.codjo.agent.Aid;
import net.codjo.agent.test.AgentAssert;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.DateUtil;
import net.codjo.workflow.common.message.JobAudit.Status;
import net.codjo.workflow.common.message.JobAudit.Type;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.PurgeAuditJobRequest;
import net.codjo.workflow.server.api.WorkflowTestCase;
import net.codjo.workflow.server.audit.AuditDaoMock;

public class PurgeAuditAgentTest extends WorkflowTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        startPurgeAuditAgent();
    }


    public void test_nominal() throws Exception {
        String purgePeriod = "3";
        story.record()
              .startTester("initiator")
              .sendMessage(createJobRequestMessage(
                    createPurgeJobRequest(purgePeriod), new Aid(PurgeAuditAgent.AGENT_NAME)))
              .then()
              .receiveMessage(containsAudit(Type.PRE, Status.OK))
              .then()
              .receiveMessage(containsAudit(Type.POST, Status.OK));

        story.record()
              .addAssert(AgentAssert.log(log,
                                         "deleteAudit(" + DateUtil.computeSqlDateFromPeriod(purgePeriod)
                                         + ")"));

        story.execute();
    }


    public void test_periodEqualsZero() throws Exception {
        String purgePeriod = "0";
        story.record()
              .startTester("initiator")
              .sendMessage(createJobRequestMessage(
                    createPurgeJobRequest(purgePeriod), new Aid(PurgeAuditAgent.AGENT_NAME)))
              .then()
              .receiveMessage(containsAudit(Type.PRE, Status.OK))
              .then()
              .receiveMessage(containsAudit(Type.POST, Status.OK));

        story.record()
              .addAssert(AgentAssert.log(log,
                                         "deleteAudit(" + DateUtil.computeSqlDateFromPeriod(purgePeriod)
                                         + ")"));

        story.execute();
    }


    public void test_noPeriod() throws Exception {
        story.record()
              .startTester("initiator")
              .sendMessage(createJobRequestMessage(
                    new JobRequest(PurgeAuditJobRequest.PURGE_AUDIT_JOB_TYPE),
                    new Aid(PurgeAuditAgent.AGENT_NAME)))
              .then()
              .receiveMessage(containsAudit(Type.PRE, Status.ERROR, "La période n'est pas renseignée"));

        story.record().addAssert(AgentAssert.log(log, ""));

        story.execute();
    }


    public void test_invalidPeriod_notANumber() throws Exception {
        String invalidPurgePeriod = "zzzz";
        story.record()
              .startTester("initiator")
              .sendMessage(createJobRequestMessage(
                    createPurgeJobRequest(invalidPurgePeriod), new Aid(PurgeAuditAgent.AGENT_NAME)))
              .then()
              .receiveMessage(containsAudit(Type.PRE, Status.ERROR,
                                            "La période est invalide : " + invalidPurgePeriod));

        story.record().addAssert(AgentAssert.log(log, ""));

        story.execute();
    }


    public void test_invalidPeriod_negative() throws Exception {
        String invalidPurgePeriod = "-3";
        story.record()
              .startTester("initiator")
              .sendMessage(createJobRequestMessage(
                    createPurgeJobRequest(invalidPurgePeriod), new Aid(PurgeAuditAgent.AGENT_NAME)))
              .then()
              .receiveMessage(containsAudit(Type.PRE, Status.ERROR,
                                            "La période est invalide : " + invalidPurgePeriod));

        story.record().addAssert(AgentAssert.log(log, ""));

        story.execute();
    }


    private void startPurgeAuditAgent() {
        story.record().startAgent(PurgeAuditAgent.AGENT_NAME, createPurgeAuditAgent(log));
        story.record().assertNumberOfAgentWithService(1, PurgeAuditJobRequest.PURGE_AUDIT_JOB_TYPE);
    }


    private PurgeAuditAgent createPurgeAuditAgent(LogString logString) {
        return new PurgeAuditAgent(new AuditDaoMock(logString));
    }


    private JobRequest createPurgeJobRequest(String period) {
        return new JobRequest(PurgeAuditJobRequest.PURGE_AUDIT_JOB_TYPE, new Arguments("period", period));
    }
}
