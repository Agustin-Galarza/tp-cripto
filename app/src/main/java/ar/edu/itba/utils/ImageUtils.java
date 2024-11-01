package ar.edu.itba.utils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class ImageUtils {

    public static BufferedImage deepCopy(BufferedImage original) {
        if (original == null) {
            throw new IllegalArgumentException(
                "The original image cannot be null"
            );
        }

        BufferedImage copy = new BufferedImage(
            original.getWidth(),
            original.getHeight(),
            BufferedImage.TYPE_3BYTE_BGR
        );

        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        return copy;
    }
}
