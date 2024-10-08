package ar.edu.itba.config;

import java.io.File;
import java.util.Arrays;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

public record ProgramConfig(
    File secretMessage,
    File stegoImage,
    File coverImage,
    boolean embed,
    SteganographyAlgorithm steg,
    EncryptionAlgorithm enc,
    EncryptionMode mode,
    String password
) {
    public static ProgramConfig fromParsed(CommandLine cmd)
        throws ParseException {
        // Manually check for required options
        for (String opt : Arrays.asList("in", "p", "out", "steg")) {
            if (!cmd.hasOption(opt)) {
                throw new ParseException("Missing required option: " + opt);
            }
        }
        if (
            (cmd.hasOption("a") || cmd.hasOption("m")) && !cmd.hasOption("pass")
        ) {
            throw new ParseException(
                "Password is required when using encryption"
            );
        }

        return new ProgramConfig(
            new File(cmd.getOptionValue("in")),
            new File(cmd.getOptionValue("out")),
            new File(cmd.getOptionValue("p")),
            cmd.hasOption("embed"),
            SteganographyAlgorithm.valueOf(cmd.getOptionValue("steg")),
            cmd.hasOption("a")
                ? EncryptionAlgorithm.valueOf(cmd.getOptionValue("a"))
                : EncryptionAlgorithm.AES128,
            cmd.hasOption("m")
                ? EncryptionMode.valueOf(cmd.getOptionValue("m"))
                : EncryptionMode.CBC,
            cmd.getOptionValue("pass")
        );
    }
}
