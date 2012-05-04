/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import net.codjo.gui.toolkit.util.ErrorDialog;
import net.codjo.mad.gui.framework.AbstractGuiAction;
import net.codjo.mad.gui.framework.GuiContext;
import net.codjo.workflow.gui.util.JobListenerAgentUtil;
import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;
/**
 *
 */
public class WizardAction extends AbstractGuiAction {
    private final WizardBuilder wizardBuilder;
    private final String title;
    private final ImageIcon wizardImage;


    public WizardAction(GuiContext ctxt, String title, String description,
                        WizardBuilder wizardBuilder, String actionId, String wizardIconId,
                        ImageIcon wizardImage) {
        super(ctxt, title, description, wizardIconId, actionId);
        this.wizardBuilder = wizardBuilder;
        this.title = title;
        this.wizardImage = wizardImage;
    }


    public void actionPerformed(ActionEvent event) {
        try {
            WizardWindow wizardWindow =
                  new WizardWindow(getGuiContext(), title, wizardBuilder.createWizard(), wizardImage);
            JobListenerAgentUtil.createGuiAgent(getGuiContext(), wizardWindow,
                                                wizardWindow.getFinalStep().getEventHandler());
        }
        catch (Exception ex) {
            ErrorDialog.show(getDesktopPane(), "Impossible d'afficher l'assistant", ex);
        }
    }
}
