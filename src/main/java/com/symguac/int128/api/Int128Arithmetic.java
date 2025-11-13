package com.symguac.int128.api;

/**
 * Contract that individual Int128 implementations must satisfy in order to participate in benchmarks.
 */
public interface Int128Arithmetic {

    /**
     * Unique identifier that is used by benchmark harness configuration.
     */
    String id();

    /**
     * Constructs a value from a pair of signed 64-bit words (high first, low second).
     */
    Int128Value fromParts(long high, long low);

    /**
     * Constructs a value from a 64-bit integer, sign-extending into the upper bits.
     */
    Int128Value fromLong(long value);

    /**
     * Creates a new mutable instance that can store intermediate results.
     */
    MutableInt128Value createMutable();

    /**
     * Adds {@code left} and {@code right} and stores the result into {@code destination}.
     */
    void addInto(Int128Value left, Int128Value right, MutableInt128Value destination);

    /**
     * Subtracts {@code right} from {@code left} and stores the result into {@code destination}.
     */
    void subtractInto(Int128Value left, Int128Value right, MutableInt128Value destination);

    /**
     * Multiplies {@code left} by {@code right} and stores the result into {@code destination}.
     */
    void multiplyInto(Int128Value left, Int128Value right, MutableInt128Value destination);

    /**
     * Returns an immutable value representing the sum of {@code left} and {@code right}.
     */
    default Int128Value add(Int128Value left, Int128Value right) {
        MutableInt128Value result = createMutable();
        addInto(left, right, result);
        return result.immutableCopy();
    }

    /**
     * Returns an immutable value representing the product of {@code left} and {@code right}.
     */
    default Int128Value multiply(Int128Value left, Int128Value right) {
        MutableInt128Value result = createMutable();
        multiplyInto(left, right, result);
        return result.immutableCopy();
    }
}
