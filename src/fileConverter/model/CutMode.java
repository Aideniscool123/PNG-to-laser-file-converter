package fileConverter.model;

import java.util.Locale;

public enum CutMode {
    NONE(label: "No Cut Line"),
    BOX(label: "Box around the art"),
    OUTLINE(label: "Follow the art's outline");

    private final String label;

    CutMode(String label) {
        this.label = label;
    }

    public String toString() {
        return label;
    }

    public String shortName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CutMode parse(String text) {
        if (text != null) {
            for (CutMode mode : values()) {
                if (mode.shortName().equals(text.trim().toLowerCase(Locale.ROOT))) {
                    return mode;
                }
            }
        }
    }
}
