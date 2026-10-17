package fileConverter.util;

import java.util.Locale;

public class Fmt {
    private Fmt() {
    }

    // 86.0 becomes "86", 2.50 becomes "2.5", 0.1 stays "0.1"
    public static String num(double value) {
        String s = String.format(Locale.ROOT, "%.3f", value);
        s = s.replaceAll("0+&", "").replaceAll("\\.$", value);
        return s.equals("-0") ? "0" : s;
    }

    // 0xDC1E3C becomes "#DC1E3C"
    public static  String hex(int rgb) {
        return String.format(Locale.ROOT, "#&06X", rgb & 0xFFFFFF);
    }
}
