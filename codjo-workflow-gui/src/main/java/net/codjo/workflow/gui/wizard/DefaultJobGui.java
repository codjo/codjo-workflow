/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import net.codjo.gui.toolkit.GradientPanel;
import net.codjo.gui.toolkit.ShadowBorder;
import net.codjo.gui.toolkit.util.ErrorDialog;
import net.codjo.workflow.common.message.JobAudit;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SpringLayout;
/**
 * Composant graphique représentant un Job dans le wizard.
 *
 * @see FinalStep
 */
public class DefaultJobGui extends GradientPanel implements FinalStep.JobGui {
    private static final String UNSTARTED_ICON = "unstarted.png";
    static final Color ERROR_COLOR = new Color(255, 100, 100);
    static final Color WARNING_COLOR = new Color(255, 180, 0);
    static final String DEFAULT_PROGRESS_MESSAGE = "Traitement en cours...";
    static final String FINISHED_PROGRESS_MESSAGE = "Traitement terminé.";
    private JLabel statusIcon = new JLabel();
    private JLabel progressLabel = new JLabel(DEFAULT_PROGRESS_MESSAGE, JLabel.LEFT);
    private JProgressBar progressBar = new JProgressBar();
    private JobAudit jobResult;
    private final String title;


    public DefaultJobGui(String title) {
        super(new SpringLayout());
        this.title = title;
        setStatusIcon(UNSTARTED_ICON);
        buildGui();
    }


    public JComponent getGui() {
        return this;
    }


    public void displayStart() {
        progressLabel.setVisible(true);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        setStatusIcon("started.png");
        setBackground(getDefaultStartColor());
    }


    public void displayMidAudit(JobAudit audit) {
        progressLabel.setText(audit.toString());
    }


    public void displayStop(JobAudit jobAuditResult) {
        progressBar.setIndeterminate(false);
        progressBar.setValue(progressBar.getMaximum());

        if (jobAuditResult.hasError()) {
            String errorMessage = "<html><u>" + getDisplayErrorMessage(jobAuditResult) + "</u></html>";
            displayJobAudit(jobAuditResult, errorMessage, "error.png", ERROR_COLOR, Color.BLUE);
        }
        else if (JobAudit.Status.WARNING == jobAuditResult.getStatus()) {
            String warningMessage = jobAuditResult.getWarningMessage();
            displayJobAudit(jobAuditResult, warningMessage, "warning.png", WARNING_COLOR, Color.BLUE);
        }
        else {
            displayJobAudit(null, FINISHED_PROGRESS_MESSAGE, "ok.png", getEndColor(), Color.BLACK);
        }
    }


    private void displayJobAudit(JobAudit jobAuditResult, String message, String icon,
                                 Color backgroundColor, Color foregroundColor) {
        progressLabel.setText(message);
        setBackground(backgroundColor);
        setStatusIcon(icon);
        jobResult = jobAuditResult;
        progressLabel.setForeground(foregroundColor);
    }


    private void buildGui() {
        setPreferredSize(new Dimension(350, 90));
        setBackground(getEndColor());

        JLabel label = new AntialiasedJLabel(title);
        label.setName("title");
        Font font = label.getFont().deriveFont(Font.ITALIC, 20.0f);
        label.setFont(font);

        GradientPanel separator = new GradientPanel();
        separator.setPreferredSize(new Dimension(180, 2));
        separator.setBackground(new Color(38, 97, 145));

        JPanel progressPanel = createProgressPanel();

        add(statusIcon);
        add(label);
        add(separator);
        add(progressPanel);

        SpringLayout layout = (SpringLayout)getLayout();
        layout.putConstraint(SpringLayout.WEST, statusIcon, 5, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, statusIcon, 5, SpringLayout.NORTH, this);

        layout.putConstraint(SpringLayout.WEST, label, 5, SpringLayout.EAST, statusIcon);
        layout.putConstraint(SpringLayout.NORTH, label, 5, SpringLayout.NORTH, this);

        layout.putConstraint(SpringLayout.WEST, separator, 5, SpringLayout.EAST, statusIcon);
        layout.putConstraint(SpringLayout.NORTH, separator, 2, SpringLayout.SOUTH, label);

        layout.putConstraint(SpringLayout.NORTH, progressPanel, 10, SpringLayout.SOUTH, label);
        layout.putConstraint(SpringLayout.WEST, progressPanel, 5, SpringLayout.EAST, statusIcon);

        layout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST, progressPanel);

        setBorder(new ShadowBorder());
    }


    private JPanel createProgressPanel() {
        progressLabel.setName("progressLabel");
        progressLabel.setForeground(Color.DARK_GRAY);

        progressLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if (jobResult != null && jobResult.hasError()) {
                    String errorMessage = jobResult.getErrorMessage();
                    if (errorMessage != null) {
                        ErrorDialog.show(DefaultJobGui.this,
                                         "Erreur durant '" + title.toLowerCase() + "'",
                                         jobResult.getErrorMessage(),
                                         jobResult.getError().getDescription());
                    }
                }
            }
        });

        progressLabel.setVisible(false);
        progressBar.setVisible(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.add(progressLabel);
        panel.add(Box.createRigidArea(new Dimension(5, 5)));
        panel.add(progressBar);

        return panel;
    }


    private ImageIcon loadIcon(String name) {
        return new ImageIcon(DefaultJobGui.class.getResource(name));
    }


    private void setStatusIcon(String iconName) {
        statusIcon.setIcon(loadIcon(iconName));
    }


    private String getDisplayErrorMessage(JobAudit jobAuditResult) {
        String errorMessage = jobAuditResult.getErrorMessage();
        if (errorMessage.length() > 60) {
            errorMessage = errorMessage.substring(0, 59) + "...";
        }
        return errorMessage;
    }


    public static void main(String[] args) throws InterruptedException {
        JFrame frame = new JFrame("Test DefaultJobGuiTest");
        DefaultJobGui contentPane = new DefaultJobGui("Import des données");
        frame.setContentPane(contentPane);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });
        contentPane.displayStart();
        Thread.sleep(3000);
        JobAudit jobAudit = new JobAudit();
        jobAudit.setError(new JobAudit.Anomaly("[message] toto; [process] process", new Throwable("eee")));
        contentPane.displayStop(jobAudit);
    }


    // TODO utiliser la version de gui-toolkit lors du prochain passage SNAP
    private static class AntialiasedJLabel extends JLabel {
        AntialiasedJLabel(String text) {
            super(text);
        }


        @Override
        public void paint(Graphics graphics) {
            Graphics2D g2d = (Graphics2D)graphics;
            Object previous = g2d.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);

            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            super.paint(graphics);

            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, previous);
        }
    }
}
