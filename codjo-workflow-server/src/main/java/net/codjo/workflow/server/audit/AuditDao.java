package net.codjo.workflow.server.audit;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobRequest;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
/**
 *
 */
public interface AuditDao {
    void saveRequest(Agent agent, AclMessage message, JobRequest request)
          throws Exception;


    void saveAudit(Agent agent, AclMessage message, JobAudit audit) throws Exception;


    List<JobRequest> findRequest(Agent agent, AclMessage message,
                                 String requestType, Date beginDate, Date endDate) throws Exception;


    void deleteAudit(Agent agent, AclMessage message, Date date) throws SQLException;


    AuditDao NULL = new AuditDao() {
        public void saveRequest(Agent agent, AclMessage message, JobRequest request) {
        }


        public void saveAudit(Agent agent, AclMessage message, JobAudit audit) {
        }


        public List<JobRequest> findRequest(Agent agent, AclMessage message,
                                            String requestType, Date beginDate, Date endDate) {
            return Collections.emptyList();
        }


        public void deleteAudit(Agent agent, AclMessage message, Date date) throws SQLException {
        }
    };
}
