package ar.edu.itba.config;

public enum EncryptionAlgorithmType {
    PLAIN_TEXT("Plain Text", null, 0, 0),
    AES128("AES128", "AES", 128, 16),
    AES192("AES192", "AES", 192, 16),
    AES256("AES256", "AES", 256, 16),
    _3DES("3DES", "DESede", 192, 8);

    private final String name;
    private final String algorithm;
    private final int ivSize;
    private final int keySize;
    EncryptionAlgorithmType(String name, String algorithm, int keySize, int ivSize) {
        this.name = name;
        this.algorithm = algorithm;
        this.keySize = keySize;
        this.ivSize = ivSize;
    }

    public String algorithmName() {
        return name;
    }
    public String algorithm() {
        return algorithm;
    }

    public int ivSize() {
        return ivSize;
    }

    public int keySize() {
        return keySize;
    }
}
