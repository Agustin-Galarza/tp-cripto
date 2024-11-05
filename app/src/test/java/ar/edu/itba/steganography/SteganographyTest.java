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

    private File getOutputFile(String filename) throws IOException {
        var resource = getClass().getResource(TEST_RESULTS_BASE_PATH);
        if(resource == null) {
            throw new RuntimeException("Could not find resource directory " + TEST_RESULTS_BASE_PATH);
        }
        var file = new File(resource.getPath() + "/" + filename);
        if(file.exists()) {
            return file;
        }
        var isCreated = file.createNewFile();
        if(!isCreated) {
            throw new RuntimeException("Could not create file " + file.getAbsolutePath());
        }
        var isWriteable = file.setWritable(true);
        if(!isWriteable) {
            throw new RuntimeException("Could not make file " + file.getAbsolutePath() + " writeable.");
        }

        return file;
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
    public void testExtractBasicImages() {
        var outputFiles = new HashSet<File>();
        try {
            var stegoFiles = Files.list(getImagesPath("basic")).filter(Files::isRegularFile).map(Path::toFile).toList();

            for (var algo : SteganographyAlgorithmType.values()) {
                var file = stegoFiles.stream().filter(sf -> sf.getName().equals(String.format("lado%s.bmp", algo.name()))).findFirst().orElseThrow();

                var outputFile = getOutputFile(String.format("output-%s-%s", file.getName().substring(0, file.getName().lastIndexOf(".")), algo));
                outputFiles.add(outputFile);
                System.out.printf("Testing file %s with algorithm %s%n", file.getName(), algo);
                var config = new ProgramConfig(
                  null,
                  outputFile,
                  file,
                  false,
                  algo,
                  EncryptionAlgorithmType.PLAIN_TEXT,
                  null,
                  null
                );

                assertDoesNotThrow(() -> callCodec(config));
            }
        } catch (IOException e) {
            fail("Error with files management: " + e.getMessage());
        } finally {
            outputFiles.stream().flatMap(f -> Stream.of(f, new File(f.getAbsolutePath() + ".png"))).forEach(File::delete);
        }
    }
}
