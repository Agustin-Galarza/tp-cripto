package ar.edu.itba.encryption;

import ar.edu.itba.config.EncryptionAlgorithmType;
import ar.edu.itba.config.EncryptionMode;
import ar.edu.itba.utils.EncryptionUtils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Set;

public class EncryptionCodec implements EncryptionAlgorithm {
    private static final int SALT_SIZE = 16;

    private final EncryptionAlgorithmType algorithmType;
    private final EncryptionMode encryptionMode;
    private final Cipher cipher;

    public EncryptionCodec(EncryptionAlgorithmType algorithmType, EncryptionMode mode) {
        if(algorithmType.equals(EncryptionAlgorithmType.PLAIN_TEXT)) {
            throw new IllegalArgumentException("PLAIN TEXT encryption is not a valid algorithm");
        }
        this.algorithmType = algorithmType;
        this.encryptionMode = mode;
        try {
            this.cipher = Cipher.getInstance(EncryptionUtils.buildTransformation(algorithmType.algorithm(), mode));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_SIZE];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    @Override
    public byte[] encrypt(byte[] data, String password) {
        try {
            byte[] salt = generateSalt();

            var key = EncryptionUtils.deriveKey(algorithmType.algorithm(), password, salt, algorithmType.keySize());
            if(encryptionMode.equals(EncryptionMode.ECB)) {
                // ECB mode does not use IV
                cipher.init(Cipher.ENCRYPT_MODE, key);
                byte[] encryptedBytes = cipher.doFinal(data);
                byte[] encryptedBytesWithSalt = new byte[salt.length + encryptedBytes.length];
                System.arraycopy(salt, 0, encryptedBytesWithSalt, 0, salt.length);
                System.arraycopy(encryptedBytes, 0, encryptedBytesWithSalt, salt.length, encryptedBytes.length);

                return encryptedBytesWithSalt;
            }
            var iv = EncryptionUtils.deriveIV(password, salt, algorithmType.ivSize());

            // Encrypt the text
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] encryptedBytes = cipher.doFinal(data);
            byte[] encryptedBytesWithSalt = new byte[salt.length + iv.getIV().length + encryptedBytes.length];
            System.arraycopy(salt, 0, encryptedBytesWithSalt, 0, salt.length);
            System.arraycopy(iv.getIV(), 0, encryptedBytesWithSalt, salt.length, iv.getIV().length);
            System.arraycopy(encryptedBytes, 0, encryptedBytesWithSalt, salt.length + iv.getIV().length, encryptedBytes.length);

            return encryptedBytesWithSalt;
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Could not initialize " + algorithmType.algorithmName() + " algorithm", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid key", e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Invalid block size selected", e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] data, String password) {
        try {
            byte[] salt = Arrays.copyOfRange(data, 0, SALT_SIZE);
            if(encryptionMode.equals(EncryptionMode.ECB)) {
                // ECB mode does not use IV
                byte[] encryptedBytes = Arrays.copyOfRange(data, SALT_SIZE, data.length);

                var key = EncryptionUtils.deriveKey(algorithmType.algorithm(), password, salt, algorithmType.keySize());

                cipher.init(Cipher.DECRYPT_MODE, key);
                return cipher.doFinal(encryptedBytes);
            }
            byte[] ivBytes = Arrays.copyOfRange(data, SALT_SIZE, SALT_SIZE + algorithmType.ivSize());
            byte[] encryptedBytes = Arrays.copyOfRange(data, SALT_SIZE + algorithmType.ivSize(), data.length);

            var key = EncryptionUtils.deriveKey(algorithmType.algorithm(), password, salt, algorithmType.keySize());
            var iv = new IvParameterSpec(ivBytes);

            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            return cipher.doFinal(encryptedBytes);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Could not initialize " + algorithmType.algorithmName() + " algorithm", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid key", e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Invalid block size selected", e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
}
