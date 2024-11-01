package ar.edu.itba.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public final class EncryptionUtils {
    private static final int ITERATION_COUNT = 65536; // Number of PBKDF2 iterations
    private static final String SECRET_KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";

    public static SecretKeySpec deriveKey(String algorithm, String password, byte[] salt, int keySize) throws NoSuchAlgorithmException {
        if(!(algorithm.equals("AES") || algorithm.equals("DES"))) {
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
