package net.codjo.workflow.server.organiser;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobAudit.Type;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.server.audit.AuditDao;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
/**
 *
 */
public class FilteredAuditDao implements AuditDao {
    private final AuditDao auditDao;
    private final List<String> jobFilter;
    private final List<String> filteredRequestIds = new ArrayList<String>();


    public FilteredAuditDao(AuditDao auditDao, List<String> jobFilter) {
        this.auditDao = auditDao;
        this.jobFilter = jobFilter;
    }


    public void saveRequest(Agent agent, AclMessage message, JobRequest request) throws Exception {
        if (jobFilter.contains(request.getType())) {
            filteredRequestIds.add(request.getId());
        }
        else {
            auditDao.saveRequest(agent, message, request);
        }
    }


    public void saveAudit(Agent agent, AclMessage message, JobAudit audit) throws Exception {
        String requestId = audit.getRequestId();
        if (!filteredRequestIds.contains(requestId)) {
            auditDao.saveAudit(agent, message, audit);
        }
        else if (audit.hasError() || audit.getType() == Type.POST) {
            filteredRequestIds.remove(requestId);
        }
    }


    public List<JobRequest> findRequest(Agent agent,
                                        AclMessage message,
                                        String requestType,
                                        Date beginDate,
                                        Date endDate) throws Exception {
        return auditDao.findRequest(agent, message, requestType, beginDate, endDate);
    }


    public void deleteAudit(Agent agent, AclMessage message, Date date) throws SQLException {
        auditDao.deleteAudit(agent, message, date);
    }
}
