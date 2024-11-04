package ar.edu.itba.steganography;

import ar.edu.itba.steganography.exceptions.SecretTooLargeException;
import ar.edu.itba.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class LSB4Codec implements StegoCodec {

    private static final byte PIXEL_MASK = (byte) 0xF0;
    private static final byte DATA_MASK = (byte) 0x0F;
    private static final short BYTE_CAPACITY = 2;
    private static final short SHIFT_FACTOR = 4;

    public BufferedImage encode(byte[] secret, BufferedImage coverImage)
        throws SecretTooLargeException {
        var stegoImage = ImageUtils.deepCopy(coverImage);

        int x = 0;
        int y = 0;
        short c = 0; // RGB component: 0 = B, 1 = G, 2 = R
        int[] rgb = new int[3]; // RGB values: 0 = B, 1 = G, 2 = R

        for (byte s : secret) {
            for (int i = BYTE_CAPACITY-1; i >= 0; i--) {
                rgb[c++] = s >>> (SHIFT_FACTOR * i) & DATA_MASK;

                if (c < 3) {
                    continue;
                }
                c = 0;
                if (y >= coverImage.getHeight()) {
                    throw new SecretTooLargeException(
                        coverImage.getWidth() * coverImage.getHeight() * 3L / BYTE_CAPACITY,
                        secret.length
                    );
                }
                Color pixel = new Color(coverImage.getRGB(x, y));

                int b = (pixel.getBlue() & PIXEL_MASK) | rgb[0];
                int g = (pixel.getGreen() & PIXEL_MASK) | rgb[1];
                int r = (pixel.getRed() & PIXEL_MASK) | rgb[2];

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
                        coverImage.getWidth() * coverImage.getHeight() * 3L / BYTE_CAPACITY,
                        secret.length
                );
            }
            Color pixel = new Color(coverImage.getRGB(x, y));
            int b = pixel.getBlue();
            int g = pixel.getGreen();
            int r = pixel.getRed();
            switch (c) {
                case 1:
                    b = (b & PIXEL_MASK) | rgb[0];
                    break;
                case 2:
                    b = (b & PIXEL_MASK) | rgb[0];
                    g = (g & PIXEL_MASK) | rgb[1];
                    break;
            }
            stegoImage.setRGB(x, y, new Color(r, g, b).getRGB());
        }

        return stegoImage;
    }

    public byte[] decode(byte[] imageBytes) {
        var secret = new ArrayList<Byte>(256);
        short v = 0;
        int[] halves = new int[BYTE_CAPACITY];
        for (var b : imageBytes) {
            var color = new Color(b);
            for (int c = 0; c < 3; c++) {
                halves[v++] = switch (c) {
                    case 0 -> color.getBlue() & DATA_MASK;
                    case 1 -> color.getGreen() & DATA_MASK;
                    case 2 -> color.getRed() & DATA_MASK;
                    default -> throw new IllegalStateException(
                        "Unexpected value: " + c
                    );
                };
                if (v == BYTE_CAPACITY) {
                    byte s = 0;
                    for (int i = 0; i < BYTE_CAPACITY; i++) {
                        s |= (byte) (halves[i] << (4 - SHIFT_FACTOR * i));
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
