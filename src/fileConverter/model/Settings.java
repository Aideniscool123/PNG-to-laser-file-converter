package fileConverter.model;

import java.util.HashSet;
import java.util.Set;

public class Settings {

    public double pixelMm = 5.0;
    public double borderMm = 3.0;
    public int alphaCutoff = 128;
    public CutMode cutMode = CutMode.BOX;
    public boolean mergeRuns = true;
    public boolean trim = true;
    public boolean mirrorX = false;
    public double bedWidthMm = 300;
    public double bedHeightMm = 200;

    public final Set<Integer> excluded = new HashSet<>();

    public Settings copy() {
        Settings c = new Settings();
        c.pixelMm = pixelMm;
        c.borderMm = borderMm;
        c.alphaCutoff = alphaCutoff;
        c.cutMode = cutMode;
        c.mergeRuns = mergeRuns;
        c.trim = trim;
        c.mirrorX = mirrorX;
        c.bedWidthMm = bedWidthMm;
        c.bedHeightMm = bedHeightMm;
        c.excluded.addAll(excluded);
        return c;
    }
}
