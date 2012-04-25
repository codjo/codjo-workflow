package net.codjo.workflow.gui.task;
import java.awt.Point;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import net.codjo.i18n.gui.TranslationNotifier;
import net.codjo.mad.gui.framework.GuiContext;
import net.codjo.mad.gui.i18n.InternationalizationUtil;
/**
 *
 */
public class TaskManagerAction extends AbstractAction {
    private final TaskManagerConfiguration configuration;
    private final TaskManagerListModel listModel;
    private TaskManagerGui taskManagerGui;


    public TaskManagerAction(TaskManagerConfiguration configuration,
                             TaskManagerListModel listModel) {
        this.configuration = configuration;
        this.listModel = listModel;

        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/images/taskManager.gif")));
        putValue(Action.SHORT_DESCRIPTION, "Gestionnaire de tâches");
    }


    public void actionPerformed(ActionEvent e) {
        if (taskManagerGui == null) {
            taskManagerGui = new TaskManagerLogic(configuration, listModel).getGui();
        }
        if (!taskManagerGui.isVisible()) {
            taskManagerGui.setLocation(conputeLocationRelativeTo((JButton)e.getSource()));
        }
        taskManagerGui.setVisible(!taskManagerGui.isVisible());
    }


    private Point conputeLocationRelativeTo(JButton taskManagerButton) {
        Point taskManagerLocation = taskManagerButton.getLocation();
        SwingUtilities.convertPointToScreen(taskManagerLocation, taskManagerButton);

        int locationX = taskManagerLocation.x - taskManagerGui.getWidth() + taskManagerButton.getWidth();
        int locationY = taskManagerLocation.y - taskManagerGui.getHeight() - 5;

        return new Point(locationX, locationY);
    }
}
