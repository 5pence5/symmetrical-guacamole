package com.symguac.int128.api;

/**
 * Read-only view of a signed 128-bit integer value.
 */
public interface Int128Value {
    /**
     * Returns the most significant 64 bits of the two's-complement representation.
     */
    long high();

    /**
     * Returns the least significant 64 bits of the two's-complement representation.
     */
    long low();

    /**
     * Creates a human-readable hexadecimal representation that is convenient for debugging.
     */
    default String toHexString() {
        return String.format("0x%016X%016X", high(), low());
    }
}
