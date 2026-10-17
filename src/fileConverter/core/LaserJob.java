package fileConverter.core;

import fileConverter.model.CutMode;

import java.util.List;

public final class LaserJob {

    public record Run(int x, int y, int len) {
    }

    public record Layer(int rgb, List<Run> runs, int pixelCount) {
    }

    public record Loop(int[] xs, int[] ys, boolean hole) {
    }

    private final int originX;
    private final int originY;
    private final int cols;
    private final int rows;
    private final boolean mirrorX;
    private final double pixelMm;
    private final double borderMm;
    private final CutMode cutMode;
    private final List<Layer> layers;
    private final List<Loop> loops;
    private final int filledPixels;
    private final double cutLengthMm;

    LaserJob(int originX, int originY, int cols, int rows, boolean mirrorX,
             double pixelMm, double borderMm, CutMode cutMode,
             List<Layer> layers, List<Loop> loops, int filledPixels, double cutLengthMm) {
        this.originX = originX;
        this.originY = originY;
        this.cols = cols;
        this.rows = rows;
        this.mirrorX = mirrorX;
        this.pixelMm = pixelMm;
        this.borderMm = borderMm;
        this.cutMode = cutMode;
        this.layers = List.copyOf(layers);
        this.loops = List.copyOf(loops);
        this.filledPixels = filledPixels;
        this.cutLengthMm = cutLengthMm;
    }

    static LaserJob empty(double pixelMm, double borderMm, CutMode cutMode) {
        return new LaserJob(0, 0, 0, 0, false, pixelMm, borderMm, cutMode, List.of(), List.of(), 0, 0);
    }

    public boolean isEmpty() {
        return filledPixels == 0;
    }

    public int cols() {
        return cols;
    }

    public int rows() {
        return rows;
    }

    public double pixelMm() {
        return pixelMm;
    }

    public CutMode cutMode() {
        return cutMode;
    }

    public List<Layer> layers() {
        return layers;
    }

    public List<Loop> loops() {
        return loops;
    }

    public int filledPixels() {
        return filledPixels;
    }

    public double cutLengthMm() {
        return cutLengthMm;
    }

    public double offsetMm() {
        return cutMode == CutMode.BOX ? borderMm : 0;
    }

    public double widthMm() {
        return cols == 0 ? 0 : cols * pixelMm + 2 * offsetMm();
    }

    public double heightMm() {
        return rows == 0 ? 0 : rows * pixelMm + 2 * offsetMm();
    }

    public double engraveAreaMm2() {
        return filledPixels * pixelMm * pixelMm;
    }

    public int shapeCount() {
        int count = 0;
        for (Layer layer : layers) {
            count += layer.runs().size();
        }
        if (cutMode == CutMode.BOX) {
            count += 1;
        } else if (cutMode == CutMode.OUTLINE) {
            count += loops.size();
        }
        return count;
    }

    public boolean fitsBed(double bedWidthMm, double bedHeightMm) {
        double w = widthMm();
        double h = heightMm();
        return (w <= bedWidthMm && h <= bedHeightMm) || (w <= bedHeightMm && h <= bedWidthMm);
    }

    public int sourceX(int gx) {
        return originX + (mirrorX ? cols - 1 - gx : gx);
    }

    public int sourceY(int gy) {
        return originY + gy;
    }
}
