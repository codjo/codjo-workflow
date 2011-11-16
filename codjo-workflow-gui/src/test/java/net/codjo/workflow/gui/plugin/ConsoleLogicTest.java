/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.plugin;
import net.codjo.agent.AclMessage;
import net.codjo.test.common.GuiUtil;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.subscribe.ProtocolErrorEvent;
import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.text.JTextComponent;
import org.uispec4j.Table;
import org.uispec4j.UISpecTestCase;
/**
 * Classe de test de {@link ConsoleLogic}.
 */
public class ConsoleLogicTest extends UISpecTestCase {
    private ConsoleGui gui;
    private ConsoleLogic logic;


    protected void setUp() throws Exception {
        gui = new ConsoleGui();
        logic = new ConsoleLogic(gui);
    }


    public void test_getEventHandler() throws Exception {
        assertNotNull(logic.getEventHandler());
        assertSame(logic.getEventHandler(), logic.getEventHandler());
    }


    public void test_receiveRequest_first() throws Exception {
        logic.getEventHandler().receive(createRequestEvent("job-1", "import"));

        JTree tree = getRequestTree();

        assertEquals("[Root, import (job-1)]", GuiUtil.uiDisplayedContent(tree));
    }


    public void test_receiveRequest_unrelated() throws Exception {
        logic.getEventHandler().receive(createRequestEvent("job-1", "import"));
        logic.getEventHandler().receive(createRequestEvent("job-2", "control"));

        JTree tree = getRequestTree();

        assertEquals("[Root, import (job-1)] [Root, control (job-2)]",
                     GuiUtil.uiDisplayedContent(tree));
    }


    public void test_receiveRequest_related() throws Exception {
        logic.getEventHandler().receive(createRequestEvent("job-1", "import"));
        logic.getEventHandler().receive(createRequestEvent("job-1", "job-11", "control"));

        JTree tree = getRequestTree();

        assertEquals("[Root, import (job-1)] [Root, import (job-1), control (job-11)]",
                     GuiUtil.uiDisplayedContent(tree));
    }


    public void test_selectRequest_noAudit() throws Exception {
        JobRequest request = createRequest("job-1", "import");
        request.setInitiatorLogin("guillie");
        request.setDate(java.sql.Timestamp.valueOf("2005-12-01 10:00:01.0"));
        request.setArguments(new Arguments("file", "c:/file.txt"));
        logic.getEventHandler().receive(new JobEvent(request));

        logic.selectRequest("job-1");

        JTextComponent requestDetail = getRequestDetail();

        String expected = "Type = import";
        expected += "\nDate = 01/12/2005 10:00:01";
        expected += "\nInitiateur = guillie";
        expected += "\nArguments :";
        expected += "\nfile=c:/file.txt\n";
        assertEquals(expected, requestDetail.getText());

        assertEquals(0, getAuditTable().getRowCount());
    }


    public void test_selectRequest_withAudit() throws Exception {
        logic.getEventHandler().receive(createRequestEvent("job-1", "import"));

        JobAudit audit = new JobAudit(JobAudit.Type.PRE);
        audit.setRequestId("job-1");
        audit.setDate(java.sql.Timestamp.valueOf("2006-01-30 10:20:30"));
        logic.getEventHandler().receive(new JobEvent(audit));

        audit = new JobAudit(JobAudit.Type.POST);
        audit.setRequestId("job-1");
        audit.setDate(java.sql.Timestamp.valueOf("2006-01-30 10:20:35"));
        audit.setArguments(new Arguments("key", "value"));
        logic.getEventHandler().receive(new JobEvent(audit));

        logic.selectRequest("job-1");

        Table auditTable = new Table(getAuditTable());
        assertTrue(auditTable.getHeader().contentEquals(new String[]{
              "Type", "Date", "Argument", "Erreur"
        }));

        assertTrue(auditTable.contentEquals(
              new Object[][]{
                    {"PRE", "10:20:30 (30/01/2006)", "", ""},
                    {"POST", "10:20:35 (30/01/2006)", "key=value\n", ""}
              }));
        assertTrue(auditTable.backgroundEquals(
              new Object[][]{
                    {"white", "white", "white", "white"},
                    {"white", "white", "white", "white"}
              }));
    }


    public void test_selectRequest_withAuditError()
          throws Exception {
        logic.getEventHandler().receive(createRequestEvent("job-1", "import"));

        JobAudit audit = new JobAudit(JobAudit.Type.PRE);
        audit.setRequestId("job-1");
        audit.setErrorMessage("error message");
        audit.setDate(java.sql.Timestamp.valueOf("2006-01-30 10:20:30"));
        logic.getEventHandler().receive(new JobEvent(audit));

        logic.selectRequest("job-1");

        Table auditTable = new Table(getAuditTable());
        assertTrue(auditTable.contentEquals(
              new Object[][]{
                    {"PRE", "10:20:30 (30/01/2006)", "", "error message"}
              }));
        assertTrue(auditTable.backgroundEquals(
              new Object[][]{
                    {"red", "red", "red", "red"}
              }));
    }


    public void test_closeButton() throws Exception {
        logic = new ConsoleLogic(gui);

        JDesktopPane desktopPane = new JDesktopPane();
        desktopPane.add(gui);

        ((JButton)GuiUtil.findByName("closeButton", gui)).doClick();

        assertEquals(0, desktopPane.getAllFrames().length);
    }


    public void test_receiveError() throws Exception {
        AclMessage failure = new AclMessage(AclMessage.Performative.FAILURE);
        failure.setLanguage("perl");
        ProtocolErrorEvent event =
              new ProtocolErrorEvent(ProtocolErrorEvent.Type.FAILURE, failure);
        logic.getEventHandler().receiveError(event);

        JTextComponent errorArea = (JTextComponent)getComponent("errorArea");

        assertEquals(">>>>>\n" + ">>>>>\n" + ">>>>>\n" + failure.toFipaACLString() + "\n",
                     errorArea.getText());
    }


    private JobRequest createRequest(String parentId, String id, String type) {
        JobRequest request = createRequest(id, type);
        request.setParentId(parentId);
        return request;
    }


    private JobRequest createRequest(String id, String type) {
        JobRequest request = new JobRequest();
        request.setId(id);
        request.setType(type);
        return request;
    }


    private JobEvent createRequestEvent(String id, String type) {
        return new JobEvent(createRequest(id, type));
    }


    private JobEvent createRequestEvent(String parentId, String id, String type) {
        return new JobEvent(createRequest(parentId, id, type));
    }


    private JTree getRequestTree() {
        return (JTree)getComponent("requestTree");
    }


    private JTextComponent getRequestDetail() {
        return (JTextComponent)getComponent("requestDetail");
    }


    private JTable getAuditTable() {
        return (JTable)getComponent("auditTable");
    }


    private Component getComponent(String name) {
        Component component = GuiUtil.findByName(name, gui.getContent());
        assertNotNull(component);
        return component;
    }
}
