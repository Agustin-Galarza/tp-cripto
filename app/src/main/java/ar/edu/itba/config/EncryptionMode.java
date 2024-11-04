package ar.edu.itba.config;

public enum EncryptionMode {
    ECB("ECB"),
    CBC("CBC"),
    CFB("CFB8"),
    OFB("OFB");

    private final String mode;
    EncryptionMode(String mode) {
        this.mode = mode;
    }

    public String value() {
        return mode;
    }
}
