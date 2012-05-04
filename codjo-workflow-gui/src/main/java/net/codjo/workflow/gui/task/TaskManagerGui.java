package net.codjo.workflow.gui.task;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import net.codjo.i18n.gui.TranslationNotifier;
import net.codjo.mad.gui.i18n.InternationalizationUtil;
/**
 *
 */
public class TaskManagerGui extends JDialog {

    private JPanel mainPanel;
    private JPanel jobsPanel;

    private JButton minimiseButton;
    private JPanel titlePanel;
    private JPanel actionPanel;
    private JLabel titleLabel;

    private static final int MIN_HEIGHT = 200;
    private TranslationNotifier notifier;


    public TaskManagerGui(TaskManagerConfiguration configuration, TaskManagerListModel model) {
        this(configuration, model.getWaitingJobs(), model.getRunningJobs(), model.getDoneJobs());
    }


    public TaskManagerGui(TaskManagerConfiguration configuration,
                          ListModel waitingJobsModel,
                          ListModel runningJobsModel,
                          ListModel doneJobsModel) {

        notifier = InternationalizationUtil.retrieveTranslationNotifier(configuration.getGuiContext());
        notifier.addInternationalizableComponent(titleLabel, "TaskManager.title");

        setUndecorated(true);
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());
        setAlwaysOnTop(true);
        setSize(new Dimension(350, 450));

        add(mainPanel);

        titlePanel.setName("title");
        jobsPanel.setName("jobs");

        jobsPanel.setLayout(new BoxLayout(jobsPanel, BoxLayout.Y_AXIS));

        jobsPanel.add(new JobList(configuration,
                                  "running", runningJobsModel,
                                  "JobList.runningTasks",
                                  JobPanel.PAUSED_ICON));
        jobsPanel.add(new JobList(configuration,
                                  "waiting", waitingJobsModel,
                                  "JobList.pendingTasks",
                                  JobPanel.WAITING_ICON));
        jobsPanel.add(new JobList(configuration,
                                  "done", doneJobsModel,
                                  "JobList.finishedTasks",
                                  JobPanel.DONE_ICON));

        DragWindowListener dragWindowListener = new DragWindowListener();
        titlePanel.addMouseMotionListener(dragWindowListener);
        titlePanel.addMouseListener(dragWindowListener);

        ResizeWindowListener resizeWindowListener = new ResizeWindowListener();
        addMouseMotionListener(resizeWindowListener);
        addMouseListener(resizeWindowListener);

        minimiseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
    }


    public void addToolBarButton(AbstractButton button, String key) {
        button.setOpaque(false);
        button.setBorderPainted(false);
        button.setBackground(Color.WHITE);
        button.setFocusable(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        actionPanel.add(button);

        notifier.addInternationalizableComponent(button, null, key);
    }


    private void createUIComponents() {
        jobsPanel = new ScrollablePanel();
    }


    private static class ScrollablePanel extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }


        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }


        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }


        public boolean getScrollableTracksViewportWidth() {
            return true;
        }


        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private class DragWindowListener extends MouseAdapter implements MouseMotionListener {
        private Point startPoint;


        public void mouseDragged(MouseEvent e) {
            Point point = e.getPoint();
            SwingUtilities.convertPointToScreen(point, TaskManagerGui.this);

            point.translate(-startPoint.x, -startPoint.y);
            setLocation(point);
        }


        public void mouseMoved(MouseEvent e) {
        }


        @Override
        public void mousePressed(MouseEvent e) {
            startPoint = e.getPoint();
        }
    }

    private class ResizeWindowListener extends MouseAdapter implements MouseMotionListener {
        private boolean resizing = false;
        private boolean mousePressing = false;


        public void mouseDragged(MouseEvent e) {
            if (resizing && e.getY() > MIN_HEIGHT) {
                setSize(getWidth(), e.getY());
                validate();
            }
        }


        public void mouseMoved(MouseEvent e) {
            boolean isInResizeZone = getHeight() - 5 < e.getPoint().getY();
            if (resizing && !isInResizeZone) {
                setResizing(false);
            }
            else if (!resizing && isInResizeZone) {
                setResizing(true);
            }
        }


        @Override
        public void mousePressed(MouseEvent e) {
            mousePressing = true;
        }


        @Override
        public void mouseReleased(MouseEvent e) {
            mousePressing = false;
            setResizing(false);
        }


        @Override
        public void mouseExited(MouseEvent e) {
            if (!mousePressing) {
                setResizing(false);
            }
        }


        private void setResizing(boolean resizing) {
            boolean oldResizing = this.resizing;
            this.resizing = resizing;
            if (resizing != oldResizing) {
                if (resizing) {
                    setCursor(new Cursor(Cursor.S_RESIZE_CURSOR));
                }
                else {
                    setCursor(null);
                }
            }
        }
    }
}
