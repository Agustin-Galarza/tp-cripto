package ar.edu.itba.encryption;

import ar.edu.itba.config.EncryptionMode;
import ar.edu.itba.utils.EncryptionUtils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

public class AESEncryption implements EncryptionAlgorithm {
    private static final Set<Integer> ALLOWED_KEY_SIZES = Set.of(128, 192, 256);
    private final int keySize;
    private final int ivSize;
    private static final int SALT_SIZE = 16;

    public AESEncryption(int keySize) {
        if(!ALLOWED_KEY_SIZES.contains(keySize)) {
            throw new IllegalArgumentException("Key size must be one of " + ALLOWED_KEY_SIZES.toString().replaceAll("[\\[\\]]", ""));
        }
        this.keySize = keySize;
        this.ivSize = keySize / 8;
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_SIZE];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    @Override
    public byte[] encrypt(byte[] data, EncryptionMode mode, String password) {
        try {
            byte[] salt = generateSalt();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            var key = EncryptionUtils.deriveKey("AES", password, salt, keySize);
            var iv = EncryptionUtils.deriveIV(password, salt, ivSize);

            // Encrypt the text
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] encryptedBytes = cipher.doFinal(data);
            byte[] encryptedBytesWithSalt = new byte[salt.length + iv.getIV().length + encryptedBytes.length];
            System.arraycopy(salt, 0, encryptedBytesWithSalt, 0, salt.length);
            System.arraycopy(iv.getIV(), 0, encryptedBytesWithSalt, salt.length, iv.getIV().length);
            System.arraycopy(encryptedBytes, 0, encryptedBytesWithSalt, salt.length + iv.getIV().length, encryptedBytes.length);

            return encryptedBytesWithSalt;
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Could not initialize AES algorithm", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid key", e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("Invalid padding selected", e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Invalid block size selected", e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] data, EncryptionMode mode, String password) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] salt = Arrays.copyOfRange(data, 0, SALT_SIZE);
            byte[] ivBytes = Arrays.copyOfRange(data, SALT_SIZE, SALT_SIZE + ivSize);
            byte[] encryptedBytes = Arrays.copyOfRange(data, SALT_SIZE + ivSize, data.length);

            var key = EncryptionUtils.deriveKey("AES", password, salt, keySize);
            var iv = new IvParameterSpec(ivBytes);

            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            return cipher.doFinal(encryptedBytes);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Could not initialize AES algorithm", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid key", e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("Invalid padding selected", e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Invalid block size selected", e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
}
