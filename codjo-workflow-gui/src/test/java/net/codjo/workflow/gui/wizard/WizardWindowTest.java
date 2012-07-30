/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import net.codjo.gui.toolkit.wizard.Step;
import net.codjo.gui.toolkit.wizard.StepPanel;
import net.codjo.gui.toolkit.wizard.Wizard;
import javax.swing.ImageIcon;
import net.codjo.workflow.gui.WorkflowGuiContext;
import org.uispec4j.UISpecTestCase;
import org.uispec4j.Window;
/**
 *
 */
public class WizardWindowTest extends UISpecTestCase {
    private static final String FIRST_STEP = "Première étape";
    private static final String FINAL_STEP = "Etape finale";
    private WizardWindow gui;
    private Window window;
    private StepPanelMock firstStep;
    private StepPanelMock finalStep;


    @Override
    protected void setUp() throws Exception {
        firstStep = new StepPanelMock(FIRST_STEP);
        finalStep = new StepPanelMock(FINAL_STEP);
        gui = new WizardWindow(new WorkflowGuiContext(),
                               "Assistant diffusion",
                               new WizardBuilderMock().createWizard(),
                               new ImageIcon(getClass().getResource("started.png")));
        gui.pack();
        window = new Window(gui);
    }


    public void test_isFirstStepDisplayAtFirst() throws Exception {
        assertEquals(FIRST_STEP, window.findSwingComponent(Step.class).getName());
    }


    public void test_gotoFinalStep() throws Exception {
        window.getButton("WizardPanel.applyButton").click();
        Thread.sleep(100);
        assertEquals(FINAL_STEP, window.findSwingComponent(Step.class).getName());
    }


    public void test_notFulfilledThenYes() throws Exception {
        firstStep.setFulfilled(false);
        assertFalse(window.getButton("WizardPanel.applyButton").isEnabled());
        firstStep.setFulfilled(true);
        assertTrue(window.getButton("WizardPanel.applyButton").isEnabled());
    }


    public void test_close() throws Exception {
        gui.setVisible(true);
        assertTrue(window.isVisible());
        window.getButton("WizardPanel.cancelButton").click();
        assertFalse(window.isVisible());
    }


    public void test_hasCloseIcon() throws Exception {
        assertTrue(gui.isClosable());
    }


    private static class StepPanelMock extends StepPanel {
        private StepPanelMock(String name) {
            setName(name);
            setFulfilled(true);
        }
    }

    private class WizardBuilderMock implements WizardBuilder {
        public Wizard createWizard() {
            Wizard wizard = new Wizard();
            wizard.addStep(firstStep);
            wizard.addStep(finalStep);
            wizard.setFinalStep(finalStep);
            return wizard;
        }
    }
}
