package ar.edu.itba.encryption;

import ar.edu.itba.config.EncryptionAlgorithmType;
import ar.edu.itba.config.EncryptionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;


public class EncryptionTest {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void testEncryptionAndDecryption() throws Exception {
        for(var algorithm : EncryptionAlgorithmType.values()) {
            if(algorithm.equals(EncryptionAlgorithmType.PLAIN_TEXT)){
                continue;
            }
            for(var mode : EncryptionMode.values()) {
                System.out.printf("Testing with Alg: %s || Mode: %s%n", algorithm.algorithmName(), mode.value());
                var cipher = new EncryptionCodec(algorithm, mode);

                String originalText = "Hello, " + algorithm.algorithmName() + "!";
                String password = "password";

                // Encrypt the original text
                var encryptedBytes = cipher.encrypt(
                    originalText.getBytes(CHARSET),
                    password
                );

                var encryptedText = new String(encryptedBytes, CHARSET);

                // Ensure that encrypted text is different from the original
                assertNotEquals(originalText, encryptedText, "Encrypted text should be different from the original.");

                // Decrypt the encrypted text
                var decryptedBytes = cipher.decrypt(
                    encryptedBytes,
                    password
                );
                var decryptedText = new String(decryptedBytes, CHARSET);

                // Verify that the decrypted text matches the original
                assertEquals(originalText, decryptedText, "Decrypted text should match the original.");
            }
        }
    }

    @Test
    public void testDifferentKeysProduceDifferentCiphertexts() {
        for(var algorithm : EncryptionAlgorithmType.values()) {
            if(algorithm.equals(EncryptionAlgorithmType.PLAIN_TEXT)){
                continue;
            }
            for (var mode : EncryptionMode.values()) {
                System.out.printf("Testing with Alg: %s || Mode: %s%n", algorithm.algorithmName(), mode.value());
                var cipher = new EncryptionCodec(algorithm, mode);

                String originalText = "Hello, " + algorithm.algorithmName() + "!";
                var password = "password";
                var differentPassword = "password2";

                var encryptedBytes1 = cipher.encrypt(originalText.getBytes(CHARSET), password);
                var encryptedBytes2 = cipher.encrypt(originalText.getBytes(CHARSET), differentPassword);

                var encryptedText1 = new String(encryptedBytes1, CHARSET);
                var encryptedText2 = new String(encryptedBytes2, CHARSET);

                // Ensure that different keys produce different encrypted text
                assertNotEquals(encryptedText1, encryptedText2, "Different keys should produce different ciphertexts.");
            }
        }
    }
}
