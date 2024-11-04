package ar.edu.itba.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class DataUtils {

    /*
     *   Writes an integer into 4 bytes of the given byte array starting at the given offset.
     *   The most significant byte is written first.
     */
    public static void intToBytes(int value, byte[] byteArray, int offset) {
        assert byteArray != null;
        if (byteArray.length < offset + 4) {
            throw new IllegalArgumentException(
                "The byte array is too small to hold the int value"
            );
        }

        byteArray[offset] = (byte) (value >> 24);
        byteArray[offset + 1] = (byte) (value >> 16);
        byteArray[offset + 2] = (byte) (value >> 8);
        byteArray[offset + 3] = (byte) value;
    }

    public static void fileToBytes(File file, byte[] byteArray, int offset) {
        assert file != null;
        assert byteArray != null;
        if (byteArray.length < offset + file.length()) {
            throw new IllegalArgumentException(
                "The byte array is too small to hold the file contents"
            );
        }

        try (var fis = new FileInputStream(file)) {
            fis.read(byteArray, offset, (int) file.length());
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "An error occurred while reading the file"
            );
        }
    }

    public static void bytesToFile(
        byte[] bytes,
        int offset,
        int maxBytes,
        File target
    ) {
        assert bytes != null;
        assert target != null;
        if (bytes.length < offset + maxBytes) {
            throw new IllegalArgumentException(
                "The byte array is too small to hold the file contents"
            );
        }

        try (var fos = new FileOutputStream(target)) {
            fos.write(bytes, offset, maxBytes);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "An error occurred while writing the file: " + e.getMessage()
            );
        }
    }

    public static void stringToBytes(String str, byte[] byteArray, int offset) {
        assert str != null;
        assert byteArray != null;
        if (byteArray.length < offset + str.length()) {
            throw new IllegalArgumentException(
                "The byte array is too small to hold the string"
            );
        }

        int i = 0;
        for (; i < str.length(); i++) {
            byteArray[offset + i] = (byte) str.charAt(i);
        }
        byteArray[offset + i] = (byte) '\0';
    }

    public static String bytesToString(byte[] bytes, int offset, byte stop) {
        assert bytes != null;
        if (bytes.length < offset) {
            throw new IllegalArgumentException(
                "The byte array is too small to hold the string"
            );
        }

        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < bytes.length; i++) {
            if (bytes[i] == stop) {
                break;
            }
            sb.append((char) bytes[i]);
        }

        return sb.toString();
    }

    public static int bytesToInt(byte[] bytes, int offset) {
        assert bytes != null;
        if (bytes.length < offset + 4) {
            throw new IllegalArgumentException(
                "The byte array is too small to hold the int value"
            );
        }

        return (
            (bytes[offset] << 24) |
            ((bytes[offset + 1] & 0xFF) << 16) |
            ((bytes[offset + 2] & 0xFF) << 8) |
            (bytes[offset + 3] & 0xFF)
        );
    }

    public static byte[] readImage(File file) throws FileNotFoundException, IOException {
        var stream = new FileInputStream(file);
        var reader = ByteBuffer.wrap(stream.readNBytes(54));
        reader.order(ByteOrder.LITTLE_ENDIAN);
        reader.getShort();
        var length = reader.getInt();
        System.err.println("length: %d".formatted(length));
        reader.getShort();
        reader.getShort();
        var offset = reader.getInt();
        System.err.println("offset: %d".formatted(length));
        stream.skipNBytes(offset - 54);
        var data = stream.readNBytes(length);
        stream.close();

        return data;
    }
}
