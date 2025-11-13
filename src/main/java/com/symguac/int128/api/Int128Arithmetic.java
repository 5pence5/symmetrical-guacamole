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
     * Divides {@code dividend} by {@code divisor} and stores quotient and remainder into the destinations.
     *
     * @param dividend the value to be divided
     * @param divisor the value to divide by (must not be zero)
     * @param quotient destination for the quotient
     * @param remainder destination for the remainder
     * @throws ArithmeticException if divisor is zero
     */
    default void divideRemainderInto(Int128Value dividend, Int128Value divisor,
                                      MutableInt128Value quotient, MutableInt128Value remainder) {
        throw new UnsupportedOperationException("Division not implemented by " + id());
    }

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

    /**
     * Returns an immutable value representing the quotient of {@code dividend} divided by {@code divisor}.
     *
     * @throws ArithmeticException if divisor is zero
     */
    default Int128Value divide(Int128Value dividend, Int128Value divisor) {
        MutableInt128Value quotient = createMutable();
        MutableInt128Value remainder = createMutable();
        divideRemainderInto(dividend, divisor, quotient, remainder);
        return quotient.immutableCopy();
    }

    /**
     * Returns an immutable value representing the remainder of {@code dividend} divided by {@code divisor}.
     *
     * @throws ArithmeticException if divisor is zero
     */
    default Int128Value remainder(Int128Value dividend, Int128Value divisor) {
        MutableInt128Value quotient = createMutable();
        MutableInt128Value remainder = createMutable();
        divideRemainderInto(dividend, divisor, quotient, remainder);
        return remainder.immutableCopy();
    }
}
