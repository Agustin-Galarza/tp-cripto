package ar.edu.itba;

import ar.edu.itba.encryption.*;
import ar.edu.itba.steganography.*;
import ar.edu.itba.steganography.exceptions.*;
import ar.edu.itba.utils.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;

public class FileCodec {

    private final EncryptionAlgorithm encryptionAlgorithm;
    private final StegoCodec steganographyAlgorithm;

    public FileCodec(
            StegoCodec steganographyAlgorithm,
            EncryptionAlgorithm encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.steganographyAlgorithm = steganographyAlgorithm;
    }

    private boolean requiresEncryption() {
        return encryptionAlgorithm != null;
    }

    public void encode(File input, File coverImage, File output, String password) {
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
                4 + (int) input.length());
        if (requiresEncryption()) {
            var encryptedBytes = encryptionAlgorithm.encrypt(message, password);

            message = new byte[4 + encryptedBytes.length];
            DataUtils.intToBytes(encryptedBytes.length, message, 0);
            System.arraycopy(encryptedBytes, 0, message, 4, encryptedBytes.length);
        }

        try {
            var coverImageBuffer = new Image(new FileInputStream(coverImage));
            var secretImage = steganographyAlgorithm.encode(
                    message,
                    coverImageBuffer);
            var outputFile = new File(output.getAbsolutePath().substring(
                    0,
                    output.getAbsolutePath().lastIndexOf('.') != -1 ? output.getAbsolutePath().lastIndexOf('.')
                            : output.getAbsolutePath().length())
                    + ".bmp");
            secretImage.save(outputFile);
            System.err.println(
                    "Secret message encoded successfully as " + outputFile.getName());
        } catch (IOException e) {
            System.err.println("An error occurred while reading the file: " + e.getMessage());
            // e.printStackTrace();
        } catch (SecretTooLargeException e) {
            System.err.println(
                    "The secret message is too large: " + e.getMessage());
        }
    }

    public void decode(File input, File output, String password) {
        try {
            var secretImage = new Image(new FileInputStream(input));
            var message = steganographyAlgorithm.decode(secretImage);
            var messageLength = DataUtils.bytesToInt(message, 0);
            System.out.println("Message length: " + messageLength);

            if (requiresEncryption()) {
                var decryptedBytes = encryptionAlgorithm.decrypt(
                        Arrays.copyOfRange(message, 4, 4 + messageLength),
                        password);

                messageLength = DataUtils.bytesToInt(decryptedBytes, 0);
                message = decryptedBytes;
                System.out.println("True message length: " + messageLength);
            }

            DataUtils.bytesToFile(message, 4, messageLength, output);
            if (message[messageLength + 4] != (byte) '.') {
                throw new IllegalArgumentException("Invalid message format");
            }

            var messageExtension = DataUtils.bytesToString(
                    message,
                    4 + messageLength,
                    (byte) '\0');

            String newFilename = (output.getName().lastIndexOf('.') == -1)
                    ? output.getName()
                    : output
                            .getName()
                            .substring(0, output.getName().lastIndexOf('.'));

            var fullOutput = new File(
                    output.toPath().resolveSibling(newFilename + messageExtension).toString());
            if (!fullOutput.exists()) {
                fullOutput.createNewFile();
            }
            var renamed = output.renameTo(fullOutput);
            if (!renamed) {
                throw new RuntimeException("File " + fullOutput.getAbsolutePath() + " could not be created."
                        + (output.canWrite() ? (fullOutput.canWrite() ? "" : " Cannot write dest file.")
                                : " Cannot write source file."));
            }

            // var outputFile = new File(output, messageExtension.toString());
            DataUtils.bytesToFile(message, 4, messageLength, fullOutput);
            System.out.println(
                    "Secret message decoded successfully as " + fullOutput.getName());
            output.delete();
        } catch (IOException e) {
            System.err.println("An error occurred while reading the file");
        }
    }
}
