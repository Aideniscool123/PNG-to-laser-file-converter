package fileConverter.util;

import java.util.Locale;

public final class Fmt {
    private Fmt() {
    }

    public static String num(double value) {
        String s = String.format(Locale.ROOT, "%.3f", value);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s.equals("-0") ? "0" : s;
    }

    public static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }
}