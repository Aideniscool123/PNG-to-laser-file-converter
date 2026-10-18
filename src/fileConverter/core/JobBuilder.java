package fileConverter.core;

import fileConverter.model.CutMode;
import fileConverter.model.PixelImage;
import fileConverter.model.Settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class JobBuilder {

    private static final int EMPTY = -1;

    private JobBuilder() {
    }

    public static LaserJob build(PixelImage image, Settings s) {
        int w = image.getWidth();
        int h = image.getHeight();


        boolean[][] keep = new boolean[h][w];
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (image.visible(x, y, s.alphaCutoff) && !s.excluded.contains(image.rgb(x, y))) {
                    keep[y][x] = true;
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return LaserJob.empty(s.pixelMm, s.borderMm, s.cutMode);
        }

        int originX = s.trim ? minX : 0;
        int originY = s.trim ? minY : 0;
        int cols = s.trim ? maxX - minX + 1 : w;
        int rows = s.trim ? maxY - minY + 1 : h;
        int[][] grid = new int[rows][cols];
        boolean[][] cells = new boolean[rows][cols];
        for (int gy = 0; gy < rows; gy++) {
            for (int gx = 0; gx < cols; gx++) {
                int sx = s.mirrorX ? originX + (cols - 1 - gx) : originX + gx;
                int sy = originY + gy;
                if (keep[sy][sx]) {
                    grid[gy][gx] = image.rgb(sx, sy);
                    cells[gy][gx] = true;
                } else {
                    grid[gy][gx] = EMPTY;
                }
            }
        }

        Map<Integer, List<LaserJob.Run>> runsByColor = new LinkedHashMap<>();
        Map<Integer, Integer> pixelsByColor = new LinkedHashMap<>();
        int filledPixels = 0;
        for (int gy = 0; gy < rows; gy++) {
            int gx = 0;
            while (gx < cols) {
                int color = grid[gy][gx];
                if (color == EMPTY) {
                    gx++;
                    continue;
                }
                int start = gx;
                gx++;
                if (s.mergeRuns) {
                    while (gx < cols && grid[gy][gx] == color) {
                        gx++;
                    }
                }
                int len = gx - start;
                runsByColor.computeIfAbsent(color, c -> new ArrayList<>()).add(new LaserJob.Run(start, gy, len));
                pixelsByColor.merge(color, len, Integer::sum);
                filledPixels += len;
            }
        }
        List<LaserJob.Layer> layers = new ArrayList<>();
        for (Map.Entry<Integer, List<LaserJob.Run>> entry : runsByColor.entrySet()) {
            layers.add(new LaserJob.Layer(entry.getKey(), entry.getValue(), pixelsByColor.get(entry.getKey())));
        }

        List<LaserJob.Loop> loops = List.of();
        double cutLengthMm = 0;
        if (s.cutMode == CutMode.OUTLINE) {
            OutlineTracer.Result traced = OutlineTracer.trace(cells);
            loops = traced.loops();
            cutLengthMm = traced.edgeCount() * s.pixelMm;
        } else if (s.cutMode == CutMode.BOX) {
            double boxW = cols * s.pixelMm + 2 * s.borderMm;
            double boxH = rows * s.pixelMm + 2 * s.borderMm;
            cutLengthMm = 2 * (boxW + boxH);
        }

        return new LaserJob(originX, originY, cols, rows, s.mirrorX, s.pixelMm, s.borderMm,
                s.cutMode, layers, loops, filledPixels, cutLengthMm);
    }
}
