/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import net.codjo.gui.toolkit.wizard.Wizard;
import net.codjo.gui.toolkit.wizard.WizardPanel;
import net.codjo.i18n.common.TranslationManager;
import net.codjo.i18n.gui.TranslationNotifier;
import net.codjo.mad.gui.framework.GuiContext;
import net.codjo.mad.gui.i18n.InternationalizationUtil;
/**
 *
 */
public class WizardWindow extends JInternalFrame {
    private Wizard wizard;
    private WizardPanel wizardPanel = new WizardPanel();


    public WizardWindow(GuiContext guiContext, String title, Wizard wizard, ImageIcon wizardIcon) {
        super(title, true, true, true, true);
        this.wizard = wizard;
        setContentPane(wizardPanel);
        addCloseListener();
        TranslationNotifier notifier = InternationalizationUtil.retrieveTranslationNotifier(guiContext);
        TranslationManager manager = InternationalizationUtil.retrieveTranslationManager(guiContext);
        if (manager.hasKey(title, notifier.getLanguage())) {
            notifier.addInternationalizableComponent(this, title);
        }
        wizardPanel.setTranslationBackpack(manager, notifier);
        wizardPanel.setWizard(wizard);
        wizardPanel.setWizardIcon(wizardIcon);
        setMinimumSize(new Dimension(500, wizardPanel.getPreferredSize().height + 20));
        setPreferredSize(new Dimension(650, 450));
    }


    private void addCloseListener() {
        wizardPanel.getCancelButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
    }


    public FinalStep getFinalStep() {
        return (FinalStep)wizard.getFinalStep();
    }
}
