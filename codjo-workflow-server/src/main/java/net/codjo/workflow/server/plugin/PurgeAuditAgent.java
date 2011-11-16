package net.codjo.workflow.server.plugin;
import net.codjo.agent.DFService.AgentDescription;
import net.codjo.agent.DFService.ServiceDescription;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobAudit.Type;
import net.codjo.workflow.common.message.JobException;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.PurgeAuditJobRequest;
import net.codjo.workflow.common.protocol.JobProtocolParticipant;
import net.codjo.workflow.server.api.JobAgent;
import net.codjo.workflow.server.audit.AuditDao;
import java.sql.SQLException;

public class PurgeAuditAgent extends JobAgent {
    public static final String AGENT_NAME = "purge-audit-agent";
    public static final AgentDescription PURGE_AUDIT_AGENT_DESCRIPTION =
          new AgentDescription(new ServiceDescription(WorkflowAuditServerPlugin.PURGE_AUDIT_JOB_TYPE,
                                                      "worflow-"
                                                      + WorkflowAuditServerPlugin.PURGE_AUDIT_JOB_TYPE
                                                      + "-service"));


    public PurgeAuditAgent(AuditDao auditDao) {
        this(auditDao, MODE.NOT_DELEGATE);
    }


    public PurgeAuditAgent(AuditDao auditDao, MODE mode) {
        super(new PurgeAuditParticipant(auditDao),
              PURGE_AUDIT_AGENT_DESCRIPTION, mode);
    }


    private static class PurgeAuditParticipant extends JobProtocolParticipant {
        private final AuditDao auditDao;


        private PurgeAuditParticipant(AuditDao auditDao) {
            this.auditDao = auditDao;
        }


        @Override
        protected void handlePRE(JobRequest request) {
            JobAudit jobAudit = new JobAudit(Type.PRE);
            PurgeAuditJobRequest purgeAuditJobRequest = new PurgeAuditJobRequest(request);
            String period = purgeAuditJobRequest.getPeriod();
            if (period == null || "".equals(period)) {
                jobAudit.setError(new JobAudit.Anomaly("La période n'est pas renseignée"));
            }
            else if (!isValidPeriod(period)) {
                jobAudit.setError(new JobAudit.Anomaly("La période est invalide : " + period));
            }
            sendAudit(jobAudit);
        }


        @Override
        protected void executeJob(JobRequest request) throws JobException {
            try {
                executeImpl(request);
            }
            catch (SQLException e) {
                throw new JobException("Purge en erreur fatale", e);
            }
        }


        private boolean isValidPeriod(String period) {
            try {
                return Integer.parseInt(period) >= 0;
            }
            catch (NumberFormatException e) {
                return false;
            }
        }


        private void executeImpl(JobRequest request) throws SQLException {
            PurgeAuditJobRequest purgeAuditJobRequest = new PurgeAuditJobRequest(request);
            auditDao.deleteAudit(getAgent(), getRequestMessage(), purgeAuditJobRequest.getBeforeDate());
        }
    }
}
