package fileConverter.util;

import fileConverter.model.CutMode;
import fileConverter.model.Settings;
import org.w3c.dom.Node;

import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Prefs {
    private static final Preferences NODE = Preferences.userNodeForPackage(Prefs.class);

    private Prefs() {
    }

    public static Settings load() {
        Settings s = new Settings();
        s.pixelMm = NODE.getDouble("pixelMm", s.pixelMm);
        s.borderMm = NODE.getDouble("borderMm", s.borderMm);
        s.alphaCutoff = NODE.getInt("alphaCutoff", s.alphaCutoff);
        s.cutMode = CutMode.parse(NODE.get("cutMode", s.cutMode.shortName()));
        s.mergeRuns = NODE.getBoolean("mergeRuns", s.mergeRuns);
        s.trim = NODE.getBoolean("trim", s.trim);
        s.mirrorX = NODE.getBoolean("mirrorX", s.mirrorX);
        s.bedWidthMm = NODE.getDouble("bedWitchMm", s.bedWidthMm);
        s.bedHeightMm = NODE.getDouble("bedHeightMm", s.bedHeightMm);
        return s;
    }

   public static void save(Settings s) {
       NODE.putDouble("pixelMm", s.pixelMm);
       NODE.putDouble("borderMm", s.borderMm);
       NODE.putInt("alphaCutoff", s.alphaCutoff);
       NODE.put("cutMode", s.cutMode.shortName());
       NODE.putBoolean("mergeRuns", s.mergeRuns);
       NODE.putBoolean("trim", s.trim);
       NODE.putBoolean("mirrorX", s.mirrorX);
       NODE.putDouble("bedWidthMm", s.bedWidthMm);
       NODE.putDouble("bedHeightMm", s.bedHeightMm);
       flush();
   }

       public static File lastDir() {
           String saved = NODE.get("lastDir", null);
           if (saved != null && new File(saved).isDirectory()) {
               return new File(saved);
           }
           return new File(System.getProperty("user.dir"));
       }

       public static void setLastDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            NODE.put("lastDir", dir.getAbsolutePath());
            flush();
        }
    }

    private static void flush() {
        try {
            NODE.flush();
        } catch (BackingStoreException ignored) {
        }
    }

}
