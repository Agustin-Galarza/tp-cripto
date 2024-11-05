package ar.edu.itba;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.FileAlreadyExistsException;

import javax.imageio.ImageIO;

public class Image {
    private final byte[] header;
    private byte[] body;

    public Image(InputStream stream) throws IOException {
        this.header = stream.readNBytes(54);
        var reader = ByteBuffer.wrap(this.header);
        reader.order(ByteOrder.LITTLE_ENDIAN);
        reader.getShort();
        var length = reader.getInt();
        System.err.println("length: %d".formatted(length));
        reader.getShort();
        reader.getShort();
        var offset = reader.getInt();
        System.err.println("offset: %d".formatted(length));
        stream.skipNBytes(offset - 54);
        this.body = stream.readNBytes(length);
        stream.close();
    }

    public static Image load(String path) throws FileNotFoundException, IOException {
        var file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException(path);
        }
        var stream = new FileInputStream(file);
        return new Image(stream);
    }

    public static Image fromBufferedImage(BufferedImage image) {
        var outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "bmp", outputStream);
            var encoded = new ByteArrayInputStream(outputStream.toByteArray());
            return new Image(encoded);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public BufferedImage toBufferedImage() {
        var stream = new SequenceInputStream(
                new ByteArrayInputStream(getHeader()),
                new ByteArrayInputStream(getBody()));
        try {
            return ImageIO.read(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] getHeader() {
        return header;
    }

    public byte[] getBody() {
        return body;
    }

    public Image withBody(byte[] body) {
        this.body = body;
        return this;
    }

    public File save(File file) throws FileAlreadyExistsException, IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        var stream = new FileOutputStream(file);
        stream.write(header);
        stream.write(body);
        stream.close();

        return file;
    }
}
