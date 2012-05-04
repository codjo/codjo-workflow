package net.codjo.workflow.gui.task;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JButton;
import net.codjo.workflow.gui.WorkflowGuiContext;
import org.uispec4j.Trigger;
import org.uispec4j.UISpecTestCase;
import org.uispec4j.Window;
import org.uispec4j.interception.WindowInterceptor;
/**
 *
 */
public class TaskManagerActionTest extends UISpecTestCase {
    private TaskManagerAction taskManagerAction;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        taskManagerAction = new TaskManagerAction(createTaskManagerConfiguration(), new TaskManagerListModel(10));
    }


    public void test_constructor() throws Exception {
        assertNotNull(taskManagerAction.getValue(Action.SMALL_ICON));
        assertEquals("Gestionnaire de t�ches", taskManagerAction.getValue(Action.SHORT_DESCRIPTION));
    }


    public void test_actionPerformed() throws Exception {
        final JButton button = new JButton("le bouton");

        Window window = WindowInterceptor.run(new Trigger() {
            public void run() throws Exception {
                taskManagerAction.actionPerformed(new ActionEvent(button, 1, null));
            }
        });

        Component taskManagerDialog = window.getAwtComponent();
        assertTrue(taskManagerDialog instanceof TaskManagerGui);

        taskManagerAction.actionPerformed(new ActionEvent(this, 1, null));

        assertFalse(taskManagerDialog.isVisible());

        window = WindowInterceptor.run(new Trigger() {
            public void run() throws Exception {
                taskManagerAction.actionPerformed(new ActionEvent(button, 1, null));
            }
        });

        assertSame(taskManagerDialog, window.getAwtComponent());
    }


    private TaskManagerConfiguration createTaskManagerConfiguration() {
        TaskManagerConfiguration configuration = new TaskManagerConfiguration();
        configuration.setGuiContext(new WorkflowGuiContext());
        return configuration;
    }
}
