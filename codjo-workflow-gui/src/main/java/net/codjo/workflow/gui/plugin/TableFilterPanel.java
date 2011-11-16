package net.codjo.workflow.gui.plugin;
import net.codjo.gui.toolkit.combo.ComboBoxPopupWidthMaximizer;
import net.codjo.gui.toolkit.util.ErrorDialog;
import net.codjo.mad.client.request.FieldsList;
import net.codjo.mad.client.request.RequestException;
import net.codjo.mad.client.request.Result;
import net.codjo.mad.client.request.Row;
import net.codjo.mad.gui.request.RequestTable;
import net.codjo.mad.gui.request.event.DataSourceAdapter;
import net.codjo.mad.gui.request.event.DataSourceEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class TableFilterPanel extends JPanel {
    public static final String NULL_FOR_MAD = "null";
    public static final String NULL_FOR_COMBO = "";
    private final JPanel filtersPanel = new JPanel();
    private final JButton applyButton = new JButton();
    private final JButton resetButton = new JButton();
    private final Map<String, SortedComboBoxModel> fieldToModel = new HashMap<String, SortedComboBoxModel>();
    private final RequestTable requestTable;
    enum FilterType {
        TEXT,
        DATE;
    }


    public TableFilterPanel(RequestTable requestTable) {
        this.requestTable = requestTable;

        initPanel();
        initApplyButton();
        initResetButton();
        linkTable();
    }


    public void addFilter(String name, String value, FilterType filterType, String... defaultValues) {
        addFilter(name, value, filterType, -1, defaultValues);
    }


    public void addFilter(String name,
                          String value,
                          FilterType filterType,
                          int preferredWidth,
                          String... defaultValues) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;

        constraints.weightx = 0;
        constraints.insets = new Insets(0, 3, 0, 2);
        panel.add(new JLabel(name), constraints);

        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 2, 0, 7);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(buildComboForField(value, filterType, preferredWidth, defaultValues), constraints);

        filtersPanel.add(panel);
    }


    private void initPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        filtersPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        filtersPanel.setLayout(new BoxLayout(filtersPanel, BoxLayout.X_AXIS));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;

        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        add(filtersPanel, constraints);

        add(Box.createHorizontalGlue());

        constraints.insets = new Insets(0, 0, 0, 4);
        constraints.weightx = 0;
        constraints.anchor = GridBagConstraints.EAST;
        add(applyButton, constraints);

        constraints.insets = new Insets(0, 4, 0, 5);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        add(resetButton, constraints);

        setBorder(BorderFactory.createTitledBorder("Filtre"));
    }


    private void initApplyButton() {
        applyButton.setName("apply");
        applyButton.setIcon(new ImageIcon(getClass().getResource("/images/tick.png")));
        applyButton.setMargin(new Insets(2, 0, 2, 2));

        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                apply();
            }
        });
    }


    private void initResetButton() {
        resetButton.setName("reset");
        resetButton.setIcon(new ImageIcon(getClass().getResource("/images/cross.png")));
        resetButton.setMargin(new Insets(2, 0, 2, 2));

        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                reset();
            }
        });
    }


    protected void apply() {
        requestTable.getDataSource().setLoadFactory(requestTable.getPreference().getSelectAll());
        requestTable.setSelector(buildSelector());
        try {
            requestTable.load();
        }
        catch (RequestException e) {
            ErrorDialog.show(this, "Erreur lors du chargement de la table", e);
        }
    }


    private void reset() {
        for (SortedComboBoxModel model : fieldToModel.values()) {
            model.setSelectedItem(NULL_FOR_COMBO);
        }
        apply();
    }


    private JComboBox buildComboForField(String field,
                                         FilterType filterType,
                                         int preferredWidth,
                                         String[] defaultValues) {
        if (fieldToModel.containsKey(field)) {
            throw new RuntimeException("Un filtre a déjà été mis pour le champ " + field);
        }

        SortedComboBoxModel comboBoxModel;
        switch (filterType) {
            case DATE:
                comboBoxModel = new DateSortedComboBoxModel(Arrays.asList(defaultValues));
                break;

            case TEXT:
            default:
                comboBoxModel = new SortedComboBoxModel(Arrays.asList(defaultValues));
        }
        JComboBox comboBox = new JComboBox(comboBoxModel);
        comboBox.setName(field);
        comboBox.setEditable(true);
        ComboBoxPopupWidthMaximizer.install(comboBox, preferredWidth);

        fieldToModel.put(field, comboBoxModel);

        return comboBox;
    }


    private void linkTable() {
        requestTable.getDataSource().addDataSourceListener(new DataSourceAdapter() {
            @Override
            public void loadEvent(DataSourceEvent event) {
                if (!fieldToModel.isEmpty()) {
                    fillComboFromResult(event.getResult());
                }
            }
        });
    }


    private void fillComboFromResult(Result result) {
        for (int i = 0; i < result.getRowCount(); i++) {
            Row row = result.getRow(i);
            for (Entry<String, SortedComboBoxModel> entry : fieldToModel.entrySet()) {
                SortedComboBoxModel comboBoxModel = entry.getValue();
                String value = row.getFieldValue(entry.getKey());
                if (NULL_FOR_MAD.equals(value)) {
                    comboBoxModel.addItem(NULL_FOR_COMBO);
                }
                else {
                    comboBoxModel.addItem(value);
                }
            }
        }
    }


    private FieldsList buildSelector() {
        FieldsList selector = new FieldsList();
        for (String column : requestTable.getDataSource().getColumns()) {
            if (fieldToModel.containsKey(column)) {
                String value = (String)fieldToModel.get(column).getSelectedItem();
                if (NULL_FOR_COMBO.equals(value)) {
                    selector.addField(column, NULL_FOR_MAD);
                }
                else {
                    selector.addField(column, value);
                }
            }
        }
        return selector;
    }


    private static class SortedComboBoxModel extends AbstractListModel
          implements ComboBoxModel, Comparator<String> {
        private final Set<String> items = new TreeSet<String>(this);
        private String selectedItem = NULL_FOR_COMBO;
        private List<String> defaultValues;


        private SortedComboBoxModel(List<String> defaultValues) {
            this.defaultValues = defaultValues;
            addItem(NULL_FOR_COMBO);
            for (String defaultValue : defaultValues) {
                addItem(defaultValue);
            }
        }


        public int getSize() {
            return items.size();
        }


        public Object getElementAt(int index) {
            return items.toArray()[index];
        }


        public Object getSelectedItem() {
            return selectedItem;
        }


        public void setSelectedItem(Object anItem) {
            if (this.selectedItem != null && !this.selectedItem.equals(anItem) ||
                this.selectedItem == null && anItem != null) {
                this.selectedItem = (String)anItem;
                fireContentsChanged(this, -1, -1);
            }
        }


        public int compare(String item1, String item2) {
            if (NULL_FOR_COMBO.equals(item1) && NULL_FOR_COMBO.equals(item2)) {
                return 0;
            }
            if (NULL_FOR_COMBO.equals(item1)) {
                return -1;
            }
            if (NULL_FOR_COMBO.equals(item2)) {
                return 1;
            }
            return compareImpl(item1, item2);
        }


        public void addItem(String item) {
            if (items.add(item)) {
                fireContentsChanged(this, -1, -1);
            }
        }


        protected int compareImpl(String item1, String item2) {
            if (defaultValues.contains(item1) && defaultValues.contains(item2)) {
                return defaultValues.indexOf(item1) - defaultValues.indexOf(item2);
            }
            if (defaultValues.contains(item1)) {
                return -1;
            }
            if (defaultValues.contains(item2)) {
                return 1;
            }

            int compareInUpperCase = item1.toUpperCase().compareTo(item2.toUpperCase());
            if (compareInUpperCase == 0) {
                return item1.compareTo(item2);
            }
            else {
                return compareInUpperCase;
            }
        }
    }

    private static class DateSortedComboBoxModel extends SortedComboBoxModel {
        private DateSortedComboBoxModel(List<String> defaultValues) {
            super(defaultValues);
        }


        @Override
        public int compareImpl(String item1, String item2) {
            return -super.compareImpl(item1, item2);
        }


        @Override
        public void addItem(String item) {
            String newItem = item;
            if (item.length() >= 10) {
                newItem = item.substring(0, 10);
            }
            super.addItem(newItem);
        }
    }
}
