/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.server.audit;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.agent.test.DummyAgent;
import net.codjo.database.common.api.JdbcFixture;
import static net.codjo.database.common.api.structure.SqlTable.table;
import net.codjo.datagen.DatagenFixture;
import net.codjo.sql.server.JdbcServiceUtilMock;
import net.codjo.test.common.LogString;
import net.codjo.test.common.fixture.CompositeFixture;
import net.codjo.tokio.TokioFixture;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobRequest;
import static net.codjo.workflow.server.TestUtil.createRequest;
import java.io.File;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
/**
 * Classe de test de {@link JdbcAuditDao}.
 */
public class JdbcAuditDaoTest {
    private static final String TABLE_NAME = "AP_WORKFLOW_LOG";
    private Agent agent = new DummyAgent();
    private TokioFixture tokioFixture = new TokioFixture(getClass());
    private AclMessage message = new AclMessage(AclMessage.Performative.NOT_UNDERSTOOD);
    private JdbcAuditDao dao;


    @BeforeClass
    public static void createTable() throws Exception {
        DatagenFixture datagen = new DatagenFixture(JdbcAuditDaoTest.class);
        JdbcFixture jdbc = JdbcFixture.newFixture();
        CompositeFixture fixture = new CompositeFixture(jdbc, datagen);

        fixture.doSetUp();
        try {
            datagen.generate();
            jdbc.advanced().dropAllObjects();

            jdbc.advanced().executeCreateTableScriptFile(new File(datagen.getSqlPath(), TABLE_NAME + ".tab"));
        }
        catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
        finally {
            fixture.doTearDown();
        }
    }


    @AfterClass
    public static void dropTable() throws Exception {
        JdbcFixture jdbc = JdbcFixture.newFixture();

        jdbc.doSetUp();
        jdbc.advanced().dropAllObjects();
        jdbc.doTearDown();
    }


    @Before
    public void doSetup() throws Exception {
        JdbcFixture jdbcFixture = JdbcFixture.newFixture();
        tokioFixture.doSetUp(jdbcFixture);
        tokioFixture.getJdbcFixture().delete(table(TABLE_NAME));
        dao = createDao();
    }


    @After
    public void doTearDown() throws Exception {
        tokioFixture.doTearDown();
    }


    @Test
    public void test_saveRequest() throws Exception {
        JobRequest request = createRequest("import", "request-125", "request-124", "crego");
        request.setArguments(new Arguments("file", "myfile.txt"));

        dao.saveRequest(agent, message, request);

        tokioFixture.assertAllOutputs("saveRequest");
    }


    @Test
    public void test_saveRequest_noArguments() throws Exception {
        JobRequest request = createRequest("import", "request-125", "request-124", "crego");
        request.setArguments(null);

        dao.saveRequest(agent, message, request);

        tokioFixture.assertAllOutputs("saveRequestNoArguments");
    }


    @Test
    public void test_saveRequest_multipleLinesArguments() throws Exception {
        Arguments arguments = new Arguments("file", "myfile.txt");
        arguments.put("comment", "nothing");
        JobRequest request = createRequest("import", "request-125", "request-124", "crego", arguments);

        dao.saveRequest(agent, message, request);

        tokioFixture.assertAllOutputs("saveRequestMultipleLinesArguments");
    }


    @Test
    public void test_saveRequest_withStringifier() throws Exception {
        Map<String, Stringifier> stringifiers = new HashMap<String, Stringifier>();
        stringifiers.put("import", new Stringifier() {
            public String toString(JobRequest jobRequest) {
                return "file=toto.txt";
            }
        });
        stringifiers.put("segmentation", new Stringifier() {
            public String toString(JobRequest jobRequest) {
                return "axe=titi";
            }
        });
        dao = createDao(stringifiers);

        dao.saveRequest(agent,
                        message,
                        createRequest("broadcast",
                                      "request-126",
                                      "request-124",
                                      "crego",
                                      new Arguments("file", "myfile.txt")));
        dao.saveRequest(agent,
                        message,
                        createRequest("import",
                                      "request-125",
                                      "request-124",
                                      "crego",
                                      new Arguments("comment", "nothing")));

        tokioFixture.assertAllOutputs("saveRequestWithStringifier");
    }


    @Test
    public void test_savePreAudit() throws Exception {
        dao.saveRequest(agent, message,
                        createRequest("import", "request-125", "request-124", "crego"));

        JobAudit audit = new JobAudit(JobAudit.Type.PRE);
        audit.setRequestId("request-125");
        audit.setArguments(new Arguments("importedTable", "AP_MA_TABLE"));

        dao.saveAudit(agent, message, audit);

        tokioFixture.assertAllOutputs("savePreAudit");
    }


    @Test
    public void test_savePreAudit_warning() throws Exception {
        dao.saveRequest(agent, message,
                        createRequest("import", "request-125", "request-124", "crego"));

        JobAudit audit = new JobAudit(JobAudit.Type.PRE);
        audit.setRequestId("request-125");
        audit.setArguments(new Arguments("importedTable", "AP_MA_TABLE"));
        audit.setWarning(new JobAudit.Anomaly("message warning", "description du warning"));

        dao.saveAudit(agent, message, audit);

        tokioFixture.assertAllOutputs("savePreAuditWithWarning");
    }


    @Test
    public void test_savePostAudit() throws Exception {
        dao.saveRequest(agent, message,
                        createRequest("import", "request-125", "request-124", "crego"));

        JobAudit audit = new JobAudit(JobAudit.Type.POST);
        audit.setRequestId("request-125");
        audit.setArguments(new Arguments("importedTable", "AP_MA_TABLE"));

        dao.saveAudit(agent, message, audit);

        tokioFixture.assertAllOutputs("savePostAudit");
    }


    @Test
    public void test_saveAudit_withBadRequestId() throws Exception {
        JobAudit audit = new JobAudit(JobAudit.Type.POST);
        audit.setRequestId("unknown-request-125");

        try {
            dao.saveAudit(agent, message, audit);
            fail();
        }
        catch (UnknownRequestIdException ex) {
            assertEquals(UnknownRequestIdException.computeMessage(audit), ex.getMessage());
        }
    }


    @Test
    public void test_findRequest() throws Exception {
        dao.saveRequest(agent, message, createRequest("import", "request-126", null, "crego"));
        dao.saveRequest(agent, message, createRequest("import", "request-127", null, "crego"));

        JobRequest expected = createRequest("import", "request-125", "expected-124", "crego");
        expected.setDate(toDate("2007-02-01 10:00:00"));
        expected.setArguments(new Arguments("file", "myfile.txt"));
        dao.saveRequest(agent, message, expected);

        List<JobRequest> result = dao.findRequest(agent, message,
                                                  "import",
                                                  toDate("2007-02-01 10:00:00"),
                                                  toDate("2007-02-01 20:00:00"));

        assertEquals(1, result.size());
        JobRequest actual = result.get(0);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getParentId(), actual.getParentId());
        assertEquals(expected.getInitiatorLogin(), actual.getInitiatorLogin());
        assertEquals(expected.getInitiatorLogin(), actual.getInitiatorLogin());
        assertEquals(expected.getDate(), actual.getDate());
        assertEquals(expected.getArguments().encode(), actual.getArguments().encode());
    }


    @Test
    public void test_findRequest_noRequest() throws Exception {
        List<JobRequest> result = dao.findRequest(agent, message,
                                                  "my-import",
                                                  toDate("2007-02-01 10:00:00"),
                                                  toDate("2007-02-01 10:00:00"));
        assertEquals(0, result.size());
    }


    @Test
    public void test_deleteAudit() throws Exception {
        tokioFixture.insertInputInDb("deleteAudit");

        dao.deleteAudit(null, null, toDate("2008-12-01 00:00:00"));

        tokioFixture.assertAllOutputs("deleteAudit");
    }


    private JdbcAuditDao createDao() {
        return new JdbcAuditDao(new JdbcServiceUtilMock(new LogString(), tokioFixture.getJdbcFixture()));
    }


    private JdbcAuditDao createDao(Map<String, Stringifier> stringifiers) {
        return new JdbcAuditDao(new JdbcServiceUtilMock(new LogString(), tokioFixture.getJdbcFixture()),
                                new DiscriminentStringifier(stringifiers));
    }


    private Date toDate(String date) {
        return Timestamp.valueOf(date);
    }
}
