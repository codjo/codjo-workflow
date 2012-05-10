/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.plugin;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import net.codjo.i18n.gui.InternationalizableContainer;
import net.codjo.i18n.gui.TranslationNotifier;
import net.codjo.mad.gui.framework.GuiContext;
import net.codjo.mad.gui.i18n.InternationalizationUtil;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobRequest;
/**
 * IHM de la Console.
 */
class ConsoleGui extends JInternalFrame implements InternationalizableContainer {
    private JTree requestTree;
    private JTable auditTable;
    private JButton closeButton;
    private JTextArea requestDetail;
    private JComponent content;
    private JTextArea errorArea;
    private JPanel requestPanel;
    private JPanel detailRequestPanel;
    private JPanel auditMessagePanel;
    private JPanel internalErrorsPanel;
    private RequestDataTreeModel dataTreeModel;
    private SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
    private AuditTableModel auditTableModel;


    ConsoleGui() {
        super("Console du serveur", true, true, true, true);
        setContentPane(content);
        requestDetail.setName("requestDetail");
        auditTable.setName("auditTable");
        requestTree.setName("requestTree");
        closeButton.setName("closeButton");
        errorArea.setName("errorArea");

        requestTree.setRootVisible(false);
        dataTreeModel = new RequestDataTreeModel(new RequestData());
        requestTree.setModel(dataTreeModel);
        requestTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent event) {
                displayRequest((RequestData)event.getPath().getLastPathComponent());
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        auditTableModel = new AuditTableModel(new RequestData());
        auditTable.setModel(auditTableModel);
        auditTable.setDefaultRenderer(String.class, new AuditCellRenderer());
        auditTable.setDefaultRenderer(Date.class, new AuditCellRenderer());
    }


    public void init(GuiContext guiContext) {
        TranslationNotifier translationNotifier = InternationalizationUtil.retrieveTranslationNotifier(guiContext);
        translationNotifier.addInternationalizableContainer(this);
    }


    public void addInternationalizableComponents(TranslationNotifier translationNotifier) {
        translationNotifier.addInternationalizableComponent(this, "ConsoleGui.title");
        translationNotifier.addInternationalizableComponent(closeButton, "ConsoleGui.closeButton", null);

        translationNotifier.addInternationalizableComponent(requestPanel, "ConsoleGui.requestPanel.title");
        translationNotifier.addInternationalizableComponent(detailRequestPanel, "ConsoleGui.detailRequestPanel.title");
        translationNotifier.addInternationalizableComponent(auditMessagePanel, "ConsoleGui.auditMessagePanel.title");
        translationNotifier.addInternationalizableComponent(internalErrorsPanel,
                                                            "ConsoleGui.internalErrorsPanel.title");

        translationNotifier.addInternationalizableComponent(auditTable, null, new String[]{
              "ConsoleGui.auditTable.type",
              "ConsoleGui.auditTable.date",
              "ConsoleGui.auditTable.argument",
              "ConsoleGui.auditTable.error"
        });
    }


    public JComponent getContent() {
        return content;
    }


    public void receiveRequest(JobRequest request) {
        RequestData requestData = new RequestData(request);

        dataTreeModel.getRootData().add(requestData);
        TreePath treePath = dataTreeModel.getRootData().getParentTreePath(requestData);
        dataTreeModel.fireTreeStructureChanged(dataTreeModel.getRootData());

        requestTree.expandPath(treePath);
    }


    public void selectRequest(String requestId) {
        TreePath treePath = dataTreeModel.getRootData().getCurrentTreePath(requestId);
        requestTree.setSelectionPath(treePath);
    }


    private void displayRequest(RequestData requestData) {
        JobRequest request = requestData.request;
        if (request == null) {
            return;
        }
        StringBuilder detail = new StringBuilder();

        detail.append("Type = ").append(request.getType()).append("\n");
        detail.append("Date = ").append(format.format(request.getDate())).append("\n");
        detail.append("Initiateur = ").append(request.getInitiatorLogin()).append("\n");
        if (request.getArguments() != null) {
            detail.append("Arguments :").append("\n");
            detail.append(request.getArguments().encode());
        }
        requestDetail.setText(detail.toString());

        auditTableModel.updateData(requestData);
        auditTable.getColumn(AuditTableModel.TYPE).setMaxWidth(50);
        auditTable.getColumn(AuditTableModel.DATE).setMaxWidth(150);
    }


    public void receiveAudit(JobAudit audit) {
        String requestId = audit.getRequestId();

        RequestData requestData = dataTreeModel.getRootData().findRequestData(requestId);
        requestData.addAudit(audit);
    }


    public void receiveError(String failure) {
        errorArea.append(">>>>>\n>>>>>\n>>>>>\n" + failure + "\n");
    }


    /**
     * RequestData.
     */
    private static class RequestData {
        private JobRequest request;
        private List<JobAudit> auditList = new ArrayList<JobAudit>();
        private List<RequestData> childRequestData = new ArrayList<RequestData>();


        RequestData() {
        }


        RequestData(JobRequest request) {
            this.request = request;
        }


        public RequestData getChild(int index) {
            return childRequestData.get(index);
        }


        public int getChildCount() {
            return childRequestData.size();
        }


        public int getIndexOf(RequestData child) {
            for (int i = 0; i < childRequestData.size(); i++) {
                if (childRequestData.get(i) == child) {
                    return i;
                }
            }
            return -1;
        }


        @Override
        public String toString() {
            if (request == null) {
                return "Root";
            }
            return request.getType() + " (" + request.getId() + ")";
        }


        public boolean add(RequestData requestData) {
            if (isMyChild(requestData)) {
                childRequestData.add(requestData);
                return true;
            }

            for (RequestData data : childRequestData) {
                if (data.add(requestData)) {
                    return true;
                }
            }

            if (request == null) {
                childRequestData.add(requestData);
                return true;
            }
            else {
                return false;
            }
        }


        private boolean isMyChild(RequestData requestData) {
            if (request == null) {
                return false;
            }
            if (request.getId() == null) {
                return false;
            }

            String parentId = requestData.request.getParentId();
            return parentId != null && parentId.equals(request.getId());
        }


        public TreePath getParentTreePath(RequestData requestData) {
            return new TreePath(getPath(requestData.request.getId(), 0, true));
        }


        public TreePath getCurrentTreePath(String requestId) {
            return new TreePath(getPath(requestId, 0, false));
        }


        private Object[] getPath(String id, int index, boolean isParentPath) {
            if (this.request != null && this.request.getId().equals(id)) {
                if (isParentPath) {
                    return new Object[index];
                }
                else {
                    Object[] objects = new Object[index + 1];
                    objects[index] = this;
                    return objects;
                }
            }

            for (RequestData data : childRequestData) {
                Object[] path = data.getPath(id, index + 1, isParentPath);
                if (path != null) {
                    path[index] = this;
                    return path;
                }
            }
            return null;
        }


        public RequestData findRequestData(String id) {
            if (this.request != null && this.request.getId().equals(id)) {
                return this;
            }

            for (RequestData data : childRequestData) {
                RequestData requestData = data.findRequestData(id);
                if (requestData != null) {
                    return requestData;
                }
            }
            return null;
        }


        public void addAudit(JobAudit audit) {
            auditList.add(audit);
        }
    }

    /**
     * TreeModel mappant un RequestData.
     */
    private static class RequestDataTreeModel implements TreeModel {
        private RequestData root;
        private EventListenerList listenerList = new EventListenerList();


        RequestDataTreeModel(RequestData root) {
            this.root = root;
        }


        public Object getRoot() {
            return root;
        }


        public RequestData getRootData() {
            return root;
        }


        public Object getChild(Object parent, int index) {
            return ((RequestData)parent).getChild(index);
        }


        public int getChildCount(Object parent) {
            return ((RequestData)parent).getChildCount();
        }


        public boolean isLeaf(Object node) {
            return ((RequestData)node).getChildCount() == 0;
        }


        public void valueForPathChanged(TreePath path, Object newValue) {
            throw new IllegalStateException("Impossible");
        }


        public int getIndexOfChild(Object parent, Object child) {
            return ((RequestData)parent).getIndexOf((RequestData)child);
        }


        public void addTreeModelListener(TreeModelListener listener) {
            listenerList.add(TreeModelListener.class, listener);
        }


        public void removeTreeModelListener(TreeModelListener listener) {
            listenerList.remove(TreeModelListener.class, listener);
        }


        protected void fireTreeStructureChanged(RequestData oldRoot) {
            Object[] listeners = listenerList.getListenerList();
            TreeModelEvent event = new TreeModelEvent(this, new Object[]{oldRoot});

            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                ((TreeModelListener)listeners[i + 1]).treeStructureChanged(event);
            }
        }
    }

    private static class AuditTableModel extends AbstractTableModel {
        static final String TYPE = "Type";
        static final String DATE = "Date";
        private static final String[] COLUMN_NAMES = {TYPE, DATE, "Argument", "Erreur"};
        private static final Class[] COLUMN_CLASS =
              {String.class, Date.class, String.class, String.class};
        private RequestData requestData;


        AuditTableModel(RequestData requestData) {
            this.requestData = requestData;
        }


        public void updateData(RequestData data) {
            this.requestData = data;
        }


        @Override
        public String getColumnName(int column) {
            if (column >= 0 && column < COLUMN_NAMES.length) {
                return COLUMN_NAMES[column];
            }
            else {
                return "Not yet defined";
            }
        }


        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }


        public int getRowCount() {
            return requestData.auditList.size();
        }


        @Override
        public Class getColumnClass(int columnIndex) {
            return COLUMN_CLASS[columnIndex];
        }


        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case -1:
                    return getAudit(rowIndex);
                case 0:
                    return getAudit(rowIndex).getType().toString();
                case 1:
                    return getAudit(rowIndex).getDate();
                case 2:
                    Arguments arguments = getAudit(rowIndex).getArguments();
                    return arguments == null ? "" : arguments.encode();
                case 3:
                    JobAudit audit = getAudit(rowIndex);
                    if (audit.hasError()) {
                        return audit.getErrorMessage();
                    }
                    else {
                        return "";
                    }
                default:
                    return "Not yet defined";
            }
        }


        private JobAudit getAudit(int rowIndex) {
            return requestData.auditList.get(rowIndex);
        }
    }

    private static class AuditCellRenderer extends DefaultTableCellRenderer {
        private Color blackRed = Color.RED.darker();
        private Color lightRed = new Color(255, 100, 100);
        private SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss (dd/MM/yyyy)");


        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            if (!isSelected) {
                super.setBackground(table.getBackground());
                super.setForeground(table.getForeground());
            }

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                                                column);

            JobAudit audit = (JobAudit)table.getValueAt(row, -1);
            if (audit.hasError()) {
                setBackground(isSelected ? blackRed : lightRed);
                if (isSelected) {
                    setForeground(Color.WHITE);
                }
            }

            if (table.getColumnClass(column) == Date.class) {
                setValue(format.format(value));
            }

            return this;
        }
    }
}
