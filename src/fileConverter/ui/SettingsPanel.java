package fileConverter.ui;

import fileConverter.core.ColorStats.ColorInfo;
import fileConverter.model.CutMode;
import fileConverter.model.Settings;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;


@SuppressWarnings("serial")
final class SettingsPanel extends JPanel {

    interface Actions {
        void changed();

        void ignoreCorner();

        void allColorsOn();

        void gridChanged(boolean showGrid);
    }

    private final Settings settings;
    private final Actions actions;
    private boolean updating;

    private final JSpinner pixelMm = spinner(5, 0.5, 50, 0.5);
    private final JComboBox<CutMode> cutMode = new JComboBox<>(CutMode.values());
    private final JSpinner borderMm = spinner(3, 0, 50, 0.5);
    private final JCheckBox mergeRuns = new JCheckBox("Join pixels in a row");
    private final JCheckBox trim = new JCheckBox("Trim empty space");
    private final JCheckBox mirror = new JCheckBox("Mirror left to right");
    private final JSpinner alpha = integerSpinner(128, 1, 255);
    private final JSpinner bedW = spinner(300, 50, 3000, 10);
    private final JSpinner bedH = spinner(200, 50, 3000, 10);
    private final JCheckBox grid = new JCheckBox("Show pixel grid", true);
    private final ColorTableModel colorModel;

    SettingsPanel(Settings settings, Actions actions) {
        this.settings = settings;
        this.actions = actions;
        this.colorModel = new ColorTableModel(settings.excluded, actions::changed);

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(360, 600));

        JPanel size = section("Size and cut line");
        row(size, 0, "Size of one pixel (mm)", pixelMm);
        row(size, 1, "Cut line", cutMode);
        row(size, 2, "Border for the box (mm)", borderMm);

        JPanel engrave = section("Engraving");
        full(engrave, 0, mergeRuns);
        full(engrave, 1, trim);
        full(engrave, 2, mirror);
        row(engrave, 3, "Transparency cutoff", alpha);

        JPanel bed = section("Laser bed size");
        row(bed, 0, "Width (mm)", bedW);
        row(bed, 1, "Height (mm)", bedH);

        JPanel view = section("View");
        full(view, 0, grid);

        addSection(size, 0, 0);
        addSection(engrave, 1, 0);
        addSection(bed, 2, 0);
        addSection(view, 3, 0);
        addSection(colorsSection(), 4, 1);

        pixelMm.addChangeListener(e -> edited());
        borderMm.addChangeListener(e -> edited());
        alpha.addChangeListener(e -> edited());
        bedW.addChangeListener(e -> edited());
        bedH.addChangeListener(e -> edited());
        cutMode.addActionListener(e -> edited());
        mergeRuns.addActionListener(e -> edited());
        trim.addActionListener(e -> edited());
        mirror.addActionListener(e -> edited());
        grid.addActionListener(e -> actions.gridChanged(grid.isSelected()));

        mergeRuns.setToolTipText("Fewer, wider rectangles: a smaller, faster file with no hairline gaps between pixels.");
        trim.setToolTipText("Cuts the file down to the smallest box that holds the art.");
        mirror.setToolTipText("Flips the piece left to right, for engraving the back or for software that mirrors.");
        alpha.setToolTipText("Pixels less opaque than this count as empty. 128 is half transparent.");
        bedW.setToolTipText("Only used to warn you when the piece is too big for your laser.");
        bedH.setToolTipText("Only used to warn you when the piece is too big for your laser.");
        cutMode.setToolTipText("The red line your laser should cut. Outline follows the pixels and also cuts out holes.");

        showSettings();
    }

    void showSettings() {
        updating = true;
        pixelMm.setValue(settings.pixelMm);
        cutMode.setSelectedItem(settings.cutMode);
        borderMm.setValue(settings.borderMm);
        mergeRuns.setSelected(settings.mergeRuns);
        trim.setSelected(settings.trim);
        mirror.setSelected(settings.mirrorX);
        alpha.setValue(settings.alphaCutoff);
        bedW.setValue(settings.bedWidthMm);
        bedH.setValue(settings.bedHeightMm);
        borderMm.setEnabled(settings.cutMode == CutMode.BOX);
        updating = false;
    }

    void setColors(List<ColorInfo> colors) {
        colorModel.setColors(colors);
    }

    void refreshColors() {
        colorModel.refresh();
    }

    boolean isGridShown() {
        return grid.isSelected();
    }

    private void edited() {
        if (updating) {
            return;
        }
        settings.pixelMm = number(pixelMm);
        settings.cutMode = (CutMode) cutMode.getSelectedItem();
        settings.borderMm = number(borderMm);
        settings.mergeRuns = mergeRuns.isSelected();
        settings.trim = trim.isSelected();
        settings.mirrorX = mirror.isSelected();
        settings.alphaCutoff = ((Number) alpha.getValue()).intValue();
        settings.bedWidthMm = number(bedW);
        settings.bedHeightMm = number(bedH);
        borderMm.setEnabled(settings.cutMode == CutMode.BOX);
        actions.changed();
    }

    private JPanel colorsSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Colors (untick to leave a color out)"));

        JTable table = new JTable(colorModel);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setCellRenderer(new SwatchRenderer());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(300, 150));

        JButton corner = new JButton("Ignore corner color");
        corner.setToolTipText("Switches off the color of the top-left pixel, which is usually the background.");
        corner.addActionListener(e -> actions.ignoreCorner());
        JButton allOn = new JButton("All on");
        allOn.addActionListener(e -> actions.allColorsOn());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(corner);
        buttons.add(allOn);

        JLabel tip = new JLabel("<html><small>Tip: click a pixel in the preview to switch its color off.</small></html>");
        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.add(buttons, BorderLayout.NORTH);
        south.add(tip, BorderLayout.SOUTH);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel section(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private void addSection(JPanel section, int y, double weightY) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y;
        c.weightx = 1;
        c.weighty = weightY;
        c.fill = weightY > 0 ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 6, 0);
        add(section, c);
    }

    private static void row(JPanel panel, int y, String label, JComponent control) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.insets = new Insets(2, 6, 2, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(control, c);
    }

    private static void full(JPanel panel, int y, JComponent control) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(1, 6, 1, 6);
        c.anchor = GridBagConstraints.WEST;
        panel.add(control, c);
    }

    private static JSpinner spinner(double value, double min, double max, double step) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setColumns(5);
        return s;
    }

    private static JSpinner integerSpinner(int value, int min, int max) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setColumns(5);
        return s;
    }

    private static double number(JSpinner spinner) {
        return ((Number) spinner.getValue()).doubleValue();
    }

    @SuppressWarnings("serial")
    private static final class SwatchRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, "", false, false, row, column);
            setBackground((Color) value);
            setBorder(BorderFactory.createMatteBorder(2, 4, 2, 4, table.getBackground()));
            return this;
        }
    }
}
