/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.server.audit;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobRequest;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
/**
 *
 */
public class AuditDaoMock implements AuditDao {
    private LogString log;


    public AuditDaoMock(LogString log) {
        this.log = log;
    }


    public void saveRequest(Agent agent, AclMessage message, JobRequest request) throws Exception {
        log.call("saveRequest", request.getType());
    }


    public void saveAudit(Agent agent, AclMessage message, JobAudit audit) throws Exception {
        log.call("saveAudit", audit.getRequestId());
    }


    public List<JobRequest> findRequest(Agent agent,
                                        AclMessage message,
                                        String requestType,
                                        Date beginDate,
                                        Date endDate) throws Exception {
        log.call("findRequest", requestType, beginDate, endDate);
        return Collections.emptyList();
    }


    public void deleteAudit(Agent agent, AclMessage message, Date date) throws SQLException {
        log.call("deleteAudit", date);
    }
}
