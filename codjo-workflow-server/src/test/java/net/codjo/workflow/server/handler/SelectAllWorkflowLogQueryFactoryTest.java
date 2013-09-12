package net.codjo.workflow.server.handler;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import net.codjo.database.api.query.PreparedQuery;
import net.codjo.mad.server.handler.sql.HandlerSqlMock;
import net.codjo.mad.server.handler.sql.SqlHandler;
import net.codjo.test.common.LogString;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
/**
 *
 */
public class SelectAllWorkflowLogQueryFactoryTest {
    private SelectAllWorkflowLogQueryFactory queryFactory = new SelectAllWorkflowLogQueryFactory();
    private SqlHandler handlerSqlMock = new HandlerSqlMock("workflow");


    @Test
    public void test_nominal() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        String query = queryFactory.buildQuery(args, handlerSqlMock);

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

        assertFillQuery(args, "");
    }


    @Test
    public void test_args_null() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("initiatorLogin", "null");
        args.put("requestType", "null");
        String query = queryFactory.buildQuery(args, handlerSqlMock);

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

        assertFillQuery(args, "");
    }


    @Test
    public void test_args_nullAndNotNull() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("initiatorLogin", "workflow");
        args.put("requestDate", "null");
        args.put("requestType", "null");
        args.put("preAuditStatus", "OK");
        args.put("postAuditStatus", "OK");
        String query = queryFactory.buildQuery(args, handlerSqlMock);

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
                     + "where INITIATOR_LOGIN = ? "
                     + "and PRE_AUDIT_STATUS = ? "
                     + "and POST_AUDIT_STATUS = ? "
                     + "order by REQUEST_DATE DESC", query);

        assertFillQuery(args, "setString(1, workflow), setString(2, OK), setString(3, OK)");
    }


    @Test
    public void test_requestDate() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("requestDate", "2008-03-01");
        String query = queryFactory.buildQuery(args, handlerSqlMock);

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
                     + "where REQUEST_DATE >= ? and REQUEST_DATE < ? "
                     + "order by REQUEST_DATE DESC", query);

        assertFillQuery(args, "setDate(1, 2008-03-01), setDate(2, 2008-03-02)");
    }


    @Test
    public void test_args() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("initiatorLogin", "user2");
        args.put("requestDate", "2011-12-12");
        args.put("requestType", "rtype2");
        args.put("preAuditStatus", "OK");
        args.put("postAuditStatus", "ERROR");

        String query = queryFactory.buildQuery(args, handlerSqlMock);
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
                     + "where REQUEST_TYPE = ? "
                     + "and REQUEST_DATE >= ? and REQUEST_DATE < ? "
                     + "and INITIATOR_LOGIN = ? "
                     + "and PRE_AUDIT_STATUS = ? "
                     + "and POST_AUDIT_STATUS = ? "
                     + "order by REQUEST_DATE DESC", query);

        assertFillQuery(args, "setString(1, rtype2), "
                              + "setDate(2, 2011-12-12), "
                              + "setDate(3, 2011-12-13), "
                              + "setString(4, user2), "
                              + "setString(5, OK), "
                              + "setString(6, ERROR)");
    }


    private void assertFillQuery(Map<String, String> args, String expectedContent) throws SQLException {
        LogString logString = new LogString();
        queryFactory.fillQuery(new MockPreparedQuery(logString), args);
        logString.assertContent(expectedContent);
    }


    private static class MockPreparedQuery implements PreparedQuery {
        private LogString logString;


        MockPreparedQuery(LogString logString) {
            this.logString = logString;
        }


        public void setBigDecimal(int parameterIndex, BigDecimal value) throws SQLException {
            logString.call("setBigDecimal", parameterIndex, value);
        }


        public void setBoolean(int parameterIndex, boolean value) throws SQLException {
            logString.call("setBoolean", parameterIndex, value);
        }


        public void setDate(int parameterIndex, Date value) throws SQLException {
            logString.call("setDate", parameterIndex, value);
        }


        public void setDouble(int parameterIndex, double value) throws SQLException {
            logString.call("setDouble", parameterIndex, value);
        }


        public void setInt(int parameterIndex, int value) throws SQLException {
            logString.call("setInt", parameterIndex, value);
        }


        public void setObject(int parameterIndex, Object value) throws SQLException {
            logString.call("setObject", parameterIndex, value);
        }


        public void setObject(int parameterIndex, Object value, int sqlType) throws SQLException {
            logString.call("setObject", parameterIndex, value, sqlType);
        }


        public void setString(int parameterIndex, String value) throws SQLException {
            logString.call("setString", parameterIndex, value);
        }


        public void setTimestamp(int parameterIndex, Timestamp value) throws SQLException {
            logString.call("setTimestamp", parameterIndex, value);
        }
    }
}
