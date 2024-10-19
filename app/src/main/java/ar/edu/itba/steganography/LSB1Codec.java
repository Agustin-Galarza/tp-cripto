package ar.edu.itba.steganography;

import ar.edu.itba.steganography.exceptions.SecretTooLargeException;
import ar.edu.itba.utils.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class LSB1Codec implements StegoCodec {

    public BufferedImage encode(byte[] secret, BufferedImage coverImage)
        throws SecretTooLargeException {
        var stegoImage = ImageUtils.deepCopy(coverImage);

        int x = 0;
        int y = 0;
        short c = 0; // RGB component: 0 = B, 1 = G, 2 = R
        int[] rgb = new int[3]; // RGB values: 0 = B, 1 = G, 2 = R

        for (byte s : secret) {
            for (int i = 7; i >= 0;) {
                rgb[c++] = (s >>> i--) & 1;

                if (c < 3) {
                    continue;
                }
                c = 0;
                if (y >= coverImage.getHeight()) {
                    throw new SecretTooLargeException(
                        coverImage.getWidth() * coverImage.getHeight() * 3,
                        secret.length
                    );
                }
                Color pixel = new Color(coverImage.getRGB(x, y));

                int b = (pixel.getBlue() & 0xFE) | rgb[0];
                int g = (pixel.getGreen() & 0xFE) | rgb[1];
                int r = (pixel.getRed() & 0xFE) | rgb[2];

                stegoImage.setRGB(x, y, new Color(r, g, b).getRGB());

                x++;
                if (x % coverImage.getWidth() == 0) {
                    x = 0;
                    y++;
                }
            }
        }
        if (c != 0) {
            // Write the remaining bits
            if (y > coverImage.getHeight()) {
                throw new SecretTooLargeException(
                    coverImage.getWidth() * coverImage.getHeight() * 3,
                    secret.length
                );
            }
            Color pixel = new Color(coverImage.getRGB(x, y));
            int b = pixel.getBlue();
            int g = pixel.getGreen();
            int r = pixel.getRed();
            switch (c) {
                case 1:
                    b = (b & 0xFE) | rgb[0];
                    break;
                case 2:
                    b = (b & 0xFE) | rgb[0];
                    g = (g & 0xFE) | rgb[1];
                    break;
                case 3:
                    b = (b & 0xFE) | rgb[0];
                    g = (g & 0xFE) | rgb[1];
                    r = (r & 0xFE) | rgb[2];
                    break;
            }
            stegoImage.setRGB(x, y, new Color(r, g, b).getRGB());
        }

        return stegoImage;
    }

    public byte[] decode(BufferedImage stegoImage) {
        var secret = new ArrayList<Byte>(256);
        int x = 0;
        int y = 0;
        short v = 0;
        int[] values = new int[8];
        var imageBytes = stegoImage.getRGB(
            0,
            0,
            stegoImage.getWidth(),
            stegoImage.getHeight(),
            null,
            0,
            stegoImage.getWidth()
        );
        for (var b : imageBytes) {
            var color = new Color(b);
            for (int c = 0; c < 3; c++) {
                values[v++] = switch (c) {
                    case 0 -> color.getBlue() & 1;
                    case 1 -> color.getGreen() & 1;
                    case 2 -> color.getRed() & 1;
                    default -> throw new IllegalStateException(
                        "Unexpected value: " + c
                    );
                };
                if (v == 8) {
                    byte s = 0;
                    for (int i = 0; i < 8; i++) {
                        s |= values[i] << (7 - i);
                    }
                    secret.add(s);
                    v = 0;
                }
            }
        }

        var result = new byte[secret.size()];
        for (int i = 0; i < secret.size(); i++) {
            result[i] = secret.get(i);
        }
        return result;
    }
}
