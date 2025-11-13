package com.symguac.int128.impl.twolongs;

import com.symguac.int128.api.Int128Value;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Immutable Int128 value backed by two 64-bit words. This class favours clarity over raw performance and serves as a
 * convenient reference implementation for benchmarking purposes.
 */
public final class TwoLongsBaselineValue implements Int128Value, Comparable<TwoLongsBaselineValue> {
    private static final BigInteger WORD_MASK = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

    private final long high;
    private final long low;

    public TwoLongsBaselineValue(long high, long low) {
        this.high = high;
        this.low = low;
    }

    @Override
    public long high() {
        return high;
    }

    @Override
    public long low() {
        return low;
    }

    public BigInteger toBigInteger() {
        BigInteger upper = BigInteger.valueOf(high);
        BigInteger lower = BigInteger.valueOf(low & 0xFFFFFFFFFFFFFFFFL);
        BigInteger combined = upper.shiftLeft(64).add(lower.and(WORD_MASK));
        return combined;
    }

    public static TwoLongsBaselineValue fromBigInteger(BigInteger value) {
        Objects.requireNonNull(value, "value");
        BigInteger normalized = value;
        BigInteger lowWord = normalized.and(WORD_MASK);
        BigInteger highWord = normalized.shiftRight(64);
        return new TwoLongsBaselineValue(highWord.longValue(), lowWord.longValue());
    }

    @Override
    public int compareTo(TwoLongsBaselineValue other) {
        int highCompare = Long.compare(this.high, other.high);
        if (highCompare != 0) {
            return highCompare;
        }
        return Long.compareUnsigned(this.low, other.low);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TwoLongsBaselineValue that)) {
            return false;
        }
        return this.high == that.high && this.low == that.low;
    }

    @Override
    public int hashCode() {
        return (int) (high ^ (high >>> 32) ^ low ^ (low >>> 32));
    }

    @Override
    public String toString() {
        return toBigInteger().toString();
    }
}
