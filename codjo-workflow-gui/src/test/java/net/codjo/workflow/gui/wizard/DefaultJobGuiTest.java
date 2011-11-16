/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import net.codjo.gui.toolkit.GradientPanel;
import net.codjo.workflow.common.message.JobAudit;
import org.uispec4j.Panel;
import org.uispec4j.UISpecTestCase;
/**
 * Classe de test de {@link net.codjo.workflow.gui.wizard.DefaultJobGui}.
 */
public class DefaultJobGuiTest extends UISpecTestCase {
    private static final String TITLE = "Import des données";
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


    protected void setUp() throws Exception {
        gui = new DefaultJobGui(TITLE);
        panel = new Panel(gui);
    }
}
