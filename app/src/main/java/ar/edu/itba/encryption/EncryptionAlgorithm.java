package ar.edu.itba.encryption;

import ar.edu.itba.config.EncryptionMode;

public interface EncryptionAlgorithm {
    byte[] encrypt(
        byte[] data,
        String key,
        EncryptionMode mode,
        String password
    );
    byte[] decrypt(
        byte[] data,
        String key,
        EncryptionMode mode,
        String password
    );
}
