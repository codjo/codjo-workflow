/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.util;
import net.codjo.agent.AgentContainerMock;
import net.codjo.mad.gui.base.GuiPlugin;
import net.codjo.mad.gui.framework.DefaultGuiContext;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.subscribe.JobEventHandlerMock;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import junit.framework.TestCase;
/**
 * Classe de test de {@link net.codjo.workflow.gui.util.JobListenerAgentUtil}.
 */
public class JobListenerAgentUtilTest extends TestCase {
    private LogString log = new LogString();
    private JDesktopPane desktopPane;
    private DefaultGuiContext ctxt;
    private AgentContainerMock containerMock;


    public void test_closeInternalFrame() throws Exception {
        JobListenerAgentUtil.createGuiAgent(ctxt, new JInternalFrame(),
                                            new JobEventHandlerMock());

        assertEquals(1, desktopPane.getAllFrames().length);

        String listenerName = containerMock.getLastNickname();
        log.assertContent("container.acceptNewAgent(" + listenerName
                          + ", JobListenerAgent), " + listenerName + ".start()");
        log.clear();

        JInternalFrame gui = desktopPane.getAllFrames()[0];
        gui.dispose();

        log.assertContent(listenerName + ".kill()");
    }


    @Override
    protected void setUp() throws Exception {
        desktopPane = new JDesktopPane();
        ctxt = new DefaultGuiContext(desktopPane);

        containerMock = new AgentContainerMock(new LogString("container", log));
        ctxt.putProperty(GuiPlugin.AGENT_CONTAINER_KEY, containerMock);
    }
}
