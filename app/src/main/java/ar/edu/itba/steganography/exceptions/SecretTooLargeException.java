package ar.edu.itba.steganography.exceptions;

public class SecretTooLargeException extends Exception {

    public SecretTooLargeException(long maxBytes, long actualBytes) {
        super(
            "The secret is too large. The maximum allowed size for the secret with the selected cover image is " +
            maxBytes +
            " bytes, but the actual size of the secret is " +
            actualBytes +
            " bytes."
        );
    }
}
