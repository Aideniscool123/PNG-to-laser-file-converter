package fileConverter;

import fileConverter.core.ColorStats;
import fileConverter.core.ImageLoader;
import fileConverter.core.JobBuilder;
import fileConverter.core.LaserJob;
import fileConverter.core.SvgWriter;
import fileConverter.model.CutMode;
import fileConverter.model.PixelImage;
import fileConverter.model.Settings;
import fileConverter.ui.MainWindow;
import fileConverter.util.Fmt;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.OptionalInt;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            launchWindow(null);
        } else if (args[0].equals("--gui")) {
            launchWindow(args.length > 1 ? new File(args[1]) : null);
        } else {
            System.exit(runCommandLine(args));
        }
    }

    private static void launchWindow(File fileToOpen) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            MainWindow window = new MainWindow();
            if (fileToOpen != null) {
                window.open(fileToOpen);
            }
            window.setVisible(true);
        });
    }


    private static int runCommandLine(String[] args) {
        if (args[0].equals("--help") || args[0].equals("-h")) {
            printHelp();
            return 0;
        }
        File input = new File(args[0]);
        File output = null;
        Settings s = new Settings();
        boolean ignoreCorner = false;
        try {
            for (int i = 1; i < args.length; i++) {
                switch (args[i]) {
                    case "--px" -> s.pixelMm = Double.parseDouble(args[++i]);
                    case "--border" -> s.borderMm = Double.parseDouble(args[++i]);
                    case "--cut" -> {
                        String value = args[++i];
                        if (!value.equalsIgnoreCase("none") && !value.equalsIgnoreCase("box") && !value.equalsIgnoreCase("outline")) {
                            throw new IllegalArgumentException("--cut must be none, box or outline, not " + value);
                        }
                        s.cutMode = CutMode.parse(value);
                    }
                    case "--mirror" -> s.mirrorX = true;
                    case "--no-merge" -> s.mergeRuns = false;
                    case "--no-trim" -> s.trim = false;
                    case "--ignore-corner" -> ignoreCorner = true;
                    case "--ignore" -> s.excluded.add(Integer.parseInt(args[++i].replace("#", ""), 16));
                    case "-o" -> output = new File(args[++i]);
                    default -> throw new IllegalArgumentException("Unknown option " + args[i]);
                }
            }
        } catch (RuntimeException e) {
            System.err.println("Problem with the options: " + e.getMessage() + " (try --help)");
            return 2;
        }

        try {
            PixelImage image = ImageLoader.load(input);
            if (ignoreCorner) {
                OptionalInt corner = ColorStats.cornerColor(image, s.alphaCutoff);
                corner.ifPresent(s.excluded::add);
            }
            LaserJob job = JobBuilder.build(image, s);
            if (job.isEmpty()) {
                System.err.println("Nothing to export: every pixel is empty or switched off.");
                return 1;
            }
            if (output == null) {
                output = new File(input.getParentFile(), input.getName().replaceFirst("\\.[^.]*$", "") + ".svg");
            }
            SvgWriter.write(job, output, image.getName());
            System.out.printf(Locale.ROOT, "Wrote %s%n  %s x %s mm, %d pixels in %d colors, cut length %s mm%n",
                    output.getPath(), Fmt.num(job.widthMm()), Fmt.num(job.heightMm()),
                    job.filledPixels(), job.layers().size(), Fmt.num(Math.round(job.cutLengthMm())));
            return 0;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }

    private static void printHelp() {
        System.out.println("""
                PixelLaser: turn pixel art into a laser-cutter SVG.
                
                Window:        java -cp out pixellaser.Main [--gui [file.png]]
                Command line:  java -cp out pixellaser.Main file.png [options]
                
                Options:
                --px N            millimeters per pixel (default 5)
                --cut MODE        none, box (default) or outline
                --border N        border around the art for box mode, in mm (default 3)
                --mirror          flip left to right
                --no-merge        one rectangle per pixel instead of one per row run
                --no-trim         keep empty space around the art
                --ignore #RRGGBB  leave a color out (repeat for more colors)
                --ignore-corner   leave out the color of the top-left pixel (the background)
                -o FILE.svg       output file (default: next to the PNG)
                """);
    }
}
