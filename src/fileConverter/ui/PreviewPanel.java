package fileConverter.ui;

import fileConverter.core.LaserJob;
import fileConverter.model.CutMode;
import fileConverter.model.PixelImage;
import fileConverter.model.Settings;
import fileConverter.util.Fmt;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.function.Consumer;
import java.util.function.IntConsumer;


@SuppressWarnings("serial")
public final class PreviewPanel extends JPanel {
    private static final int MARGIN = 48;
    private static final Color DESK = new Color(0xE6E7EB);
    private static final Color PAPER = new Color(0xFCFBF7);
    private static final Color CUT_RED = new Color(0xE00000);

    private PixelImage image;
    private Settings settings;
    private LaserJob job;
    private boolean showGrid = true;

    private Consumer<String> hoverListener = text -> {
    };
    private IntConsumer colorClickListener = rgb -> {
    };

    private double scale;
    private double paperX;
    private double paperY;

    public PreviewPanel() {
        setBackground(DESK);
        setPreferredSize(new Dimension(640, 560));
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int[] cell = cellAt(e.getX(), e.getY());
                hoverListener.accept(cell == null ? "" : describe(cell));
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int[] cell = cellAt(e.getX(), e.getY());
                if (cell != null) {
                    int sx = job.sourceX(cell[0]);
                    int sy = job.sourceY(cell[1]);
                    if (image.visible(sx, sy, settings.alphaCutoff)) {
                        colorClickListener.accept(image.rgb(sx, sy));
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverListener.accept("");
            }
        });
    }

    void setModel(PixelImage image, Settings settings, LaserJob job) {
        this.image = image;
        this.settings = settings;
        this.job = job;
        repaint();
    }

    void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        repaint();
    }

    void setHoverListener(Consumer<String> listener) {
        this.hoverListener = listener;
    }

    void setColorClickListener(IntConsumer listener) {
        this.colorClickListener = listener;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (image == null) {
                centered(g, "Drop a PNG here, or click Open PNG...");
            } else if (job == null || job.isEmpty()) {
                centered(g, "Nothing to engrave: every color is switched off.");
            } else {
                paintJob(g);
            }
        } finally {
            g.dispose();
        }
    }

    private void paintJob(Graphics2D g) {
        layoutPaper();
        double cell = job.pixelMm() * scale;
        double artX = paperX + job.offsetMm() * scale;
        double artY = paperY + job.offsetMm() * scale;
        int paperW = (int) Math.round(job.widthMm() * scale);
        int paperH = (int) Math.round(job.heightMm() * scale);
        int px = (int) Math.round(paperX);
        int py = (int) Math.round(paperY);

        // Paper with a soft shadow.
        g.setColor(new Color(0, 0, 0, 40));
        g.fillRect(px + 4, py + 4, paperW, paperH);
        g.setColor(PAPER);
        g.fillRect(px, py, paperW, paperH);

        for (int gy = 0; gy < job.rows(); gy++) {
            for (int gx = 0; gx < job.cols(); gx++) {
                int sx = job.sourceX(gx);
                int sy = job.sourceY(gy);
                if (image.visible(sx, sy, settings.alphaCutoff) && settings.excluded.contains(image.rgb(sx, sy))) {
                    Color c = new Color(image.rgb(sx, sy));
                    g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 55));
                    g.fillRect(snap(artX + gx * cell), snap(artY + gy * cell), Math.max(1, snap(cell)), Math.max(1, snap(cell)));
                }
            }
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        for (LaserJob.Layer layer : job.layers()) {
            g.setColor(new Color(layer.rgb()));
            for (LaserJob.Run run : layer.runs()) {
                int x0 = snap(artX + run.x() * cell);
                int x1 = snap(artX + (run.x() + run.len()) * cell);
                int y0 = snap(artY + run.y() * cell);
                int y1 = snap(artY + (run.y() + 1) * cell);
                g.fillRect(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
            }
        }

        if (showGrid && cell >= 7) {
            g.setColor(new Color(0, 0, 0, 30));
            int left = snap(artX);
            int top = snap(artY);
            int right = snap(artX + job.cols() * cell);
            int bottom = snap(artY + job.rows() * cell);
            for (int gx = 0; gx <= job.cols(); gx++) {
                int x = snap(artX + gx * cell);
                g.drawLine(x, top, x, bottom);
            }
            for (int gy = 0; gy <= job.rows(); gy++) {
                int y = snap(artY + gy * cell);
                g.drawLine(left, y, right, y);
            }
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.setColor(CUT_RED);
        if (job.cutMode() == CutMode.BOX) {
            g.drawRect(px, py, paperW, paperH);
        } else if (job.cutMode() == CutMode.OUTLINE) {
            for (LaserJob.Loop loop : job.loops()) {
                Path2D.Double path = new Path2D.Double();
                for (int i = 0; i < loop.xs().length; i++) {
                    double x = artX + loop.xs()[i] * cell;
                    double y = artY + loop.ys()[i] * cell;
                    if (i == 0) {
                        path.moveTo(x, y);
                    } else {
                        path.lineTo(x, y);
                    }
                }
                path.closePath();
                g.draw(path);
            }
        }

        g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        g.setColor(new Color(0x555555));
        String widthText = Fmt.num(job.widthMm()) + " mm";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(widthText, px + (paperW - fm.stringWidth(widthText)) / 2, py - 10);
        String heightText = Fmt.num(job.heightMm()) + " mm";
        Graphics2D rotated = (Graphics2D) g.create();
        rotated.rotate(-Math.PI / 2, px - 12, py + (paperH + fm.stringWidth(heightText)) / 2.0);
        rotated.drawString(heightText, px - 12, (float) (py + (paperH + fm.stringWidth(heightText)) / 2.0));
        rotated.dispose();
    }

    private void centered(Graphics2D g, String text) {
        g.setFont(getFont().deriveFont(Font.PLAIN, 16f));
        g.setColor(new Color(0x777777));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, getHeight() / 2);
    }

    private void layoutPaper() {
        double availW = Math.max(1, getWidth() - 2.0 * MARGIN);
        double availH = Math.max(1, getHeight() - 2.0 * MARGIN);
        double fit = Math.min(availW / job.widthMm(), availH / job.heightMm());
        double cell = job.pixelMm() * fit;
        if (cell >= 2) {
            cell = Math.floor(cell);
        }
        scale = cell / job.pixelMm();
        paperX = (getWidth() - job.widthMm() * scale) / 2;
        paperY = (getHeight() - job.heightMm() * scale) / 2;
    }

    private static int snap(double value) {
        return (int) Math.round(value);
    }

    private int[] cellAt(int mouseX, int mouseY) {
        if (image == null || job == null || job.isEmpty()) {
            return null;
        }
        layoutPaper();
        double mmX = (mouseX - paperX) / scale - job.offsetMm();
        double mmY = (mouseY - paperY) / scale - job.offsetMm();
        int gx = (int) Math.floor(mmX / job.pixelMm());
        int gy = (int) Math.floor(mmY / job.pixelMm());
        if (mmX < 0 || mmY < 0 || gx >= job.cols() || gy >= job.rows()) {
            return null;
        }
        return new int[]{gx, gy};
    }

    private String describe(int[] cell) {
        int sx = job.sourceX(cell[0]);
        int sy = job.sourceY(cell[1]);
        if (!image.visible(sx, sy, settings.alphaCutoff)) {
            return "Pixel " + sx + ", " + sy + ": empty";
        }
        int rgb = image.rgb(sx, sy);
        String state = settings.excluded.contains(rgb) ? " (switched off)" : "";
        return "Pixel " + sx + ", " + sy + ": " + Fmt.hex(rgb) + state;
    }
}
