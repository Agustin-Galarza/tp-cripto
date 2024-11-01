package ar.edu.itba.utils;

import ar.edu.itba.config.EncryptionMode;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public final class EncryptionUtils {
    private static final int ITERATION_COUNT = 65536; // Number of PBKDF2 iterations
    private static final String SECRET_KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final PaddingScheme DEFAULT_PADDING = PaddingScheme.PKCS5;

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

    public static SecretKeySpec deriveKey(String algorithm, String password, byte[] salt, int keySize) throws NoSuchAlgorithmException {
        if(!(algorithm.equals("AES") || algorithm.equals("DESede"))) {
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
