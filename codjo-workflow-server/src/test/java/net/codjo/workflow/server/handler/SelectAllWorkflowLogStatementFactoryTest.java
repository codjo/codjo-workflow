package net.codjo.workflow.server.handler;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import net.codjo.database.api.DatabaseTesterFactory;
import net.codjo.database.common.api.JdbcFixture;
import net.codjo.database.common.api.structure.SqlTable;
import net.codjo.mad.server.MadConnectionManagerMock;
import net.codjo.mad.server.MadRequestContextMock;
import net.codjo.mad.server.MadTransactionMock;
import net.codjo.mad.server.handler.HandlerContext;
import net.codjo.mad.server.handler.SecurityContextMock;
import net.codjo.mad.server.handler.sql.SqlHandler;
import net.codjo.mad.server.util.ConnectionNoClose;
import net.codjo.security.server.api.UserFactoryMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
/**
 *
 */
public class SelectAllWorkflowLogStatementFactoryTest {
    private JdbcFixture jdbc = JdbcFixture.newFixture();
    private SqlHandler sqlHandler;

    private SelectAllWorkflowLogStatementFactory statementFactory = new SelectAllWorkflowLogStatementFactory();


    @Before
    public void setup() throws Exception {
        jdbc.doSetUp();
        jdbc.advanced().dropAllObjects();

        jdbc.create(SqlTable.table("AP_WORKFLOW_LOG"),
                    " ID varchar(50) not null, "
                    + " REQUEST_TYPE varchar(64) not null, "
                    + " REQUEST_DATE date not null, "
                    + " POST_AUDIT_DATE date, "
                    + " INITIATOR_LOGIN varchar(30), "
                    + " DISCRIMINENT varchar(150), "
                    + " PRE_AUDIT_STATUS varchar(10), "
                    + " POST_AUDIT_STATUS varchar (10) ");
        jdbc.executeUpdate(
              "insert into AP_WORKFLOW_LOG values ('1001', 'rtype1', '2011-12-11 09:00:00.000', '2012-01-01', 'user1', 'discriminent1', 'OK', 'ERROR')");
        jdbc.executeUpdate(
              "insert into AP_WORKFLOW_LOG values ('1002', 'rtype2', '2011-12-12 10:03:40.000', '2012-01-02', 'user2', 'discriminent2', 'OK', 'ERROR')");
        jdbc.executeUpdate(
              "insert into AP_WORKFLOW_LOG values ('1003', 'rtype3', '2011-12-13 11:00:00.000', '2012-01-03', 'user3', 'discriminent3', 'OK', 'ERROR')");
        jdbc.executeUpdate(
              "insert into AP_WORKFLOW_LOG values ('1004', 'rtype4', '2011-12-14 09:38:00.000', '2012-01-04', 'user4', 'discriminent4', 'OK', 'ERROR')");
        jdbc.executeUpdate(
              "insert into AP_WORKFLOW_LOG values ('1005', 'rtype5', '2011-12-15 09:50:15.000', '2012-01-05', 'user5', 'discriminent5', 'WARNING', 'ERROR')");
        jdbc.executeUpdate(
              "insert into AP_WORKFLOW_LOG values ('1006', 'rtype6', '2011-12-16 07:00:01.001', '2012-01-06', 'user6', 'discriminent6', 'OK', 'OK')");

        MadRequestContextMock madRequestContext =
              new MadRequestContextMock(new MadTransactionMock(),
                                        SecurityContextMock.userIsInAllRole(),
                                        new UserFactoryMock());

        ((MadConnectionManagerMock)madRequestContext.getConnectionManager())
              .mockGetConnection(new ConnectionNoClose(jdbc.getConnection()));

        HandlerContext handlerContext = new HandlerContext(madRequestContext);
        handlerContext.setUser("user1");

        sqlHandler = new MyHandlerSql();
        sqlHandler.setContext(handlerContext);
    }


    @After
    public void tearDown() throws Exception {
        jdbc.doTearDown();
    }


    @Test
    public void test_nominal() throws Exception {
        PreparedStatement statement = statementFactory.buildStatement(new HashMap<String, String>(), sqlHandler);
        // select ID, REQUEST_TYPE, REQUEST_DATE, POST_AUDIT_DATE, INITIATOR_LOGIN, DISCRIMINENT, PRE_AUDIT_STATUS, POST_AUDIT_STATUS
        // from AP_WORKFLOW_LOG
        // order by REQUEST_DATE DESC
        ResultSet result = statement.executeQuery();

        int nbRows = 0;
        while (result.next()) {
            nbRows++;
        }

        assertEquals(6, nbRows);

        assertEquals(0, statement.getParameterMetaData().getParameterCount());
    }


    @Test
    public void test_args() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("initiatorLogin", "user2");
        args.put("requestDate", "2011-12-12"); // Warning : Date = 2 parameters in SelectAllWorkflowLogStatementFactory
        args.put("requestType", "rtype2");
        args.put("preAuditStatus", "OK");
        args.put("postAuditStatus", "ERROR");

        PreparedStatement statement = statementFactory.buildStatement(args, sqlHandler);
        // select ID, REQUEST_TYPE, REQUEST_DATE, POST_AUDIT_DATE, INITIATOR_LOGIN, DISCRIMINENT, PRE_AUDIT_STATUS, POST_AUDIT_STATUS
        // from AP_WORKFLOW_LOG
        // where INITIATOR_LOGIN = ? and REQUEST_DATE >= ?  and REQUEST_DATE < ? and REQUEST_TYPE = ? and PRE_AUDIT_STATUS = ? and POST_AUDIT_STATUS = ?
        // order by REQUEST_DATE DESC
        ResultSet result = statement.executeQuery();
        int nbRows = 0;
        while (result.next()) {
            nbRows++;
            assertEquals("1002", result.getString("ID"));
            assertEquals("user2", result.getString("INITIATOR_LOGIN"));
            assertEquals("rtype2", result.getString("REQUEST_TYPE"));
            assertEquals(java.sql.Date.valueOf("2011-12-12"), result.getDate("REQUEST_DATE"));
            assertEquals(java.sql.Date.valueOf("2012-01-02"), result.getDate("POST_AUDIT_DATE"));
            assertEquals("discriminent2", result.getString("DISCRIMINENT"));
            assertEquals("OK", result.getString("PRE_AUDIT_STATUS"));
            assertEquals("ERROR", result.getString("POST_AUDIT_STATUS"));
        }

        assertEquals(1, nbRows);

        assertEquals(6, statement.getParameterMetaData().getParameterCount());
    }


    @Test
    public void test_args_null() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("initiatorLogin", "null");
        args.put("requestType", "null");

        PreparedStatement statement = statementFactory.buildStatement(args, sqlHandler);
        // select ID, REQUEST_TYPE, REQUEST_DATE, POST_AUDIT_DATE, INITIATOR_LOGIN, DISCRIMINENT, PRE_AUDIT_STATUS, POST_AUDIT_STATUS
        // from AP_WORKFLOW_LOG
        // order by REQUEST_DATE DESC
        ResultSet result = statement.executeQuery();

        int nbRows = 0;
        while (result.next()) {
            nbRows++;
        }

        assertEquals(6, nbRows);

        assertEquals(0, statement.getParameterMetaData().getParameterCount());
    }


    @Test
    public void test_args_nullAndNotNull() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("initiatorLogin", "user3");
        args.put("requestDate", "null");
        args.put("requestType", "null");
        args.put("preAuditStatus", "OK");
        args.put("postAuditStatus", "ERROR");

        PreparedStatement statement = statementFactory.buildStatement(args, sqlHandler);
        // select ID, REQUEST_TYPE, REQUEST_DATE, POST_AUDIT_DATE, INITIATOR_LOGIN, DISCRIMINENT, PRE_AUDIT_STATUS, POST_AUDIT_STATUS
        // from AP_WORKFLOW_LOG
        // where INITIATOR_LOGIN = ? and PRE_AUDIT_STATUS = ? and POST_AUDIT_STATUS = ?
        // order by REQUEST_DATE DESC
        ResultSet result = statement.executeQuery();
        int nbRows = 0;
        while (result.next()) {
            nbRows++;
            assertEquals("1003", result.getString("ID"));
            assertEquals("user3", result.getString("INITIATOR_LOGIN"));
            assertEquals("rtype3", result.getString("REQUEST_TYPE"));
            assertEquals(java.sql.Date.valueOf("2011-12-13"), result.getDate("REQUEST_DATE"));
            assertEquals(java.sql.Date.valueOf("2012-01-03"), result.getDate("POST_AUDIT_DATE"));
            assertEquals("discriminent3", result.getString("DISCRIMINENT"));
            assertEquals("OK", result.getString("PRE_AUDIT_STATUS"));
            assertEquals("ERROR", result.getString("POST_AUDIT_STATUS"));
        }

        assertEquals(1, nbRows);

        assertEquals(3, statement.getParameterMetaData().getParameterCount());
    }


    @Test
    public void test_requestType() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("requestType", "rtype1"); // Warning : Date = 2 parameters in SelectAllWorkflowLogStatementFactory

        PreparedStatement statement = statementFactory.buildStatement(args, sqlHandler);
        // select ID, REQUEST_TYPE, REQUEST_DATE, POST_AUDIT_DATE, INITIATOR_LOGIN, DISCRIMINENT, PRE_AUDIT_STATUS, POST_AUDIT_STATUS
        // from AP_WORKFLOW_LOG
        // where REQUEST_TYPE = ?
        // order by REQUEST_DATE DESC
        ResultSet result = statement.executeQuery();
        int nbRows = 0;
        while (result.next()) {
            nbRows++;
            assertEquals("1001", result.getString("ID"));
            assertEquals("user1", result.getString("INITIATOR_LOGIN"));
            assertEquals("rtype1", result.getString("REQUEST_TYPE"));
            assertEquals(java.sql.Date.valueOf("2011-12-11"), result.getDate("REQUEST_DATE"));
            assertEquals(java.sql.Date.valueOf("2012-01-01"), result.getDate("POST_AUDIT_DATE"));
            assertEquals("discriminent1", result.getString("DISCRIMINENT"));
            assertEquals("OK", result.getString("PRE_AUDIT_STATUS"));
            assertEquals("ERROR", result.getString("POST_AUDIT_STATUS"));
        }

        assertEquals(1, nbRows);

        assertEquals(1, statement.getParameterMetaData().getParameterCount());
    }


    @Test
    public void test_requestDate() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("requestDate", "2011-12-12"); // Warning : Date = 2 parameters in SelectAllWorkflowLogStatementFactory

        PreparedStatement statement = statementFactory.buildStatement(args, sqlHandler);
        // select ID, REQUEST_TYPE, REQUEST_DATE, POST_AUDIT_DATE, INITIATOR_LOGIN, DISCRIMINENT, PRE_AUDIT_STATUS, POST_AUDIT_STATUS
        // from AP_WORKFLOW_LOG
        // where REQUEST_DATE >= ?  and REQUEST_DATE < ?
        // order by REQUEST_DATE DESC
        ResultSet result = statement.executeQuery();
        int nbRows = 0;
        while (result.next()) {
            nbRows++;
            assertEquals("1002", result.getString("ID"));
            assertEquals("user2", result.getString("INITIATOR_LOGIN"));
            assertEquals("rtype2", result.getString("REQUEST_TYPE"));
            assertEquals(java.sql.Date.valueOf("2011-12-12"), result.getDate("REQUEST_DATE"));
            assertEquals(java.sql.Date.valueOf("2012-01-02"), result.getDate("POST_AUDIT_DATE"));
            assertEquals("discriminent2", result.getString("DISCRIMINENT"));
            assertEquals("OK", result.getString("PRE_AUDIT_STATUS"));
            assertEquals("ERROR", result.getString("POST_AUDIT_STATUS"));
        }

        assertEquals(1, nbRows);

        assertEquals(2, statement.getParameterMetaData().getParameterCount());
    }


    @Test
    public void test_initiatorLogin() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("initiatorLogin", "user3"); // Warning : Date = 2 parameters in SelectAllWorkflowLogStatementFactory

        PreparedStatement statement = statementFactory.buildStatement(args, sqlHandler);
        // select ID, REQUEST_TYPE, REQUEST_DATE, POST_AUDIT_DATE, INITIATOR_LOGIN, DISCRIMINENT, PRE_AUDIT_STATUS, POST_AUDIT_STATUS
        // from AP_WORKFLOW_LOG
        // where INITIATOR_LOGIN = ?
        // order by REQUEST_DATE DESC
        ResultSet result = statement.executeQuery();
        int nbRows = 0;
        while (result.next()) {
            nbRows++;
            assertEquals("1003", result.getString("ID"));
            assertEquals("user3", result.getString("INITIATOR_LOGIN"));
            assertEquals("rtype3", result.getString("REQUEST_TYPE"));
            assertEquals(java.sql.Date.valueOf("2011-12-13"), result.getDate("REQUEST_DATE"));
            assertEquals(java.sql.Date.valueOf("2012-01-03"), result.getDate("POST_AUDIT_DATE"));
            assertEquals("discriminent3", result.getString("DISCRIMINENT"));
            assertEquals("OK", result.getString("PRE_AUDIT_STATUS"));
            assertEquals("ERROR", result.getString("POST_AUDIT_STATUS"));
        }

        assertEquals(1, nbRows);

        assertEquals(1, statement.getParameterMetaData().getParameterCount());
    }


    @Test
    public void test_discriminent() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("discriminent", "discriminent4"); // Warning : Date = 2 parameters in SelectAllWorkflowLogStatementFactory

        PreparedStatement statement = statementFactory.buildStatement(args, sqlHandler);
        // select ID, REQUEST_TYPE, REQUEST_DATE, POST_AUDIT_DATE, INITIATOR_LOGIN, DISCRIMINENT, PRE_AUDIT_STATUS, POST_AUDIT_STATUS
        // from AP_WORKFLOW_LOG
        // where DISCRIMINENT = ?
        // order by REQUEST_DATE DESC
        ResultSet result = statement.executeQuery();
        int nbRows = 0;
        while (result.next()) {
            nbRows++;
            assertEquals("1004", result.getString("ID"));
            assertEquals("user4", result.getString("INITIATOR_LOGIN"));
            assertEquals("rtype4", result.getString("REQUEST_TYPE"));
            assertEquals(java.sql.Date.valueOf("2011-12-14"), result.getDate("REQUEST_DATE"));
            assertEquals(java.sql.Date.valueOf("2012-01-04"), result.getDate("POST_AUDIT_DATE"));
            assertEquals("discriminent4", result.getString("DISCRIMINENT"));
            assertEquals("OK", result.getString("PRE_AUDIT_STATUS"));
            assertEquals("ERROR", result.getString("POST_AUDIT_STATUS"));
        }

        assertEquals(1, nbRows);

        assertEquals(1, statement.getParameterMetaData().getParameterCount());
    }


    @Test
    public void test_preAuditStatus() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("preAuditStatus", "WARNING"); // Warning : Date = 2 parameters in SelectAllWorkflowLogStatementFactory

        PreparedStatement statement = statementFactory.buildStatement(args, sqlHandler);
        // select ID, REQUEST_TYPE, REQUEST_DATE, POST_AUDIT_DATE, INITIATOR_LOGIN, DISCRIMINENT, PRE_AUDIT_STATUS, POST_AUDIT_STATUS
        // from AP_WORKFLOW_LOG
        // where PRE_AUDIT_STATUS = ?
        // order by REQUEST_DATE DESC
        ResultSet result = statement.executeQuery();
        int nbRows = 0;
        while (result.next()) {
            nbRows++;
            assertEquals("1005", result.getString("ID"));
            assertEquals("user5", result.getString("INITIATOR_LOGIN"));
            assertEquals("rtype5", result.getString("REQUEST_TYPE"));
            assertEquals(java.sql.Date.valueOf("2011-12-15"), result.getDate("REQUEST_DATE"));
            assertEquals(java.sql.Date.valueOf("2012-01-05"), result.getDate("POST_AUDIT_DATE"));
            assertEquals("discriminent5", result.getString("DISCRIMINENT"));
            assertEquals("WARNING", result.getString("PRE_AUDIT_STATUS"));
            assertEquals("ERROR", result.getString("POST_AUDIT_STATUS"));
        }

        assertEquals(1, nbRows);

        assertEquals(1, statement.getParameterMetaData().getParameterCount());
    }


    @Test
    public void test_postAuditStatus() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("postAuditStatus", "OK"); // Warning : Date = 2 parameters in SelectAllWorkflowLogStatementFactory

        PreparedStatement statement = statementFactory.buildStatement(args, sqlHandler);
        // select ID, REQUEST_TYPE, REQUEST_DATE, POST_AUDIT_DATE, INITIATOR_LOGIN, DISCRIMINENT, PRE_AUDIT_STATUS, POST_AUDIT_STATUS
        // from AP_WORKFLOW_LOG
        // where POST_AUDIT_STATUS = ?
        // order by REQUEST_DATE DESC
        ResultSet result = statement.executeQuery();
        int nbRows = 0;
        while (result.next()) {
            nbRows++;
            assertEquals("1006", result.getString("ID"));
            assertEquals("user6", result.getString("INITIATOR_LOGIN"));
            assertEquals("rtype6", result.getString("REQUEST_TYPE"));
            assertEquals(java.sql.Date.valueOf("2011-12-16"), result.getDate("REQUEST_DATE"));
            assertEquals(java.sql.Date.valueOf("2012-01-06"), result.getDate("POST_AUDIT_DATE"));
            assertEquals("discriminent6", result.getString("DISCRIMINENT"));
            assertEquals("OK", result.getString("PRE_AUDIT_STATUS"));
            assertEquals("OK", result.getString("POST_AUDIT_STATUS"));
        }

        assertEquals(1, nbRows);

        assertEquals(1, statement.getParameterMetaData().getParameterCount());
    }


    @Test
    public void test_postAuditStatus_bis() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("postAuditStatus", "ERROR"); // Warning : Date = 2 parameters in SelectAllWorkflowLogStatementFactory

        PreparedStatement statement = statementFactory.buildStatement(args, sqlHandler);
        // select ID, REQUEST_TYPE, REQUEST_DATE, POST_AUDIT_DATE, INITIATOR_LOGIN, DISCRIMINENT, PRE_AUDIT_STATUS, POST_AUDIT_STATUS
        // from AP_WORKFLOW_LOG
        // where POST_AUDIT_STATUS = ?
        // order by REQUEST_DATE DESC
        ResultSet result = statement.executeQuery();
        int nbRows = 0;
        while (result.next()) {
            nbRows++;
        }

        assertEquals(5, nbRows);

        assertEquals(1, statement.getParameterMetaData().getParameterCount());
    }


    public static class MyHandlerSql extends SqlHandler {

        public MyHandlerSql() {
            super(new String[]{"id"}, "", DatabaseTesterFactory.create().createDatabase());
        }
    }
}
