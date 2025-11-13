package com.symguac.int128.api;

/**
 * Mutable variant of {@link Int128Value} that allows benchmarks to avoid intermediate allocations.
 */
public interface MutableInt128Value extends Int128Value {

    /**
     * Assigns the value represented by the pair of signed 64-bit words.
     */
    void set(long high, long low);

    /**
     * Returns an immutable snapshot of the current state.
     */
    Int128Value immutableCopy();

    /**
     * Produces a mutable copy that can be safely modified without affecting the original instance.
     */
    MutableInt128Value copy();
}
