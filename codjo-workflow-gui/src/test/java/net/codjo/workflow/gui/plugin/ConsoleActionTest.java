/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.plugin;
import net.codjo.workflow.gui.GuiAgentActionTestCase;
import javax.swing.Action;
/**
 * Classe de test de {@link ConsoleAction}.
 */
public class ConsoleActionTest extends GuiAgentActionTestCase {
    @Override
    protected Action createAction() {
        return new ConsoleAction(getGuiContext());
    }
}
