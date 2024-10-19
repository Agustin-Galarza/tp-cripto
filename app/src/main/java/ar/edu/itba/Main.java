package ar.edu.itba;

import ar.edu.itba.config.*;
import ar.edu.itba.steganography.*;
import ar.edu.itba.utils.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.*;
import javax.imageio.ImageIO;
import org.apache.commons.cli.*;

public class Main {

    private static Option createOption(
        String name,
        Class<?> type,
        boolean hasArgument,
        String description
    ) {
        return Option.builder(name)
            .type(type)
            .hasArg(hasArgument)
            .desc(description)
            .required(false) // make all arguments optional so we can have a help argument
            .build();
    }

    private static void printHelp(Options options) {
        var formatter = new HelpFormatter();
        formatter.printHelp("Main", options);
    }

    public static void main(String[] args) {
        var complete = false;
        var options = new Options();
        System.out.println("Initializing");

        try {
            options.addOption(
                "h",
                "help",
                false,
                "Prints the help message and exits"
            );
            options.addOption(
                createOption(
                    "in",
                    String.class,
                    true,
                    "Path to the secret message file"
                )
            );
            options.addOption(
                createOption(
                    "embed",
                    Boolean.class,
                    false,
                    "When present, the program will embed the message. If this param is not present, then 'extract' must be."
                )
            );
            options.addOption(
                createOption(
                    "extract",
                    Boolean.class,
                    false,
                    "When present, the program will extract the embedded message from an image. If this param is not present, then 'embed' must be."
                )
            );
            options.addOption(
                createOption("p", String.class, true, "Path to the cover image")
            );
            options.addOption(
                createOption(
                    "out",
                    String.class,
                    true,
                    "Path to the output file"
                )
            );
            options.addOption(
                createOption(
                    "steg",
                    String.class,
                    true,
                    "<LSB1 | LSB4 | LSBI> Steganography algorithm to use"
                )
            );
            options.addOption(
                createOption(
                    "a",
                    String.class,
                    true,
                    "<aes128 | aes192 | aes256 | 3des> Encryption algorithm to use"
                )
            );
            options.addOption(
                createOption(
                    "m",
                    String.class,
                    true,
                    "<ecb | cfb | ofb | cbc> Encryption mode to use"
                )
            );
            options.addOption(
                createOption(
                    "pass",
                    String.class,
                    true,
                    "Encryption password. This argument is required if an encryption algorithm is specified"
                )
            );
            // TODO: Add extraction arguments

            var parser = new DefaultParser();
            var cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                printHelp(options);
                System.exit(0);
            }

            System.out.println("Parsing");
            var config = ProgramConfig.fromParsed(cmd);

            var codec = new FileCodec(
                switch (config.steg()) {
                    case SteganographyAlgorithmType.LSB1 -> new LSB1Codec();
                    case SteganographyAlgorithmType.LSB4,
                        SteganographyAlgorithmType.LSBI -> throw new Exception(
                        "Not implemented"
                    );
                },
                switch (config.enc()) {
                    case EncryptionAlgorithmType.PLAIN_TEXT -> null;
                    case EncryptionAlgorithmType.AES128,
                        EncryptionAlgorithmType.AES192,
                        EncryptionAlgorithmType.AES256,
                        EncryptionAlgorithmType._3DES -> throw new Exception(
                        "Not Implemented"
                    );
                }
            );

            if (config.embed()) {
                codec.encode(
                    config.secretMessage(),
                    config.coverImage(),
                    config.stegoImage()
                );
            } else {
                codec.decode(config.coverImage(), config.stegoImage());
            }

            complete = true;
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            printHelp(options);
        } catch (NoSuchFileException e) {
            System.err.println("No such file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println(
                "Unknown Error (" + e.getClass() + "): " + e.getMessage()
            );
        } finally {
            if (!complete) {
                System.exit(1);
            }
        }
    }
}
