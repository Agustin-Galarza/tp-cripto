package ar.edu.itba.encryption;

import ar.edu.itba.config.EncryptionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;


public class AESTest {

    private AESEncryption aesCipher;
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    @BeforeEach
    public void setUp() {
        // Set up AES with a 128-bit key for this test
        aesCipher = new AESEncryption(128);
    }

    @Test
    public void testEncryptionAndDecryption() throws Exception {
        String originalText = "Hello, AES!";
        String password = "password";
        EncryptionMode mode  = EncryptionMode.CBC;

        // Encrypt the original text
        var encryptedBytes = aesCipher.encrypt(
                originalText.getBytes(CHARSET),
                mode,
                password
        );

        var encryptedText = new String(encryptedBytes, CHARSET);

        // Ensure that encrypted text is different from the original
        assertNotEquals(originalText, encryptedText, "Encrypted text should be different from the original");

        // Decrypt the encrypted text
        var decryptedBytes = aesCipher.decrypt(
                encryptedBytes,
                mode,
                password
        );
        var decryptedText = new String(decryptedBytes, CHARSET);

        // Verify that the decrypted text matches the original
        assertEquals(originalText, decryptedText, "Decrypted text should match the original");
    }

    @Test
    public void testDifferentKeysProduceDifferentCiphertexts() {
        var originalText = "Hello, AES!";
        var mode = EncryptionMode.CBC;
        var password = "password";
        var differentPassword = "password2";

        var encryptedBytes1 = aesCipher.encrypt(originalText.getBytes(CHARSET), mode, password);
        var encryptedBytes2 = aesCipher.encrypt(originalText.getBytes(CHARSET), mode, differentPassword);

        var encryptedText1 = new String(encryptedBytes1, CHARSET);
        var encryptedText2 = new String(encryptedBytes2, CHARSET);

        // Ensure that different keys produce different encrypted text
        assertNotEquals(encryptedText1, encryptedText2, "Different keys should produce different ciphertexts");
    }

    @Test
    public void testRandomCipherSizesNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> new AESEncryption(127));
        assertDoesNotThrow(() -> new AESEncryption(128));
        assertDoesNotThrow(() -> new AESEncryption(192));
        assertDoesNotThrow(() -> new AESEncryption(256));
    }
}
