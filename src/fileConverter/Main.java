package fileConverter;

import javax.swing.*;
import java.io.File;

public class Main {
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
            } catch (Exception ignored) {}
            MainWindow window = new MainWindow();
            if (fileToOpen != null) {
                window.open(fileToOpen);
            }
            window.setVisible(true);
        });
    }
}
