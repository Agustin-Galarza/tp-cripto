package ar.edu.itba.steganography;

import ar.edu.itba.steganography.exceptions.SecretTooLargeException;
import java.awt.image.BufferedImage;
import java.io.File;

public interface StegoCodec {
    /**
     * Encodes a secret message into a cover image
     * @param secret The secret message to encode. This must be the final version of the message, with any padding or transformation already applied.
     * @param coverImage The cover image to encode the secret message into.
     * @return The stego image with the secret message encoded.
     * @throws SecretTooLargeException If the secret message is too large to be encoded in the cover image.
     */
    BufferedImage encode(byte[] secret, BufferedImage coverImage)
        throws SecretTooLargeException;

    /**
     * Decodes a secret message from a stego image.
     * @param stegoImage The stego image to decode the secret message from.
     * @return The secret message decoded from the stego image.
     */
    byte[] decode(byte[] stegoImage);
}
