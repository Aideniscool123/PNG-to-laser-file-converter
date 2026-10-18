package fileConverter.ui;

import fileConverter.core.ColorStats;
import fileConverter.core.ImageLoader;
import fileConverter.core.JobBuilder;
import fileConverter.core.LaserJob;
import fileConverter.core.SvgWriter;
import fileConverter.model.PixelImage;
import fileConverter.model.Settings;
import fileConverter.util.Fmt;
import fileConverter.util.Prefs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;


@SuppressWarnings("serial")
public final class MainWindow extends JFrame implements SettingsPanel.Actions {

    private final Settings settings = Prefs.load();
    private final PreviewPanel preview = new PreviewPanel();
    private final SettingsPanel settingsPanel;
    private final JLabel status = new JLabel(" ");
    private final JLabel hover = new JLabel(" ");
    private final JButton reloadButton = new JButton("Reload");
    private final JButton exportButton = new JButton("Export SVG...");

    private PixelImage image;
    private File file;
    private LaserJob job;
    private List<ColorStats.ColorInfo> colors = List.of();
    private int colorsScannedAtAlpha = -1;

    public MainWindow() {
        super("PixelLaser");
        settingsPanel = new SettingsPanel(settings, this);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Prefs.save(settings);
            }
        });

        JButton openButton = new JButton("Open PNG...");
        openButton.addActionListener(e -> chooseAndOpen());
        reloadButton.setToolTipText("Read the file again, for when you edited it in your pixel-art program (F5)");
        reloadButton.addActionListener(e -> reload());
        exportButton.addActionListener(e -> export());

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(openButton);
        toolbar.add(reloadButton);
        toolbar.addSeparator();
        toolbar.add(exportButton);

        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 2, 8));
        hover.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        hover.setForeground(Color.GRAY);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(status, BorderLayout.NORTH);
        bottom.add(hover, BorderLayout.SOUTH);

        preview.setHoverListener(text -> hover.setText(text.isEmpty() ? " " : text));
        preview.setColorClickListener(this::switchColorOff);

        add(toolbar, BorderLayout.NORTH);
        add(preview, BorderLayout.CENTER);
        add(settingsPanel, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        installDropTarget();
        installShortcuts();
        rebuild();
        pack();
        setLocationRelativeTo(null);
    }

    public void open(File chosen) {
        try {
            image = ImageLoader.load(chosen);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could not open image", JOptionPane.ERROR_MESSAGE);
            return;
        }
        file = chosen;
        settings.excluded.clear();
        colorsScannedAtAlpha = -1;
        Prefs.setLastDir(chosen.getParentFile());
        setTitle("PixelLaser - " + chosen.getName());
        rebuild();
    }

    private void rebuild() {
        if (image != null && colorsScannedAtAlpha != settings.alphaCutoff) {
            colors = ColorStats.scan(image, settings.alphaCutoff);
            colorsScannedAtAlpha = settings.alphaCutoff;
            settingsPanel.setColors(colors);
        }
        job = image == null ? null : JobBuilder.build(image, settings);
        preview.setModel(image, settings, job);
        exportButton.setEnabled(job != null && !job.isEmpty());
        reloadButton.setEnabled(file != null);
        updateStatus();
    }

    private void updateStatus() {
        status.setForeground(Color.DARK_GRAY);
        if (image == null) {
            status.setText("Open a PNG to begin (or drop one on this window).");
            return;
        }
        if (job.isEmpty()) {
            status.setText("Every color is switched off, so there is nothing to export.");
            return;
        }
        boolean fits = job.fitsBed(settings.bedWidthMm, settings.bedHeightMm);
        StringBuilder text = new StringBuilder(String.format(Locale.ROOT,
                "%s x %s mm  |  %s  |  %d pixels in %d colors  |  engrave %s mm2  |  cut %s mm  |  %d shapes",
                Fmt.num(job.widthMm()), Fmt.num(job.heightMm()),
                fits ? "fits your bed" : "TOO BIG for your " + Fmt.num(settings.bedWidthMm) + " x "
                        + Fmt.num(settings.bedHeightMm) + " mm bed",
                job.filledPixels(), job.layers().size(),
                Fmt.num(Math.round(job.engraveAreaMm2())), Fmt.num(Math.round(job.cutLengthMm())),
                job.shapeCount()));
        if (colors.size() > 32) {
            text.append("  |  lots of colors: pixel art works best");
        }
        status.setText(text.toString());
        if (!fits) {
            status.setForeground(new Color(0xB00020));
        }
    }

    @Override
    public void changed() {
        rebuild();
    }

    @Override
    public void ignoreCorner() {
        if (image == null) {
            return;
        }
        OptionalInt corner = ColorStats.cornerColor(image, settings.alphaCutoff);
        if (corner.isEmpty()) {
            status.setText("The top-left pixel is already transparent, so there is no background color to ignore.");
            return;
        }
        settings.excluded.add(corner.getAsInt());
        settingsPanel.refreshColors();
        rebuild();
    }

    @Override
    public void allColorsOn() {
        settings.excluded.clear();
        settingsPanel.refreshColors();
        rebuild();
    }

    @Override
    public void gridChanged(boolean showGrid) {
        preview.setShowGrid(showGrid);
    }

    private void switchColorOff(int rgb) {
        settings.excluded.add(rgb);
        settingsPanel.refreshColors();
        rebuild();
    }

    private void chooseAndOpen() {
        JFileChooser chooser = new JFileChooser(Prefs.lastDir());
        chooser.setFileFilter(new FileNameExtensionFilter("Images (png, gif, bmp, jpg)", "png", "gif", "bmp", "jpg", "jpeg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            open(chooser.getSelectedFile());
        }
    }

    private void reload() {
        if (file == null) {
            return;
        }
        try {
            image = ImageLoader.load(file); // keeps your settings and switched-off colors
            colorsScannedAtAlpha = -1;
            rebuild();
            status.setText("Reloaded " + file.getName() + ".  " + status.getText());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could not reload image", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void export() {
        if (job == null || job.isEmpty()) {
            return;
        }
        String baseName = file.getName().replaceFirst("\\.[^.]*$", "");
        JFileChooser chooser = new JFileChooser(file.getParentFile());
        chooser.setSelectedFile(new File(file.getParentFile(), baseName + ".svg"));
        chooser.setFileFilter(new FileNameExtensionFilter("SVG files", "svg"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File out = chooser.getSelectedFile();
        if (!out.getName().toLowerCase(Locale.ROOT).endsWith(".svg")) {
            out = new File(out.getParentFile(), out.getName() + ".svg");
        }
        if (out.exists() && JOptionPane.showConfirmDialog(this, out.getName() + " already exists. Replace it?",
                "Replace file?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            SvgWriter.write(job, out, image.getName());
            Prefs.setLastDir(out.getParentFile());
            status.setText("Saved " + out.getName() + " (" + Fmt.num(job.widthMm()) + " x " + Fmt.num(job.heightMm())
                    + " mm). In your laser software: set red to cut, and each color to engrave.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save the file:\n" + e.getMessage(),
                    "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void installDropTarget() {
        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    Transferable t = support.getTransferable();
                    List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        open(files.get(0));
                        return true;
                    }
                } catch (
                        Exception e) {
                    status.setText("Could not read the dropped file: " + e.getMessage());
                }
                return false;
            }
        });
    }

    private void installShortcuts() {
        int menuKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getRootPane().registerKeyboardAction(e -> chooseAndOpen(),
                KeyStroke.getKeyStroke(KeyEvent.VK_O, menuKey), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> export(),
                KeyStroke.getKeyStroke(KeyEvent.VK_S, menuKey), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> reload(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
}