package fileConverter.model;

import java.awt.image.BufferedImage;

public class PixelImage {
    private final String name;
    private final int width;
    private final int height;
    private final int[] argb;

    public PixelImage(String name, int width, int height, int[] argb) {
        if(argb.length != width * height) {
            throw new IllegalArgumentException("Expected " + (width * height) + " pixels but got " + argb.length);
        }

        this.name = name;
        this.width = width;
        this.height = height;
        this.argb = argb.clone();
    }

    public static PixelImage fromBufferedImage(String name, BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = new int[w * h];
        image.getRGB(0, 0, w, h, pixels, 0, w);
        return new PixelImage(name, w, h, pixels);
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public int argb(int x, int y) {
        return argb[y * width + x];
    }

    public int rgb(int x, int y) {
        return argb(x, y) & 0xFFFFFF;
    }

    public int alpha(int x, int y) {
        return argb(x, y) >>> 24;
    }

    public boolean visible(int x, int y, int alphaCutoff) {
        return inBounds(x, y) && alpha(x, y) >= alphaCutoff;
    }
}
