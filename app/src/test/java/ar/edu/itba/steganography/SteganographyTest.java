package ar.edu.itba.steganography;

import ar.edu.itba.FileCodec;
import ar.edu.itba.config.EncryptionAlgorithmType;
import ar.edu.itba.config.ProgramConfig;
import ar.edu.itba.config.SteganographyAlgorithmType;
import ar.edu.itba.encryption.EncryptionCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SteganographyTest {

    private static final String TEST_IMAGES_BASE_PATH = "/images";
    private static final String TEST_RESULTS_BASE_PATH = "/results";
    private static final List<String> IMAGE_GROUPS = List.of("basic", "encrypted", "final");

    @BeforeEach
    public void setUp() {
    }

    private Path getImagesPath(String group) {
        var resource = getClass().getResource(TEST_IMAGES_BASE_PATH + "/" + group);
        if(resource == null) {
            throw new RuntimeException("Could not find resource " + TEST_IMAGES_BASE_PATH + "/" + group);
        }
        return Path.of(resource.getPath());
    }

    private File getOutputFile(String filename) {
        var resource = getClass().getResource(TEST_RESULTS_BASE_PATH);
        if(resource == null) {
            throw new RuntimeException("Could not find resource directory " + TEST_RESULTS_BASE_PATH);
        }
        return new File(resource.getPath() + "/" + filename);
    }

    private void callCodec(ProgramConfig config) {
        var codec = new FileCodec(
          switch (config.steg()) {
              case SteganographyAlgorithmType.LSB1 -> new LSBNCodec(1);
              case SteganographyAlgorithmType.LSB4 -> new LSBNCodec(4);
              case SteganographyAlgorithmType.LSBI -> new LSBICodec();
          },
          config.enc().equals(EncryptionAlgorithmType.PLAIN_TEXT) ?
            null :
            new EncryptionCodec(config.enc(), config.mode())
        );

        if (config.embed()) {
            codec.encode(
              config.secretMessage(),
              config.coverImage(),
              config.stegoImage(),
              config.password()
            );
        } else {
            codec.decode(config.coverImage(), config.stegoImage(), config.password());
        }

    }

    @Test
    public void testExtractFinalImages() {
        try (Stream<Path> paths = Files.list(getImagesPath("final")).filter(Files::isRegularFile)) {
            for(var path : paths.toList()) {
                var file = path.toFile();

                List<SteganographyAlgorithmType> algoResults = new ArrayList<>();
                Map<SteganographyAlgorithmType, String> failures = new HashMap<>();

                for(var algo : SteganographyAlgorithmType.values()) {
                    var outputFile = getOutputFile(String.format("output-%s-%s", file.getName().substring(0, file.getName().lastIndexOf(".")), algo));
                    System.out.println(String.format("Testing file %s with algorithm %s", file.getName(), algo));
                    var config = new ProgramConfig(
                      null,
                      outputFile,
                      file,
                      false,
                      SteganographyAlgorithmType.LSB1,
                      EncryptionAlgorithmType.PLAIN_TEXT,
                      null,
                      null
                    );

                    try {
                        callCodec(config);

                        algoResults.add(algo);
                    } catch (Exception e) {
                        failures.put(algo, e.getMessage());
                    }
                }

                var str = new StringBuilder("Failures:\n");
                for(var msg : failures.entrySet()) {
                    str.append(msg.getKey().name()).append(": ").append(msg.getValue()).append("\n");
                }

                assertFalse(algoResults.isEmpty(), str.toString());

                System.out.println("Successful algorithms:");
                for(var alg : algoResults) {
                    System.out.println(alg.name());
                }
            }
        } catch (IOException e) {
            fail("Could not find image directory " + getImagesPath("final") + ": " + e.getMessage());
        }
    }
}
