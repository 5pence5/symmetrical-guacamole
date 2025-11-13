package com.symguac.int128.impl.twolongs;

import com.symguac.int128.api.Int128Arithmetic;
import com.symguac.int128.api.Int128Value;
import com.symguac.int128.api.MutableInt128Value;
import com.symguac.int128.bench.Int128ImplementationIds;

import java.math.BigInteger;

/**
 * Straightforward Int128 implementation that favours correctness over raw throughput. It is primarily useful as a
 * baseline when evaluating faster alternatives.
 */
public final class TwoLongsBaselineArithmetic implements Int128Arithmetic {
    private static final BigInteger MASK_64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
    private static final BigInteger MASK_128 = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);

    @Override
    public String id() {
        return Int128ImplementationIds.TWO_LONGS_BASELINE;
    }

    @Override
    public Int128Value fromParts(long high, long low) {
        return new TwoLongsBaselineValue(high, low);
    }

    @Override
    public Int128Value fromLong(long value) {
        long high = value < 0 ? -1L : 0L;
        return new TwoLongsBaselineValue(high, value);
    }

    @Override
    public MutableInt128Value createMutable() {
        return new MutableTwoLongsBaselineValue();
    }

    @Override
    public void addInto(Int128Value left, Int128Value right, MutableInt128Value destination) {
        long leftLow = left.low();
        long rightLow = right.low();
        long low = leftLow + rightLow;
        long carry = Long.compareUnsigned(low, leftLow) < 0 ? 1L : 0L;
        long high = left.high() + right.high() + carry;
        destination.set(high, low);
    }

    @Override
    public void subtractInto(Int128Value left, Int128Value right, MutableInt128Value destination) {
        long leftLow = left.low();
        long rightLow = right.low();
        long low = leftLow - rightLow;
        long borrow = Long.compareUnsigned(leftLow, rightLow) < 0 ? 1L : 0L;
        long high = left.high() - right.high() - borrow;
        destination.set(high, low);
    }

    @Override
    public void multiplyInto(Int128Value left, Int128Value right, MutableInt128Value destination) {
        BigInteger product = toBigInteger(left).multiply(toBigInteger(right)).and(MASK_128);
        long high = product.shiftRight(64).longValue();
        long low = product.and(MASK_64).longValue();
        destination.set(high, low);
    }

    @Override
    public void divideRemainderInto(Int128Value dividend, Int128Value divisor,
                                     MutableInt128Value quotient, MutableInt128Value remainder) {
        if (divisor.high() == 0L && divisor.low() == 0L) {
            throw new ArithmeticException("divide by zero");
        }

        BigInteger a = toBigInteger(dividend);
        BigInteger b = toBigInteger(divisor);
        BigInteger q = a.divide(b).and(MASK_128);
        BigInteger r = a.remainder(b).and(MASK_128);

        long qHigh = q.shiftRight(64).longValue();
        long qLow = q.and(MASK_64).longValue();
        long rHigh = r.shiftRight(64).longValue();
        long rLow = r.and(MASK_64).longValue();

        quotient.set(qHigh, qLow);
        remainder.set(rHigh, rLow);
    }

    private static BigInteger toBigInteger(Int128Value value) {
        BigInteger upper = BigInteger.valueOf(value.high());
        BigInteger lower = BigInteger.valueOf(value.low()).and(MASK_64);
        return upper.shiftLeft(64).add(lower);
    }
}
