package net.codjo.workflow.gui.plugin;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.AgentContainerMock;
import net.codjo.mad.client.request.MadServerFixture;
import net.codjo.mad.gui.framework.Sender;
import net.codjo.security.common.api.UserMock;
import net.codjo.test.common.LogString;
import net.codjo.workflow.gui.WorkflowGuiContext;
import org.uispec4j.ComboBox;
import org.uispec4j.Table;
import org.uispec4j.Trigger;
import org.uispec4j.UISpecTestCase;
import org.uispec4j.Window;
import org.uispec4j.interception.WindowHandler;
import org.uispec4j.interception.WindowInterceptor;

public class WorkflowLogLogicTest extends UISpecTestCase {
    private MadServerFixture server = new MadServerFixture();
    private LogString logString = new LogString();
    private WorkflowLogGui gui;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        server.doSetUp();
        server.mockServerResult(new String[]{
              "requestType", "requestDate", "postAuditDate", "initiatorLogin", "discriminent",
              "preAuditStatus",
              "postAuditStatus"
        }, new String[][]{
              {"import", "2006-12-31 23:58:35.5", "2006-12-31 23:59:30", "crego", "toto.txt", "OK", "OK"}
        });

        WorkflowGuiContext guiContext = new WorkflowGuiContext();
        guiContext.setUser(new UserMock().mockIsAllowedTo(true));
        guiContext.setSender(new Sender(server.getOperations()));
        AgentContainer agentContainer = new AgentContainerMock(logString);

        WorkflowLogLogic logic = new WorkflowLogLogic(guiContext, agentContainer);
        gui = logic.getGui();
    }


    @Override
    public void tearDown() throws Exception {
        server.doTearDown();
        super.tearDown();
    }


    public void test_display() throws Exception {
        Window window = new Window(gui);
        Table table = window.getTable();
        assertTrue(table.getHeader().contentEquals(new String[]{
              "Type", "Discriminant", "Date de début", "Date de fin", "Initiateur", "Statut initial",
              "Statut final"
        }));
        assertTrue(table.contentEquals(
              new Object[][]{
                    {"import", "toto.txt", "2006-12-31 23:58:35", "2006-12-31 23:59:30", "crego", "OK",
                     "OK"}
              }));

        table.selectRow(0);
        assertTrue(window.getButton("WorkflowList.EditAction").isEnabled());
        assertFalse(window.getButton("WorkflowList.DeleteAction").isEnabled());
        assertTrue(window.getButton("WorkflowList.LoadAction").isEnabled());
        assertFalse(window.getButton("WorkflowList.AddAction").isEnabled());
        assertTrue(window.getButton("WorkflowList.PurgeAuditAction").isEnabled());
        assertTrue(window.getButton("WorkflowList.ExportAllPagesAction").isEnabled());
    }


    public void test_purgeAudit() throws Exception {
        server.mockServerResult(new String[]{
              "count"
        }, new String[][]{
              {"7"}
        });

        WindowInterceptor
              .init(new Trigger() {
                  public void run() throws Exception {
                      new Window(gui).getButton("WorkflowList.PurgeAuditAction").click();
                  }
              })
              .process(new WindowHandler("Préparation de la purge") {
                  @Override
                  public Trigger process(Window window) throws Exception {
                      window.assertTitleEquals("Préparation de la purge");
                      window.getTextBox("OptionPane.textField").setText("3");
                      return window.getButton("OK").triggerClick();
                  }
              })
              .process(new WindowHandler("Confirmation de la purge") {
                  @Override
                  public Trigger process(Window window) throws Exception {
                      window.assertTitleEquals("Confirmer la purge");
                      return window.getButton("Oui").triggerClick();
                  }
              })
              .run();

        // Wait untill the purge is launched
        Thread.sleep(100);
        String expectedExpression = "acceptNewAgent\\(purge-audit-.*\\), purge-audit-.*\\.start\\(\\)";
        boolean matches = logString.getContent().matches(expectedExpression);

        assertTrue("bad content:" + logString.getContent() + "\nexpected:" + expectedExpression, matches);
    }


    public void test_purgeAudit_noRowsToDelete() throws Exception {
        server.mockServerResult(new String[]{
              "count"
        }, new String[][]{
              {"0"}
        });

        WindowInterceptor
              .init(new Trigger() {
                  public void run() throws Exception {
                      new Window(gui).getButton("WorkflowList.PurgeAuditAction").click();
                  }
              })
              .process(new WindowHandler("Préparation de la purge") {
                  @Override
                  public Trigger process(Window window) throws Exception {
                      window.assertTitleEquals("Préparation de la purge");
                      window.getTextBox("OptionPane.textField").setText("3");
                      return window.getButton("OK").triggerClick();
                  }
              })
              .process(new WindowHandler("Pas de purge à supprimer") {
                  @Override
                  public Trigger process(Window window) throws Exception {
                      return window.getButton("OK").triggerClick();
                  }
              })
              .run();

        logString.assertContent("");
    }


    public void test_filter_statusDefaultValues() throws Exception {
        ComboBox preAuditStatus = new Window(gui).getComboBox("preAuditStatus");
        assertTrue(preAuditStatus.contains(new String[]{"", "ERROR", "OK", "WARNING"}));

        ComboBox postAuditStatus = new Window(gui).getComboBox("postAuditStatus");
        assertTrue(postAuditStatus.contains(new String[]{"", "ERROR", "OK", "WARNING"}));
    }
}
