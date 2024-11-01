package ar.edu.itba.steganography;

import ar.edu.itba.steganography.exceptions.SecretTooLargeException;
import ar.edu.itba.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class LSBNCodec implements StegoCodec {

    /** How many bytes of image are needed to fit one byte of data */
    private final short imageByteRatio;
    private final short n;
    private final byte pixelMask;
    private final byte dataMask;

    public LSBNCodec(int n) {
        if(n > 8 || n < 0) {
            throw new IllegalArgumentException("Value for n must be between 0 and 8: " + n + " given");
        }
        if(8 % n != 0) {
            throw new IllegalArgumentException("Value for n must be a divisor of 8: " + n + " given");
        }
        this.imageByteRatio = (short) (8 / n);
        this.n = (short) n;
        this.pixelMask = (byte) ((byte) 0xFF << n);
        this.dataMask = (byte) (0xFF >>> (8-n));
    }

    @Override
    public BufferedImage encode(byte[] secret, BufferedImage coverImage)
        throws SecretTooLargeException {
        var stegoImage = ImageUtils.deepCopy(coverImage);

        int x = 0;
        int y = 0;
        short c = 0; // RGB component: 0 = B, 1 = G, 2 = R
        int[] rgb = new int[3]; // RGB values: 0 = B, 1 = G, 2 = R

        for (byte s : secret) {
            for (int i = imageByteRatio-1; i >= 0; i--) {
                rgb[c++] = s >>> (n * i) & dataMask;

                if (c < 3) {
                    continue;
                }
                c = 0;
                if (y >= coverImage.getHeight()) {
                    throw new SecretTooLargeException(
                        coverImage.getWidth() * coverImage.getHeight() * 3L / imageByteRatio,
                        secret.length
                    );
                }
                Color pixel = new Color(coverImage.getRGB(x, y));

                int b = (pixel.getBlue() & pixelMask) | rgb[0];
                int g = (pixel.getGreen() & pixelMask) | rgb[1];
                int r = (pixel.getRed() & pixelMask) | rgb[2];

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
                        coverImage.getWidth() * coverImage.getHeight() * 3L / imageByteRatio,
                        secret.length
                );
            }
            Color pixel = new Color(coverImage.getRGB(x, y));
            int b = pixel.getBlue();
            int g = pixel.getGreen();
            int r = pixel.getRed();
            switch (c) {
                case 1:
                    b = (b & pixelMask) | rgb[0];
                    break;
                case 2:
                    b = (b & pixelMask) | rgb[0];
                    g = (g & pixelMask) | rgb[1];
                    break;
            }
            stegoImage.setRGB(x, y, new Color(r, g, b).getRGB());
        }

        return stegoImage;
    }

    @Override
    public byte[] decode(BufferedImage stegoImage) {
        var secret = new ArrayList<Byte>(256);
        short v = 0;
        int[] values = new int[imageByteRatio];
        var imageBytes = stegoImage.getRGB(
            0,
            0,
            stegoImage.getWidth(),
            stegoImage.getHeight(),
            null,
            0,
            stegoImage.getWidth()
        );
        for (var y = stegoImage.getHeight() - 1; y >= 0; y--) {
            for (var x = 0; x < stegoImage.getWidth(); x++) {

                var index = y * stegoImage.getWidth() + x;
                var b = imageBytes[index];
                var color = new Color(b);
                for (int c = 0; c < 3; c++) {
                    values[v++] = switch (c) {
                        case 0 -> color.getBlue() & dataMask;
                        case 1 -> color.getGreen() & dataMask;
                        case 2 -> color.getRed() & dataMask;
                        default -> throw new IllegalStateException(
                            "Unexpected value: " + c
                        );
                    };
                    if (v == imageByteRatio) {
                        byte s = 0;
                        for (int i = 0; i < imageByteRatio; i++) {
                            s |= (byte) (values[i] << (8 - n - n * i));
                        }
                        secret.add(s);
                        v = 0;
                    }
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
