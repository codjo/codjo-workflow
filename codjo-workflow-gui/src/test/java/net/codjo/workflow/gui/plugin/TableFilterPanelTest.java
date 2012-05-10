package net.codjo.workflow.gui.plugin;
import net.codjo.mad.client.request.MadServerFixture;
import net.codjo.mad.gui.framework.DefaultGuiContext;
import net.codjo.mad.gui.framework.Sender;
import net.codjo.mad.gui.request.Column;
import net.codjo.mad.gui.request.Preference;
import net.codjo.mad.gui.request.RequestTable;
import net.codjo.security.common.api.UserMock;
import net.codjo.workflow.gui.WorkflowGuiContext;
import net.codjo.workflow.gui.plugin.TableFilterPanel.FilterType;
import java.util.Arrays;
import javax.swing.JComboBox;
import org.uispec4j.Button;
import org.uispec4j.ComboBox;
import org.uispec4j.Panel;
import org.uispec4j.UISpecTestCase;

public class TableFilterPanelTest extends UISpecTestCase {
    private MadServerFixture server = new MadServerFixture();
    private TableFilterPanel tableFilterPanel;
    private RequestTable requestTable;
    private Panel panel;
    private Button applyButton;
    private Button resetButton;
    private DefaultGuiContext guiContext;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        server.doSetUp();

        guiContext = new WorkflowGuiContext();
        guiContext.setUser(new UserMock().mockIsAllowedTo(true));
        guiContext.setSender(new Sender(server.getOperations()));


        initRequestTable();
        initTableFilterPanel();
    }


    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        server.doTearDown();
    }


    public void test_nominal() throws Exception {
        ComboBox comboBox = panel.getComboBox("couleur");
        comboBox.selectionEquals(" ");
        comboBox.contains(new String[0]);
        assertEquals(0, requestTable.getRowCount());
        assertEquals(3, requestTable.getColumnCount());
        tableFilterPanel.apply();

        comboBox.contentEquals(new String[]{"", "blanc", "bleu", "noir", "rouge", "vert"}).check();
        assertEquals(6, requestTable.getRowCount());
        assertEquals(3, requestTable.getColumnCount());
    }


    public void test_reset() throws Exception {
        server.mockServerResult(new String[]{
              "nom", "couleur", "film"
        }, new String[][]{
              {"TOTO", "vert", "Jurassik Park"},
              {"TATA", "rouge", "Iron Man"},
              {"TITI", "vert", "Retour vers le futur"},
              {"YOYO", "bleu", "Iron Man"},
              {"SOLO", "noir", "Autant en emporte le vent"},
              {"YANN", "blanc", "Autant en emporte le vent"},
        });

        tableFilterPanel.apply();

        ComboBox comboBox = panel.getComboBox("couleur");
        comboBox.select("vert");
        resetButton.click();

        comboBox.selectionEquals("").check();
        assertEquals(2, requestTable.getSelectors().getFieldCount());
        assertEquals("null", requestTable.getSelectors().getFieldValue("couleur"));
        assertEquals("null", requestTable.getSelectors().getFieldValue("film"));
    }


    public void test_filterByCombo() throws Exception {
        server.mockServerResult(new String[]{
              "nom", "couleur", "film"
        }, new String[][]{
              {"TOTO", "vert", "Jurassik Park"},
              {"TITI", "vert", "Retour vers le futur"},
        });

        tableFilterPanel.apply();

        ComboBox comboBox = panel.getComboBox("couleur");
        comboBox.select("vert");
        applyButton.click();

        assertEquals(2, requestTable.getSelectors().getFieldCount());
        assertEquals("vert", requestTable.getSelectors().getFieldValue("couleur"));
        assertEquals("null", requestTable.getSelectors().getFieldValue("film"));
    }


    public void test_filterByText() throws Exception {
        server.mockServerResult(new String[]{
              "nom", "couleur", "film"
        }, new String[][]{
              {"YOYO", "bleu", "Iron Man"},
        });

        tableFilterPanel.apply();

        ComboBox comboBox = panel.getComboBox("couleur");
        comboBox.setText("bleu");
        applyButton.click();

        assertEquals(2, requestTable.getSelectors().getFieldCount());
        assertEquals("bleu", requestTable.getSelectors().getFieldValue("couleur"));
        assertEquals("null", requestTable.getSelectors().getFieldValue("film"));
    }


    public void test_resetSelector() {
    }


    public void test_dateFilter() throws Exception {
        tableFilterPanel.addFilter("Date", "date", FilterType.DATE);

        tableFilterPanel.apply();

        ComboBox comboBox = panel.getComboBox("date");
        comboBox.contentEquals(new String[]{"", "2008-01-01", "2006-02-08", "2001-01-01"}).check();
    }


    public void test_addFilter() throws Exception {
        assertFalse(panel.containsSwingComponent(JComboBox.class, "newFilter"));

        tableFilterPanel.addFilter("New Filter", "newFilter", FilterType.TEXT);

        assertTrue(panel.containsSwingComponent(JComboBox.class, "newFilter"));
    }


    public void test_addFilter_filterExists() throws Exception {
        try {
            tableFilterPanel.addFilter("Couleur", "couleur", FilterType.TEXT);
            fail();
        }
        catch (Exception e) {
            assertEquals("Un filtre a déjà été mis pour le champ couleur", e.getMessage());
        }
    }


    public void test_addFilter_defaultValues() throws Exception {
        tableFilterPanel.addFilter("Nom", "nom", FilterType.TEXT, "Albert", "Gérard", "TITI");

        tableFilterPanel.apply();

        ComboBox comboBox = panel.getComboBox("nom");
        comboBox.contentEquals(new String[]{
              "", "Albert", "Gérard", "TITI", "SOLO", "TATA", "TOTO", "YANN", "YOYO"}).check();
    }


    public void test_sortCombo_notCaseSensitive() throws Exception {
        tableFilterPanel.apply();

        server.mockServerResult(new String[]{
              "nom", "couleur", "film", "date"
        }, new String[][]{
              {"TOTO", "vert", "Jurassik Park", "2001-01-01 00:00:00"},
              {"TATA", "ROUGE", "Iron Man", "2008-01-01 00:00:00"},
              {"TATA", TableFilterPanel.NULL_FOR_MAD, "Iron Man", "2008-01-01 00:00:00"},
              {"TATA", "rouge", "Iron Man", "2008-01-01 00:00:00"},
              {"TITI", "vert", "Retour vers le futur", "2001-01-01 01:01:01"},
              {"YOYO", "bleu", "Iron Man", "2001-01-01 01:01:01"},
              {"TATA", TableFilterPanel.NULL_FOR_MAD, "Iron Man", "2008-01-01 00:00:00"},
              {"SOLO", "noir", "Autant en emporte le vent", "2006-02-08 01:24:50"},
              {"YANN", "blanc", "Autant en emporte le vent", "2006-02-08 12:2:59"},
        });
        tableFilterPanel.apply();

        ComboBox comboBox = panel.getComboBox("couleur");
        comboBox.contentEquals(new String[]{"", "blanc", "bleu", "noir", "ROUGE", "rouge", "vert"}).check();
    }


    private void initRequestTable() {
        server.mockServerResult(new String[]{
              "nom", "couleur", "film", "date"
        }, new String[][]{
              {"TOTO", "vert", "Jurassik Park", "2001-01-01 00:00:00"},
              {"TATA", "rouge", "Iron Man", "2008-01-01 00:00:00"},
              {"TITI", "vert", "Retour vers le futur", "2001-01-01 01:01:01"},
              {"YOYO", "bleu", "Iron Man", "2001-01-01 01:01:01"},
              {"SOLO", "noir", "Autant en emporte le vent", "2006-02-08 01:24:50"},
              {"YANN", "blanc", "Autant en emporte le vent", "2006-02-08 12:2:59"},
        });

        requestTable = new RequestTable();
        Preference preference = new Preference();
        preference.setColumns(Arrays.asList(new Column("nom", "Nom"),
                                            new Column("couleur", "Couleur"),
                                            new Column("film", "Film")));
        preference.setSelectAllId("dummySelectAllId");
        requestTable.setPreference(preference);
        requestTable.getDataSource().setGuiContext(guiContext);
    }


    private void initTableFilterPanel() {
        tableFilterPanel = new TableFilterPanel(guiContext, requestTable);
        tableFilterPanel.addFilter("Couleur", "couleur", FilterType.TEXT);
        tableFilterPanel.addFilter("Film", "film", FilterType.TEXT);
        panel = new Panel(tableFilterPanel);
        applyButton = panel.getButton("apply");
        resetButton = panel.getButton("reset");
    }
}
