package ar.edu.itba;

import ar.edu.itba.config.*;
import java.awt.image.BufferedImage;
import java.io.File;
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
                    "When present, the program will embed the message"
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

            var parser = new DefaultParser();
            var cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                printHelp(options);
                System.exit(0);
            }

            var config = ProgramConfig.fromParsed(cmd);
            System.out.println("Config:");
            System.out.println(config);
            complete = true;
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            printHelp(options);
        } catch (Exception e) {
            System.err.println("Unknown Error: " + e.getMessage());
        } finally {
            if (!complete) {
                System.exit(1);
            }
        }
    }
}
