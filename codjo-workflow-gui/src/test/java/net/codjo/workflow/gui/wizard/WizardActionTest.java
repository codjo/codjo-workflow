/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import net.codjo.gui.toolkit.wizard.StepPanel;
import net.codjo.gui.toolkit.wizard.Wizard;
import net.codjo.workflow.common.message.JobRequestTemplate;
import net.codjo.workflow.gui.GuiAgentActionTestCase;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.codjo.workflow.gui.WorkflowGuiContext;
/**
 * Classe de test de {@link net.codjo.workflow.gui.wizard.WizardAction}.
 */
public class WizardActionTest extends GuiAgentActionTestCase {
    @Override
    protected Action createAction() {
        return new WizardAction(getGuiContext(), "title", "desctription",
                                new WizardBuilderMock(), "WizardAction", null, null);
    }


    private class WizardBuilderMock implements WizardBuilder {
        public Wizard createWizard() {
            WorkflowGuiContext guiContext = new WorkflowGuiContext();
            FinalStep finalStep =
                  new FinalStep("Exporter...", new WizardActionTest.VtomCallerMock(),
                                new SummaryGuiMock(), new DefaultJobGui(guiContext, ""),
                                new FinalStep.JobGuiData[]{
                                      new FinalStep.JobGuiData(new DefaultJobGui(guiContext, ""),
                                                               new DefaultRequestTemplateFactory(
                                                                     JobRequestTemplate.matchAll()))
                                });

            Wizard wizard = new Wizard();
            wizard.addStep(new StepPanel());
            wizard.setFinalStep(finalStep);
            return wizard;
        }
    }

    class VtomCallerMock implements VtomCaller {
        public void call(Map wizardState) throws CommandFile.ExecuteException {
        }
    }

    class SummaryGuiMock implements FinalStep.WizardSummaryGui {
        public JComponent getGui() {
            return new JPanel();
        }


        public void display(Map requestState) {
        }
    }
}
