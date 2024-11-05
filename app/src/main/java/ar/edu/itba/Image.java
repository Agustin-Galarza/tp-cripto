package ar.edu.itba;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.FileAlreadyExistsException;

public class Image {
    private final byte[] header;
    private final byte[] body;

    public Image(String path) throws FileNotFoundException, IOException {
        var file = new File(path);
        if(!file.exists()) {
            throw new FileNotFoundException(path);
        }
        var stream = new FileInputStream(file);
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

    public byte[] getHeader() {
        return header;
    }

    public byte[] getBody() {
        return body;
    }

    public File writeNewBody(String filename, byte[] body) throws FileAlreadyExistsException, IOException {
        var file = new File(filename);
        if(file.exists()) {
            throw new FileAlreadyExistsException("File " + file.getAbsolutePath() + " already exists");
        }

        var created = file.createNewFile();
        if(!created) {
            throw new FileAlreadyExistsException("File " + file.getAbsolutePath() + " already exists");
        }
        var stream = new FileOutputStream(file);
        stream.write(header);
        stream.write(body);
        stream.close();

        return file;
    }
}
