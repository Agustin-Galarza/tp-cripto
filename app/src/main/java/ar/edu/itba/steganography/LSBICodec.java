package ar.edu.itba.steganography;

import ar.edu.itba.steganography.exceptions.SecretTooLargeException;
import ar.edu.itba.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LSBICodec implements StegoCodec {
    private static final int PATTERN_INFO_BIT_SIZE = 4;
    private static final int BITS_PER_PIXEL = 2;
    private static final int PIXEL_MASK = 0xFE;
    private static final int DATA_MASK = 0x01;
    private static final int PATTERN_MASK = 0b11;

    private static class PatternCount {
        private int swapped;
        private int total;

        public PatternCount(int swapped, int total) {
            this.swapped = swapped;
            this.total = total;
        }

        public PatternCount() {
            this.swapped = 0;
            this.total = 0;
        }

        public void count(boolean swapped) {
            this.total++;
            if (swapped) {
                this.swapped++;
            }
        }

        public int swapped() {
            return this.swapped;
        }

        public int total() {
            return this.total;
        }
    }

    private int swapBit(int bit) {
        assert bit == 0 || bit == 1;
        return bit == 0 ? 1 : 0;
    }

    private int getPattern(int b) {
        return b >>> 1 & PATTERN_MASK;
    }

    @Override
    public BufferedImage encode(byte[] secret, BufferedImage coverImage) throws SecretTooLargeException {
        var imageHeight = coverImage.getHeight();
        var imageWidth = coverImage.getWidth();
        var stegoImage = ImageUtils.deepCopy(coverImage);
        var secretBits = BitSet.valueOf(secret);
        final Map<Integer, PatternCount> patternMap = Map.of(
          0b11, new PatternCount(),
          0b10, new PatternCount(),
          0b01, new PatternCount(),
          0b00, new PatternCount());
        int c = 0, x, y;
        int[] colors = new int[BITS_PER_PIXEL]; // Use only Blue(0) and Green(1)

        /*
         * First 4 bytes of the image (4 bits of data) will be used to store data about
         * the swapped patterns. With four bits, each one will represent a
         * pattern and will be set to 1 if the bytes with that pattern must
         * be swapped and 0 if the bytes must remain the same.
         * The order for the patterns is as follows:
         * 00 01 10 11
         *
         * So for example, if the first four bits (each stored in a byte of the image)
         * look like '0101', then for all subsequent bits if the image byte contains
         * the '11' or '01' patterns then the secret bits must be swapped in order to
         * recover the image correctly
         */

        // Reserve space for the pattern information (4 bytes)
        x = 1;
        y = imageHeight - 1;
        c = 1;

        var p = new Color(coverImage.getRGB(x++, y));

        for (int i = 0; i < secret.length * 8; i++) {
            int bit = secretBits.get(7 - i % 8 + ((int) i / 8) * 8) ? 1 : 0;
            if (c == 0) {
                if (y < 0) {
                    throw new SecretTooLargeException(
                      coverImage.getWidth() * coverImage.getHeight() * 3L / 8,
                      secret.length);
                }
                p = new Color(coverImage.getRGB(x++, y));
                if (x == imageWidth) {
                    x = 0;
                    y--;
                }
            }
            var imageByte = switch (c) {
                case 0 -> p.getBlue();
                case 1 -> p.getGreen();
                default -> throw new IllegalStateException("Unexpected value: " + c);
            };
            patternMap.get(getPattern(imageByte))
              .count((imageByte & 1) != bit);
            c = (c + 1) % BITS_PER_PIXEL;
        }

        var swappedPatterns = new ArrayList<>(4);
        for (var e : patternMap.entrySet()) {
            var patternCount = e.getValue();
            if (patternCount.swapped() > patternCount.total() / 2) {
                swappedPatterns.add(e.getKey());
            }
        }

        x = 0;
        y = imageHeight - 1;
        c = 0;

        // Store 00, 01 and 10
        stegoImage.setRGB(x++, y, new Color(
          (p.getRed() & PIXEL_MASK) | (swappedPatterns.contains(0b10) ? 1 : 0),
          (p.getGreen() & PIXEL_MASK) | (swappedPatterns.contains(0b01) ? 1 : 0),
          (p.getBlue() & PIXEL_MASK) | (swappedPatterns.contains(0b00) ? 1 : 0)
        ).getRGB());
        if (x == imageWidth) {
            y--;
            x = 0;
        }
        // Store 11
        // The next channel to write is the Green channel for the current byte
        colors[c] = swappedPatterns.contains(0b11) ? 1 : 0;
        // compensate for the bit swap that may occur when writing this bit
        if(swappedPatterns.contains(getPattern((new Color(stegoImage.getRGB(x, y))).getBlue()))) {
            colors[c] = swapBit(colors[c]);
        }
        c++;

        for (int i = 0; i < secret.length * 8; i++) {
            boolean sBit = secretBits.get(7 - i % 8 + ((int) i / 8) * 8);

            colors[c++] = sBit ? 1 : 0;

            if (c < 2) {
                continue;
            }
            c = 0;

            var pixel = new Color(coverImage.getRGB(x, y));
            int bluePattern = getPattern(pixel.getBlue());
            int greenPattern = getPattern(pixel.getGreen());

            var r = pixel.getRed();
            var b = (pixel.getBlue() & PIXEL_MASK) |
                      (swappedPatterns.contains(bluePattern) ? swapBit(colors[0]) : colors[0]);
            var g = (pixel.getGreen() & PIXEL_MASK) |
                      (swappedPatterns.contains(greenPattern) ? swapBit(colors[1]) : colors[1]);

            stegoImage.setRGB(x, y, new Color(r, g, b).getRGB());
            x++;
            if (x == imageWidth) {
                y--;
                x = 0;
            }
        }
        if (c == 1) {
            var pixel = new Color(coverImage.getRGB(x, y));
            var b = (pixel.getBlue() & PIXEL_MASK) |
                      (swappedPatterns.contains(getPattern(pixel.getBlue())) ? swapBit(colors[0]) : colors[0]);

            stegoImage.setRGB(x, y, new Color(pixel.getRed(), pixel.getGreen(), b).getRGB());
        }

        return stegoImage;
    }

    @Override
    public byte[] decode(BufferedImage stegoImage) {
        var secret = new ArrayList<Byte>(256);
        int[] values = new int[8];
        short v = 0;

        var imagePixels = stegoImage.getRGB(
          0,
          0,
          stegoImage.getWidth(),
          stegoImage.getHeight(),
          null,
          0,
          stegoImage.getWidth()
        );

        var swappedPatterns = new HashSet<>(4);

        int x = 0;
        int y = stegoImage.getHeight() - 1;

        var firstPixel = new Color(imagePixels[x++ + y * stegoImage.getWidth()]);

        // Read first three patterns
        int bIndex = 0;
        for (int b : List.of(firstPixel.getBlue(), firstPixel.getGreen(), firstPixel.getRed())) {
            if ((b & DATA_MASK) == 1) {
                swappedPatterns.add(switch (bIndex) {
                    case 0 -> 0b00;
                    case 1 -> 0b01;
                    case 2 -> 0b10;
                    default -> throw new IllegalStateException("Unexpected value for iterator: " + bIndex);
                });
            }
            bIndex++;
        }
        // Read the last pattern
        int secondPixelBlueChannel = (new Color(imagePixels[x + y * stegoImage.getWidth()])).getBlue();
        if ((secondPixelBlueChannel & DATA_MASK) == 1) {
            swappedPatterns.add(0b11);
        }
        boolean startOffset = true; // Since we read the first channel (blue), we have to start reading the second
        // one in the loop (green)

        for(; y >= 0; y--) {
            if(!startOffset) {
                x = 0;
            }
            for (; x < stegoImage.getWidth(); x++) {
                int index = x + y * stegoImage.getWidth();
                var pixel = new Color(imagePixels[index]);
                int c = 0;
                if (startOffset) {
                    c = 1;
                    startOffset = false;
                }
                for (; c < 2; c++) {
                    var b = switch (c) {
                        case 0 -> pixel.getBlue();
                        case 1 -> pixel.getGreen();
                        default -> throw new IllegalStateException(
                          "Unexpected value: " + c);
                    };

                    values[v++] = swappedPatterns.contains(getPattern(b)) ? swapBit(b & DATA_MASK) : b & DATA_MASK;
                    if (v == 8) {
                        byte s = 0;
                        for (int j = 0; j < 8; j++) {
                            s |= (byte) (values[j] << (7 - j));
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
