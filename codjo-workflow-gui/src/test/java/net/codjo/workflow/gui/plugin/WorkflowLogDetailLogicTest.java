package net.codjo.workflow.gui.plugin;
import net.codjo.mad.client.request.Field;
import net.codjo.mad.client.request.FieldsList;
import net.codjo.mad.client.request.MadServerFixture;
import net.codjo.mad.client.request.Row;
import net.codjo.mad.gui.framework.DefaultGuiContext;
import net.codjo.mad.gui.request.DetailDataSource;
import org.uispec4j.TextBox;
import org.uispec4j.UISpecTestCase;
import org.uispec4j.Window;
/**
 *
 */
public class WorkflowLogDetailLogicTest extends UISpecTestCase {
    private MadServerFixture server = new MadServerFixture();


    public void test_display() throws Exception {
        server.mockServerResult(new String[]{"id",
                                             "parentId",
                                             "requestDate",
                                             "initiatorLogin",
                                             "requestType",
                                             "argument",
                                             "preAuditStatus",
                                             "preAuditDate",
                                             "preAuditArgument",
                                             "preAuditAnomalyMessage",
                                             "preAuditAnomalyTrace",
                                             "postAuditStatus",
                                             "postAuditDate",
                                             "postAuditArgument",
                                             "postAuditAnomalyMessage",
                                             "postAuditAnomalyTrace",
                                             "discriminent"},
                                new String[][]{
                                      {"request-69",
                                       "request-68",
                                       "2006-12-31 23:50:35",
                                       "crego",
                                       "import",
                                       "file=toto.txt",
                                       "OK", "2006-12-31 23:52:35",
                                       "null",
                                       "null",
                                       "null",
                                       "ERROR", "2006-12-31 23:54:35",
                                       "encodage=UTF8",
                                       "no space left on device",
                                       "/dev/vx/dsk/rootdg/home 100%",
                                       "toto.txt"}
                                });

        WorkflowLogDetailLogic logic = new WorkflowLogDetailLogic(createReadOnlyDetailDataSource());

        Window window = new Window(logic.getGui());

        Row row = server.getServerResult().getRow(0);
        for (int i = 0; i < row.getFieldCount(); i++) {
            Field field = row.getField(i);
            TextBox inputTextBox = window.getInputTextBox(field.getName());
            assertEquals("field (" + field.getName() + ")", computeExpectedValue(field),
                         inputTextBox.getText());
            assertFalse(inputTextBox.isEditable());
        }
    }


    private String computeExpectedValue(Field field) {
        return "null".equals(field.getValue()) ? "" : field.getValue();
    }


    @Override
    protected void setUp() throws Exception {
        server.doSetUp();
    }


    @Override
    protected void tearDown() throws Exception {
        server.doTearDown();
    }


    public DetailDataSource createReadOnlyDetailDataSource() {
        DetailDataSource dataSource = new DetailDataSource(new DefaultGuiContext());
        dataSource.setLoadFactoryId("selectById");
        dataSource.setSelector(new FieldsList("id", "my-id-value"));
        return dataSource;
    }
}
