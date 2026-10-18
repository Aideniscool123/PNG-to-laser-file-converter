package fileConverter.ui;

import fileConverter.core.ColorStats.ColorInfo;
import fileConverter.util.Fmt;

import javax.swing.table.AbstractTableModel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@SuppressWarnings("serial")
final class ColorTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"Use", "Color", "Hex", "Pixels"};

    private final Set<Integer> excluded;
    private final Runnable onChange;
    private List<ColorInfo> colors = new ArrayList<>();

    ColorTableModel(Set<Integer> excluded, Runnable onChange) {
        this.excluded = excluded;
        this.onChange = onChange;
    }

    void setColors(List<ColorInfo> colors) {
        this.colors = new ArrayList<>(colors);
        fireTableDataChanged();
    }


    void refresh() {
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return colors.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return switch (column) {
            case 0 -> Boolean.class;
            case 1 -> Color.class;
            case 3 -> Integer.class;
            default -> String.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 0;
    }

    @Override
    public Object getValueAt(int row, int column) {
        ColorInfo info = colors.get(row);
        return switch (column) {
            case 0 -> !excluded.contains(info.rgb());
            case 1 -> new Color(info.rgb());
            case 2 -> Fmt.hex(info.rgb());
            default -> info.count();
        };
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (column != 0) {
            return;
        }
        int rgb = colors.get(row).rgb();
        if (Boolean.TRUE.equals(value)) {
            excluded.remove(rgb);
        } else {
            excluded.add(rgb);
        }
        fireTableRowsUpdated(row, row);
        onChange.run();
    }
}
