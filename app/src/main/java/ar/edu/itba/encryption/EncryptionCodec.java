package ar.edu.itba.encryption;

import ar.edu.itba.config.EncryptionAlgorithmType;
import ar.edu.itba.config.EncryptionMode;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import ar.edu.itba.utils.EnvUtils;


public class EncryptionCodec implements EncryptionAlgorithm {
    private static final int ITERATION_COUNT;
    private static final int DEFAULT_ITERATION_COUNT = 10000;
    private static final String SECRET_KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final PaddingScheme DEFAULT_PADDING = PaddingScheme.PKCS5;
    private static final int SALT_SIZE = Long.SIZE / 8;
    private static final long SALT;
    private static final long DEFAULT_SALT = 0x0000000000000000;

    static {
        SALT = EnvUtils.getLong("SALT", v -> Long.parseLong(v.substring(2)), DEFAULT_SALT);
        ITERATION_COUNT = EnvUtils.getInt("KEY_ITERATIONS", DEFAULT_ITERATION_COUNT);
    }

    private final EncryptionAlgorithmType algorithmType;
    private final EncryptionMode encryptionMode;
    private final Cipher cipher;

    public EncryptionCodec(EncryptionAlgorithmType algorithmType, EncryptionMode mode) {
        if (algorithmType.equals(EncryptionAlgorithmType.PLAIN_TEXT)) {
            throw new IllegalArgumentException("PLAIN TEXT encryption is not a valid algorithm");
        }
        this.algorithmType = algorithmType;
        this.encryptionMode = mode;
        try {
            this.cipher = Cipher.getInstance(buildTransformation(algorithmType.algorithm(), mode));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] getSalt() {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(SALT);
        return buffer.array();
    }

    @Override
    public byte[] encrypt(byte[] data, String password) {
        try {
            byte[] salt = getSalt();

            var key = deriveKey(algorithmType.algorithm(), password, salt, algorithmType.keySize());
            if (encryptionMode.equals(EncryptionMode.ECB)) {
                // ECB mode does not use IV
                cipher.init(Cipher.ENCRYPT_MODE, key);
                return cipher.doFinal(data);
            }
            var iv = deriveIV(password, salt, algorithmType.ivSize());

            // Encrypt the text
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            return cipher.doFinal(data);
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
            byte[] salt = getSalt();
            var key = deriveKey(algorithmType.algorithm(), password, salt, algorithmType.keySize());
            if (encryptionMode.equals(EncryptionMode.ECB)) {
                // ECB mode does not use IV

                cipher.init(Cipher.DECRYPT_MODE, key);
                return cipher.doFinal(data);
            }

            var iv = deriveIV(password, salt, algorithmType.ivSize());

            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            return cipher.doFinal(data);
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

    public enum PaddingScheme {
        PKCS5("PKCS5Padding"),  //	Pads with bytes equal to the number of padding bytes added (common for block ciphers).
        PKCS7("PKCS7Padding"),  //	Same as PKCS5, but for larger block sizes.
        NoPadding("NoPadding"),  //	No padding; plaintext must match the block size.
        ANSI_X923("ANSI_X923Padding"),  //	Pads with zero bytes, last byte indicates number of padding bytes added.
        ISO10126("ISO10126Padding"),  //	Pads with random bytes, last byte indicates number of padding bytes added.
        Zero("ZeroPadding");  //	Pads with zero bytes (not recommended due to potential ambiguity).

        private final String value;

        PaddingScheme(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public static String buildTransformation(String algorithm, EncryptionMode mode, PaddingScheme padding) {
        return algorithm + "/" + mode.value() + "/" + padding.value();
    }

    public static String buildTransformation(String algorithm, EncryptionMode mode) {
        return algorithm + "/" + mode.value() + "/" + DEFAULT_PADDING.value();
    }

    public static SecretKeySpec deriveKey(String algorithm, String password, byte[] salt, int keySize) throws
      NoSuchAlgorithmException {
        if (!(algorithm.equals("AES") || algorithm.equals("DESede"))) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, keySize);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(SECRET_KEY_FACTORY_ALGORITHM);
        try {
            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            return new SecretKeySpec(keyBytes, algorithm);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Invalid key spec", e);
        }
    }

    public static IvParameterSpec deriveIV(String password, byte[] salt, int ivSize) throws NoSuchAlgorithmException {
        PBEKeySpec ivSpec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, ivSize * 8);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(SECRET_KEY_FACTORY_ALGORITHM);
        try {
            byte[] ivBytes = keyFactory.generateSecret(ivSpec).getEncoded();
            return new IvParameterSpec(ivBytes);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Invalid key spec", e);
        }
    }
}
