package fileConverter.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class OutlineTracer {

    private OutlineTracer() {
    }

    public record Result(List<fileConverter.core.LaserJob.Loop> loops, int edgeCount) {
    }

    private static final int[] DX = {1, 0, -1, 0};
    private static final int[] DY = {0, 1, 0, -1};

    public static Result trace(boolean[][] filled) {
        int rows = filled.length;
        int cols = rows == 0 ? 0 : filled[0].length;

        List<int[]> edges = new ArrayList<>();
        Map<Integer, List<Integer>> leaving = new HashMap<>();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (!filled[y][x]) {
                    continue;
                }
                if (!isFilled(filled, x, y - 1)) addEdge(edges, leaving, cols, x, y, 0);
                if (!isFilled(filled, x + 1, y)) addEdge(edges, leaving, cols, x + 1, y, 1);
                if (!isFilled(filled, x, y + 1)) addEdge(edges, leaving, cols, x + 1, y + 1, 2);
                if (!isFilled(filled, x - 1, y)) addEdge(edges, leaving, cols, x, y + 1, 3);
            }
        }

        boolean[] used = new boolean[edges.size()];
        List<fileConverter.core.LaserJob.Loop> loops = new ArrayList<>();
        for (int first = 0; first < edges.size(); first++) {
            if (used[first]) {
                continue;
            }
            List<int[]> points = new ArrayList<>();
            int current = first;
            while (true) {
                used[current] = true;
                int[] e = edges.get(current);
                points.add(new int[]{e[0], e[1]});
                int endX = e[0] + DX[e[2]];
                int endY = e[1] + DY[e[2]];
                int next = chooseNext(edges, leaving, used, first, cols, endX, endY, e[2]);
                if (next < 0 || next == first) {
                    break;
                }
                current = next;
            }
            loops.add(toLoop(points));
        }

        loops.sort(Comparator.comparingInt(loop -> loop.hole() ? 0 : 1));
        return new Result(loops, edges.size());
    }

    private static boolean isFilled(boolean[][] filled, int x, int y) {
        return y >= 0 && y < filled.length && x >= 0 && x < filled[y].length && filled[y][x];
    }

    private static int key(int cols, int x, int y) {
        return y * (cols + 1) + x;
    }

    private static void addEdge(List<int[]> edges, Map<Integer, List<Integer>> leaving,
                                int cols, int x, int y, int dir) {
        edges.add(new int[]{x, y, dir});
        leaving.computeIfAbsent(key(cols, x, y), k -> new ArrayList<>()).add(edges.size() - 1);
    }

    private static int chooseNext(List<int[]> edges, Map<Integer, List<Integer>> leaving, boolean[] used,
                                  int first, int cols, int x, int y, int dir) {
        List<Integer> options = leaving.get(key(cols, x, y));
        if (options == null) {
            return -1;
        }
        int[] preference = {1, 0, 3};
        for (int turn : preference) {
            int wanted = (dir + turn) & 3;
            for (int id : options) {
                if (edges.get(id)[2] == wanted && (!used[id] || id == first)) {
                    return id;
                }
            }
        }
        return -1;
    }

    private static fileConverter.core.LaserJob.Loop toLoop(List<int[]> points) {
        int n = points.size();
        List<int[]> kept = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int[] prev = points.get((i + n - 1) % n);
            int[] cur = points.get(i);
            int[] next = points.get((i + 1) % n);
            boolean straight = Integer.signum(cur[0] - prev[0]) == Integer.signum(next[0] - cur[0])
                    && Integer.signum(cur[1] - prev[1]) == Integer.signum(next[1] - cur[1]);
            if (!straight) {
                kept.add(cur);
            }
        }
        int[] xs = new int[kept.size()];
        int[] ys = new int[kept.size()];
        long twiceArea = 0;
        for (int i = 0; i < kept.size(); i++) {
            xs[i] = kept.get(i)[0];
            ys[i] = kept.get(i)[1];
        }
        for (int i = 0; i < xs.length; i++) {
            int j = (i + 1) % xs.length;
            twiceArea += (long) xs[i] * ys[j] - (long) xs[j] * ys[i];
        }
        return new fileConverter.core.LaserJob.Loop(xs, ys, twiceArea < 0);
    }
}
