package net.codjo.workflow.server.handler;
import net.codjo.mad.server.handler.sql.HandlerSqlMock;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SelectAllWorkflowLogQueryFactoryTest {
    private SelectAllWorkflowLogQueryFactory queryFactory = new SelectAllWorkflowLogQueryFactory();


    @Test
    public void test_nominal() throws Exception {
        String query = queryFactory.buildQuery(new HashMap<String, String>(), new HandlerSqlMock("workflow"));

        assertEquals("select "
                     + "ID, "
                     + "REQUEST_TYPE, "
                     + "REQUEST_DATE, "
                     + "POST_AUDIT_DATE, "
                     + "INITIATOR_LOGIN, "
                     + "DISCRIMINENT, "
                     + "PRE_AUDIT_STATUS, "
                     + "POST_AUDIT_STATUS "
                     + "from AP_WORKFLOW_LOG "
                     + "order by REQUEST_DATE DESC", query);
    }


    @Test
    public void test_args() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("initiatorLogin", "workflow");
        args.put("requestDate", "2009-01-01");
        args.put("requestType", "test");
        args.put("preAuditStatus", "OK");
        args.put("postAuditStatus", "OK");
        String query = queryFactory.buildQuery(args, new HandlerSqlMock("workflow"));

        assertEquals("select "
                     + "ID, "
                     + "REQUEST_TYPE, "
                     + "REQUEST_DATE, "
                     + "POST_AUDIT_DATE, "
                     + "INITIATOR_LOGIN, "
                     + "DISCRIMINENT, "
                     + "PRE_AUDIT_STATUS, "
                     + "POST_AUDIT_STATUS "
                     + "from AP_WORKFLOW_LOG "
                     + "where REQUEST_TYPE = 'test' "
                     + "and convert(datetime, (convert(char(12), REQUEST_DATE, 112))) = '2009-01-01' "
                     + "and INITIATOR_LOGIN = 'workflow' "
                     + "and PRE_AUDIT_STATUS = 'OK' "
                     + "and POST_AUDIT_STATUS = 'OK' "
                     + "order by REQUEST_DATE DESC", query);
    }


    @Test
    public void test_args_null() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("initiatorLogin", "null");
        args.put("requestType", "null");
        String query = queryFactory.buildQuery(args, new HandlerSqlMock("workflow"));

        assertEquals("select "
                     + "ID, "
                     + "REQUEST_TYPE, "
                     + "REQUEST_DATE, "
                     + "POST_AUDIT_DATE, "
                     + "INITIATOR_LOGIN, "
                     + "DISCRIMINENT, "
                     + "PRE_AUDIT_STATUS, "
                     + "POST_AUDIT_STATUS "
                     + "from AP_WORKFLOW_LOG "
                     + "order by REQUEST_DATE DESC", query);
    }


    @Test
    public void test_args_nullAndNotNull() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("initiatorLogin", "workflow");
        args.put("requestDate", "null");
        args.put("requestType", "null");
        args.put("preAuditStatus", "OK");
        args.put("postAuditStatus", "OK");
        String query = queryFactory.buildQuery(args, new HandlerSqlMock("workflow"));

        assertEquals("select "
                     + "ID, "
                     + "REQUEST_TYPE, "
                     + "REQUEST_DATE, "
                     + "POST_AUDIT_DATE, "
                     + "INITIATOR_LOGIN, "
                     + "DISCRIMINENT, "
                     + "PRE_AUDIT_STATUS, "
                     + "POST_AUDIT_STATUS "
                     + "from AP_WORKFLOW_LOG "
                     + "where INITIATOR_LOGIN = 'workflow' "
                     + "and PRE_AUDIT_STATUS = 'OK' "
                     + "and POST_AUDIT_STATUS = 'OK' "
                     + "order by REQUEST_DATE DESC", query);
    }


    @Test
    public void test_requestDate() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("requestDate", "2008-03-01");
        String query = queryFactory.buildQuery(args, new HandlerSqlMock("workflow"));

        assertEquals("select "
                     + "ID, "
                     + "REQUEST_TYPE, "
                     + "REQUEST_DATE, "
                     + "POST_AUDIT_DATE, "
                     + "INITIATOR_LOGIN, "
                     + "DISCRIMINENT, "
                     + "PRE_AUDIT_STATUS, "
                     + "POST_AUDIT_STATUS "
                     + "from AP_WORKFLOW_LOG "
                     + "where convert(datetime, (convert(char(12), REQUEST_DATE, 112))) = '2008-03-01' "
                     + "order by REQUEST_DATE DESC", query);
    }
}
