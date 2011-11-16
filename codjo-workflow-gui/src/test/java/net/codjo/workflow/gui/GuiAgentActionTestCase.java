/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui;
import net.codjo.agent.AgentContainerMock;
import net.codjo.mad.gui.base.GuiPlugin;
import net.codjo.mad.gui.framework.DefaultGuiContext;
import net.codjo.security.common.api.UserMock;
import net.codjo.test.common.LogString;
import javax.swing.Action;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import junit.framework.TestCase;
/**
 * TestCase permettant de valider les actions affichant une IHM connecté à un agent {@link
 * net.codjo.workflow.common.subscribe.JobListenerAgent}.
 */
public abstract class GuiAgentActionTestCase extends TestCase {
    private Action action;
    private JDesktopPane desktopPane;
    private LogString log = new LogString();
    private DefaultGuiContext guiContext;


    public void test_actionPerformed() throws Exception {
        AgentContainerMock containerMock =
              new AgentContainerMock(new LogString("container", log));
        guiContext.putProperty(GuiPlugin.AGENT_CONTAINER_KEY, containerMock);

        action.actionPerformed(null);

        assertEquals(1, desktopPane.getAllFrames().length);

        String listenerName = containerMock.getLastNickname();
        log.assertContent("container.acceptNewAgent(" + listenerName
                          + ", JobListenerAgent), " + listenerName + ".start()");
        log.clear();

        JInternalFrame gui = desktopPane.getAllFrames()[0];
        gui.dispose();

        log.assertContent(listenerName + ".kill()");
    }


    public DefaultGuiContext getGuiContext() {
        return guiContext;
    }


    protected void setUp() throws Exception {
        desktopPane = new JDesktopPane();
        guiContext = new DefaultGuiContext(desktopPane);
        guiContext.setUser(new UserMock().mockIsAllowedTo(true));

        action = createAction();
    }


    protected abstract Action createAction();
}
