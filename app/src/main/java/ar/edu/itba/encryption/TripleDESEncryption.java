package ar.edu.itba.encryption;

import ar.edu.itba.config.EncryptionMode;

public class TripleDESEncryption implements EncryptionAlgorithm {
    @Override
    public byte[] encrypt(byte[] data, EncryptionMode mode, String password) {
        return new byte[0];
    }

    @Override
    public byte[] decrypt(byte[] data, EncryptionMode mode, String password) {
        return new byte[0];
    }
}
