package ar.edu.itba.config;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

public record ProgramConfig(
  File secretMessage,
  File stegoImage, // TODO: rename to output file
  File coverImage,
  boolean embed,
  SteganographyAlgorithmType steg,
  EncryptionAlgorithmType enc,
  EncryptionMode mode,
  String password
) {
    public static ProgramConfig fromParsed(CommandLine cmd)
      throws ParseException {
        // Manually check for required options
        for (String opt : Arrays.asList("p", "out", "steg")) {
            if (!cmd.hasOption(opt)) {
                throw new ParseException("Missing required option: " + opt);
            }
        }

        if (!cmd.hasOption("embed") && !cmd.hasOption("extract")) {
            throw new ParseException(
              "Missing required option: embed or extract"
            );
        }
        if (cmd.hasOption("embed") && cmd.hasOption("extract")) {
            throw new ParseException(
              "Cannot specify both embed and extract options"
            );
        }

        boolean embedding = cmd.hasOption("embed");

        if (embedding && !cmd.hasOption("in")) {
            throw new ParseException("Missing required option: in");
        }

        File secretMessage = null;
        if (embedding) {
            secretMessage = new File(cmd.getOptionValue("in"));
            if (!secretMessage.exists() || !secretMessage.isFile()) {
                throw new ParseException("Secret message file does not exist");
            }
            if (!secretMessage.canRead()) {
                throw new ParseException("Secret message file is not readable");
            }
        }

        var coverImage = new File(cmd.getOptionValue("p"));

        if (!coverImage.exists() || !coverImage.isFile()) {
            throw new ParseException("Cover image file %s does not exist".formatted(coverImage));
        }
        if (!coverImage.canRead()) {
            throw new ParseException("Cover image file is not readable");
        }
        var stegoImage = new File(cmd.getOptionValue("out"));

        if (!stegoImage.exists()) {
            // create the file if it doesn't exist
            try {
                stegoImage.createNewFile();
            } catch (Exception e) {
                throw new ParseException("Could not create stego image file");
            }
        }
        if (embedding && !stegoImage.isFile()) {
            throw new ParseException("Stego image is not a file");
        }
        if (embedding && !stegoImage.canWrite()) {
            throw new ParseException("Stego image file is not writable");
        }
        var encAlgorithm = EncryptionAlgorithmType.PLAIN_TEXT;

        var encMode = EncryptionMode.CBC;
        if (cmd.hasOption("a") || cmd.hasOption("m")) {
            if (!cmd.hasOption("pass")) {
                throw new ParseException(
                  "Password is required when using encryption"
                );
            }
            encAlgorithm = cmd.hasOption("a")
                             ? switch (cmd.getOptionValue("a").toLowerCase()) {
                case "aes128" -> EncryptionAlgorithmType.AES128;
                case "aes192" -> EncryptionAlgorithmType.AES192;
                case "aes256" -> EncryptionAlgorithmType.AES256;
                case "3des" -> EncryptionAlgorithmType._3DES;
                default ->
                  throw new RuntimeException(String.format("Encryption algorithm %s not recognized", cmd.getOptionValue("a")));
            }
                             : EncryptionAlgorithmType.AES128;
            if (cmd.hasOption("m")) {
                encMode = EncryptionMode.valueOf(cmd.getOptionValue("m").toUpperCase());
            }
        }
        return new ProgramConfig(
          secretMessage,
          stegoImage,
          coverImage,
          embedding,
          SteganographyAlgorithmType.valueOf(cmd.getOptionValue("steg")),
          encAlgorithm,
          encMode,
          cmd.getOptionValue("pass")
        );
    }
}
