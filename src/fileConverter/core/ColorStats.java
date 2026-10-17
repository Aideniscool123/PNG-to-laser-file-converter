package fileConverter.core;

import fileConverter.model.PixelImage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public final class ColorStats {

    public record ColorInfo(int rgb, int count) {
    }

    private ColorStats() {
    }

    public static List<ColorInfo> scan(PixelImage image, int alphaCutoff) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.visible(x, y, alphaCutoff)) {
                    counts.merge(image.rgb(x, y), 1, Integer::sum);
                }
            }
        }
        List<ColorInfo> list = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            list.add(new ColorInfo(e.getKey(), e.getValue()));
        }
        list.sort(Comparator.comparingInt(ColorInfo::count).reversed().thenComparingInt(ColorInfo::rgb));
        return list;
    }

    public static OptionalInt cornerColor(PixelImage image, int alphaCutoff) {
        if (image.visible(0, 0, alphaCutoff)) {
            return OptionalInt.of(image.rgb(0, 0));
        }
        return OptionalInt.empty();
    }
}
