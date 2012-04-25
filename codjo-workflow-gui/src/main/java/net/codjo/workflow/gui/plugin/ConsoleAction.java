/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.plugin;
import net.codjo.mad.gui.framework.AbstractGuiAction;
import net.codjo.mad.gui.framework.GuiContext;
import net.codjo.workflow.gui.util.JobListenerAgentUtil;
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
/**
 * Action permettant d'afficher la console.
 *
 * @version $Revision: 1.6 $
 */
class ConsoleAction extends AbstractGuiAction {
    ConsoleAction(GuiContext ctxt) {
        super(ctxt, "Console", "Console du serveur", "console.png");
    }


    public void actionPerformed(ActionEvent event) {
        ConsoleGui gui = new ConsoleGui();
        final ConsoleLogic consoleLogic = new ConsoleLogic(getGuiContext(), gui);

        try {
            JobListenerAgentUtil.createGuiAgent(getGuiContext(), gui,
                                                consoleLogic.getEventHandler());
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(getGuiContext().getDesktopPane(),
                                          "Impossible d'afficher la fenêtre 'Console'" + ex);
        }
    }
}
