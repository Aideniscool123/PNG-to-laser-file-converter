package fileConverter.core;

import fileConverter.model.PixelImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class ImageLoader {
    public static final int MAX_SIDE = 512;

    private ImageLoader() {
    }

    public static PixelImage load(File file) throws IOException {
        if (!file.isFile()) {
            throw new IOException("File not found: " + file.getPath());
        }
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Could not read \"" + file.getName() + "\" as an image. Try a PNG.");
        }
        if (image.getWidth() > MAX_SIDE || image.getHeight() > MAX_SIDE) {
            throw new IOException("\"" + file.getName() + "\" is " + image.getWidth() + " x " + image.getHeight()
                    + " pixels. The limit is " + MAX_SIDE + " x " + MAX_SIDE + ". Shrink it to real pixel-art size first.");
        }
        return PixelImage.fromBufferedImage(file.getName(), image);
    }
}
