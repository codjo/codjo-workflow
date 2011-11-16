package net.codjo.workflow.gui.task;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import javax.swing.DefaultListModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
/**
 *
 */
@Ignore("Incompatibilité entre le robot et les tests UISpec")
public class TaskManagerGuiIgnoredTest {
    private Robot robot;


    @Before
    public void setUp() throws AWTException {
        robot = new Robot();
        robot.setAutoWaitForIdle(true);
        robot.setAutoDelay(1);
    }


    @Test
    public void test_moveWindow() throws Exception {
        TaskManagerGui managerGui = new TaskManagerGui(new TaskManagerConfiguration(),
                                                       new DefaultListModel(),
                                                       new DefaultListModel(), new DefaultListModel());
        managerGui.setVisible(true);
        try {
            dragComponent(managerGui, 10, 10, 100, 50);
            assertEquals(new Point(100, 50), managerGui.getLocation());

            dragComponent(managerGui, 10, 10, 250, 400);
            assertEquals(new Point(350, 450), managerGui.getLocation());
        }
        finally {
            managerGui.dispose();
        }
    }


    @Test
    public void test_resizeWindow() throws Exception {
        TaskManagerGui managerGui = new TaskManagerGui(new TaskManagerConfiguration(),
                                                       new DefaultListModel(),
                                                       new DefaultListModel(), new DefaultListModel());
        managerGui.setVisible(true);
        try {
            int startWidth = managerGui.getWidth();
            int startHeight = managerGui.getHeight();

            dragComponent(managerGui, 10, startHeight - 1, 100, 50 + 1);

            assertEquals(startWidth, managerGui.getWidth());
            assertEquals(startHeight + 50, managerGui.getHeight());
        }
        finally {
            managerGui.dispose();
        }
    }


    @Test
    public void test_hideWindow() throws Exception {
        TaskManagerGui managerGui = new TaskManagerGui(new TaskManagerConfiguration(),
                                                       new DefaultListModel(),
                                                       new DefaultListModel(), new DefaultListModel());
        managerGui.setVisible(true);
        try {
            assertTrue(managerGui.isVisible());

            int startWidth = managerGui.getWidth();

            Point point = managerGui.getLocation();
            point.translate(startWidth - 10, 10);
            robot.mouseMove(point.x, point.y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);

            assertFalse(managerGui.isVisible());
        }
        finally {
            managerGui.dispose();
        }
    }


    private void dragComponent(Component awtComponent, int x, int y, int dx, int dy) throws AWTException {
        Point point = awtComponent.getLocation();
        point.translate(x, y);
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);

        point.translate(dx, dy);
        robot.mouseMove(point.x, point.y);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }
}
