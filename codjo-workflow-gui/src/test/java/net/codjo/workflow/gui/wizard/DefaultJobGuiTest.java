/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ListResourceBundle;
import javax.swing.JFrame;
import net.codjo.gui.toolkit.GradientPanel;
import net.codjo.i18n.common.Language;
import net.codjo.i18n.common.TranslationManager;
import net.codjo.mad.gui.i18n.InternationalizationUtil;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.gui.WorkflowGuiContext;
import org.uispec4j.Panel;
import org.uispec4j.UISpecTestCase;
/**
 * Classe de test de {@link net.codjo.workflow.gui.wizard.DefaultJobGui}.
 */
public class DefaultJobGuiTest extends UISpecTestCase {
    private static final String TITLE = "Import des données";
    private static final String TITLE_KEY = "DefaultJobGui.title";
    private DefaultJobGui gui;
    private Panel panel;


    public void test_title() throws Exception {
        assertEquals(TITLE, panel.getTextBox("title").getText());
    }


    public void test_progressPanel() throws Exception {
        assertEquals(GradientPanel.getDefaultEndColor(), gui.getStartColor());

        gui.displayStart();

        assertEquals(GradientPanel.getDefaultStartColor(), gui.getStartColor());
        assertEquals(TITLE, panel.getTextBox("title").getText());
        assertEquals(DefaultJobGui.DEFAULT_PROGRESS_MESSAGE,
                     panel.getTextBox("progressLabel").getText());
        assertTrue(panel.getProgressBar().completionEquals(-1));
        assertFalse(panel.getProgressBar().isCompleted());

        gui.displayStop(new JobAudit());

        assertEquals(GradientPanel.getDefaultEndColor(), gui.getStartColor());
        assertEquals(DefaultJobGui.FINISHED_PROGRESS_MESSAGE,
                     panel.getTextBox("progressLabel").getText());
        assertTrue(panel.getProgressBar().isCompleted());
    }


    public void test_error() throws Exception {
        JobAudit audit = new JobAudit();
        audit.setErrorMessage("error");
        gui.displayStop(audit);

        assertEquals(DefaultJobGui.ERROR_COLOR, gui.getStartColor());
        assertEquals("<html><u>error</u></html>",
                     panel.getTextBox("progressLabel").getText());
    }


    public void test_error_longMessage() throws Exception {
        JobAudit audit = new JobAudit();
        String sixtyChar = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        audit.setErrorMessage(sixtyChar + "afterafter");
        gui.displayStop(audit);

        assertEquals(DefaultJobGui.ERROR_COLOR, gui.getStartColor());
        assertEquals("<html><u>" + sixtyChar + "...</u></html>",
                     panel.getTextBox("progressLabel").getText());
    }


    public void test_warning() throws Exception {
        JobAudit audit = new JobAudit();
        audit.setWarningMessage("Contrôle terminé : 25 lignes en quarantaine.");
        gui.displayStop(audit);

        assertEquals(DefaultJobGui.WARNING_COLOR, gui.getStartColor());
        assertEquals("Contrôle terminé : 25 lignes en quarantaine.",
                     panel.getTextBox("progressLabel").getText());
    }


    @Override
    protected void setUp() throws Exception {
        WorkflowGuiContext guiContext = new WorkflowGuiContext();
        TranslationManager translationManager =
              InternationalizationUtil.retrieveTranslationManager(guiContext);
        translationManager.addBundle(new MyFrenchResources(), Language.FR);
        translationManager.addBundle(new MyEnglishResources(), Language.EN);

        gui = new DefaultJobGui(guiContext, TITLE_KEY);
        panel = new Panel(gui);
    }


    private static class MyFrenchResources extends ListResourceBundle {
        private static final Object[][] CONTENTS = new Object[][]{
              {"DefaultJobGui.title", "Import des données"},
        };


        @Override
        public Object[][] getContents() {
            return CONTENTS;
        }
    }

    private static class MyEnglishResources extends ListResourceBundle {
        private static final Object[][] CONTENTS = new Object[][]{
              {"DefaultJobGui.title", "Data Import"},
        };


        @Override
        public Object[][] getContents() {
            return CONTENTS;
        }
    }


    public static void main(String[] args) throws InterruptedException {
        JFrame frame = new JFrame("Test DefaultJobGuiTest");
        DefaultJobGui contentPane = new DefaultJobGui(new WorkflowGuiContext(), TITLE_KEY);
        frame.setContentPane(contentPane);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });
        contentPane.displayStart();
        Thread.sleep(3000);
        JobAudit jobAudit = new JobAudit();
        jobAudit.setError(new JobAudit.Anomaly("[message] toto; [process] process", new Throwable("eee")));
        contentPane.displayStop(jobAudit);
    }
}
