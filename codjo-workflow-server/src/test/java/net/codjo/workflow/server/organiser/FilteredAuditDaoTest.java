package net.codjo.workflow.server.organiser;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobAudit.Anomaly;
import net.codjo.workflow.common.message.JobAudit.Type;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.server.audit.AuditDaoMock;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
/**
 *
 */
public class FilteredAuditDaoTest {
    private LogString log = new LogString();
    private List<String> jobFilter = new ArrayList<String>();
    private FilteredAuditDao filteredAuditDao = new FilteredAuditDao(new AuditDaoMock(log), jobFilter);


    @Test
    public void test_nominal() throws Exception {
        filteredAuditDao.saveRequest(null, null, jobRequest("import", "id1"));
        filteredAuditDao.saveAudit(null, null, jobAudit(Type.PRE, "id1", false));
        filteredAuditDao.saveAudit(null, null, jobAudit(Type.POST, "id1", false));
        filteredAuditDao.saveAudit(null, null, jobAudit(Type.POST, "id1", false));

        log.assertContent("saveRequest(import)", "saveAudit(id1)", "saveAudit(id1)", "saveAudit(id1)");
    }


    @Test
    public void test_postAudit() throws Exception {
        jobFilter.add("import");

        filteredAuditDao.saveRequest(null, null, jobRequest("import", "id1"));
        filteredAuditDao.saveAudit(null, null, jobAudit(Type.PRE, "id1", false));
        filteredAuditDao.saveAudit(null, null, jobAudit(Type.POST, "id1", true));
        filteredAuditDao.saveAudit(null, null, jobAudit(Type.POST, "id1", false));

        log.assertContent("saveAudit(id1)");
    }


    @Test
    public void test_preAuditInError() throws Exception {
        jobFilter.add("import");

        filteredAuditDao.saveRequest(null, null, jobRequest("import", "id1"));
        filteredAuditDao.saveAudit(null, null, jobAudit(Type.PRE, "id1", true));
        filteredAuditDao.saveAudit(null, null, jobAudit(Type.POST, "id1", false));

        log.assertContent("saveAudit(id1)");
    }


    @Test
    public void test_postAuditInError() throws Exception {
        jobFilter.add("import");

        filteredAuditDao.saveRequest(null, null, jobRequest("import", "id1"));
        filteredAuditDao.saveAudit(null, null, jobAudit(Type.PRE, "id1", false));
        filteredAuditDao.saveAudit(null, null, jobAudit(Type.POST, "id1", true));
        filteredAuditDao.saveAudit(null, null, jobAudit(Type.POST, "id1", false));

        log.assertContent("saveAudit(id1)");
    }


    private JobRequest jobRequest(String requestType, String requestId) {
        JobRequest jobRequest = new JobRequest(requestType);
        jobRequest.setId(requestId);
        return jobRequest;
    }


    private JobAudit jobAudit(Type type, String requestId, boolean error) {
        JobAudit jobAudit = new JobAudit(type);
        jobAudit.setRequestId(requestId);
        if (error) {
            jobAudit.setError(new Anomaly("Error !!!"));
        }
        return jobAudit;
    }
}
