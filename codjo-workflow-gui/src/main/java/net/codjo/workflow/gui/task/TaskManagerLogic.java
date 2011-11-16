package net.codjo.workflow.gui.task;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.Job.State;
import net.codjo.workflow.gui.task.TaskManagerListModel.Filter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
/**
 *
 */
public class TaskManagerLogic {
    private static final ImageIcon CLEAR_ICON =
          new ImageIcon(TaskManagerLogic.class.getResource("/images/cross.png"));
    private static final ImageIcon FILTER_ICON =
          new ImageIcon(TaskManagerLogic.class.getResource("/images/filtre.png"));
    private static final ImageIcon USER_ICON =
          new ImageIcon(TaskManagerLogic.class.getResource("/images/user.png"));
    private static final ImageIcon USERS_ICON =
          new ImageIcon(TaskManagerLogic.class.getResource("/images/users.png"));

    private TaskManagerGui gui;


    public TaskManagerLogic(final TaskManagerConfiguration configuration,
                            final TaskManagerListModel listModel) {

        gui = new TaskManagerGui(configuration, listModel);

        Filter usersFilter = new Filter() {
            public boolean hideRow(Job job) {
                return !configuration.getUserLogin().equals(job.getInitiator());
            }
        };
        gui.addToolBarButton(buildFilterButton(listModel,
                                               "filterInitiator",
                                               "Filtrer les initiateurs",
                                               USER_ICON,
                                               USERS_ICON,
                                               usersFilter));

        final Filter doneFilter = new Filter() {
            public boolean hideRow(Job job) {
                State state = job.getState();
                return State.DONE.equals(state);
            }
        };
        gui.addToolBarButton(buildFilterButton(listModel,
                                               "filterDone",
                                               "Filtrer les tâches terminées",
                                               FILTER_ICON,
                                               FILTER_ICON,
                                               doneFilter));

        JButton clearButton = new JButton(CLEAR_ICON);
        clearButton.setName("clearDone");
        clearButton.setToolTipText("Nettoyer les tâches terminés");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                listModel.clearDone();
            }
        });
        gui.addToolBarButton(clearButton);
    }


    private JToggleButton buildFilterButton(final TaskManagerListModel listModel,
                                            String name,
                                            String toolTip,
                                            ImageIcon icon, ImageIcon selectedIcon,
                                            final Filter filter1) {
        final JToggleButton filterUsersButon = new JToggleButton();
        filterUsersButon.setName(name);
        filterUsersButon.setIcon(icon);
        filterUsersButon.setSelectedIcon(selectedIcon);
        filterUsersButon.setToolTipText(toolTip);
        filterUsersButon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (filterUsersButon.isSelected()) {
                    listModel.addFilter(filter1);
                }
                else {
                    listModel.removeFilter(filter1);
                }
            }
        });
        return filterUsersButon;
    }


    public TaskManagerGui getGui() {
        return gui;
    }
}
