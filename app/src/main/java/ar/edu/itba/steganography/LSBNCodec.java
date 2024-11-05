package ar.edu.itba.steganography;

import ar.edu.itba.Image;
import ar.edu.itba.steganography.exceptions.SecretTooLargeException;
import ar.edu.itba.utils.DataUtils;
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
    public Image encode(byte[] secret, Image coverImage2)
        throws SecretTooLargeException {
        var coverImage = coverImage2.getBody().clone();

        if (secret.length * imageByteRatio >= coverImage.length) {
            throw new SecretTooLargeException(
              coverImage.length / imageByteRatio,
              secret.length
            );
        }
        int b = 0;

        for (byte s : secret) {
            for (int i = imageByteRatio-1; i >= 0; i--) {
                coverImage[b] = (byte) ((coverImage[b] & pixelMask) | (s >>> (n * i) & dataMask));
                b++;

            }
        }

        return coverImage2.withBody(coverImage);
    }

    @Override
    public byte[] decode(Image imageBytes) {
        var secret = new ArrayList<Byte>(256);
        short v = 0;
        int[] values = new int[imageByteRatio];
        for (var b : imageBytes.getBody()) {
            values[v++] = b & dataMask;
            if (v == imageByteRatio) {
                byte s = 0;
                for (int i = 0; i < imageByteRatio; i++) {
                    s |= (byte) (values[i] << (8 - n - n * i));
                }
                secret.add(s);
                v = 0;
            }
        }

        var result = new byte[secret.size()];
        for (int i = 0; i < secret.size(); i++) {
            result[i] = secret.get(i);
        }
        return result;
    }
}
