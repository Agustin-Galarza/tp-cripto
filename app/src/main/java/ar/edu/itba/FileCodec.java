package ar.edu.itba;

import ar.edu.itba.encryption.*;
import ar.edu.itba.steganography.*;
import ar.edu.itba.steganography.exceptions.*;
import ar.edu.itba.utils.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class FileCodec {

    private final EncryptionAlgorithm encryptionAlgorithm;
    private final StegoCodec steganographyAlgorithm;

    public FileCodec(
        StegoCodec steganographyAlgorithm,
        EncryptionAlgorithm encryptionAlgorithm
    ) {
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.steganographyAlgorithm = steganographyAlgorithm;
    }

    private boolean requiresEncryption() {
        return encryptionAlgorithm != null;
    }

    public void encode(File input, File coverImage, File output) {
        String inputExtension = input
            .getName()
            .substring(input.getName().lastIndexOf('.'));

        var message = new byte[4 +
        (int) input.length() +
        inputExtension.length() +
        1];

        DataUtils.intToBytes((int) input.length(), message, 0);
        DataUtils.fileToBytes(input, message, 4);
        DataUtils.stringToBytes(
            inputExtension,
            message,
            4 + (int) input.length()
        );

        if (requiresEncryption()) {
            throw new UnsupportedOperationException("Encryption not supported");
        }

        try {
            var coverImageBuffer = ImageIO.read(coverImage);
            var secretImage = steganographyAlgorithm.encode(
                message,
                coverImageBuffer
            );
            ImageIO.write(secretImage, "bmp", output);
            System.out.println(
                "Secret message encoded successfully as " + output.getName()
            );
        } catch (IOException e) {
            System.err.println("An error occurred while reading the file");
        } catch (SecretTooLargeException e) {
            System.err.println(
                "The secret message is too large: " + e.getMessage()
            );
        }
    }

    public void decode(File input, File output) {
        try {
            var secretImage = ImageIO.read(input);
            var message = steganographyAlgorithm.decode(secretImage);
            var messageLength = DataUtils.bytesToInt(message, 0);
            if (message[messageLength + 4] != (byte) '.') {
                throw new IllegalArgumentException("Invalid message format");
            }

            var messageExtension = DataUtils.bytesToString(
                message,
                4 + messageLength,
                (byte) '\0'
            );

            String newFilename = (output.getName().lastIndexOf('.') == -1)
                ? output.getName()
                : output
                    .getName()
                    .substring(0, output.getName().lastIndexOf('.') - 1);

            var fullOutput = new File(
                    output.toPath().resolveSibling(newFilename + messageExtension).toString()
            );
            if(fullOutput.exists()) {
                throw new RuntimeException("Destination file with decoded extension: " + fullOutput.getName() + " already exists");
            }
            var renamed = output.renameTo(fullOutput);
            if(!renamed) {
                throw new RuntimeException("File " + fullOutput.getName() + " could not be created.");
            }

            // var outputFile = new File(output, messageExtension.toString());
            DataUtils.bytesToFile(message, 4, messageLength, fullOutput);
            System.out.println(
                "Secret message decoded successfully as " + fullOutput.getName()
            );
            output.delete();
        } catch (IOException e) {
            System.err.println("An error occurred while reading the file");
        }
    }
}
