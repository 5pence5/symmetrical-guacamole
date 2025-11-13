package com.symguac.int128.impl.highperf;

import com.symguac.int128.api.Int128Arithmetic;
import com.symguac.int128.api.Int128Value;
import com.symguac.int128.api.MutableInt128Value;
import com.symguac.int128.bench.Int128ImplementationIds;

/**
 * High-performance arithmetic provider backed by {@link FastInt128Value}. The implementation aggressively inlines the
 * hot-path operations to minimise branching and intermediate allocations.
 */
public final class FastInt128Arithmetic implements Int128Arithmetic {

    @Override
    public String id() {
        return Int128ImplementationIds.FAST_LIMB_BASED;
    }

    @Override
    public Int128Value fromParts(long high, long low) {
        return FastInt128Value.of(high, low);
    }

    @Override
    public Int128Value fromLong(long value) {
        return FastInt128Value.fromLong(value);
    }

    @Override
    public MutableInt128Value createMutable() {
        return new FastInt128Value.MutableFastInt128Value();
    }

    @Override
    public void addInto(Int128Value left, Int128Value right, MutableInt128Value destination) {
        if (destination instanceof FastInt128Value.MutableFastInt128Value mutable) {
            long leftLow = left.low();
            long rightLow = right.low();
            long low = leftLow + rightLow;
            long carry = (Long.compareUnsigned(low, leftLow) < 0) ? 1L : 0L;
            long high = left.high() + right.high() + carry;
            mutable.set(high, low);
            return;
        }
        FastInt128Value.MathOps.addInto(left, right, destination);
    }

    @Override
    public void subtractInto(Int128Value left, Int128Value right, MutableInt128Value destination) {
        if (destination instanceof FastInt128Value.MutableFastInt128Value mutable) {
            long leftLow = left.low();
            long rightLow = right.low();
            long low = leftLow - rightLow;
            long borrow = (Long.compareUnsigned(leftLow, rightLow) < 0) ? 1L : 0L;
            long high = left.high() - right.high() - borrow;
            mutable.set(high, low);
            return;
        }
        FastInt128Value.MathOps.subtractInto(left, right, destination);
    }

    @Override
    public void multiplyInto(Int128Value left, Int128Value right, MutableInt128Value destination) {
        long[] product = FastInt128Value.multiplyUnsigned128(left.high(), left.low(), right.high(), right.low());
        destination.set(product[0], product[1]);
    }

    @Override
    public void divideRemainderInto(Int128Value dividend, Int128Value divisor,
                                     MutableInt128Value quotient, MutableInt128Value remainder) {
        if (divisor.high() == 0L && divisor.low() == 0L) {
            throw new ArithmeticException("divide by zero");
        }

        // Implement inline division using Knuth's Algorithm D
        long aHi = dividend.high(), aLo = dividend.low();
        long bHi = divisor.high(), bLo = divisor.low();

        boolean negA = aHi < 0;
        boolean negB = bHi < 0;
        boolean negQ = negA ^ negB;
        boolean negR = negA;

        // Get magnitudes
        if (negA) {
            long[] t = negate128(aHi, aLo);
            aHi = t[0]; aLo = t[1];
        }
        if (negB) {
            long[] t = negate128(bHi, bLo);
            bHi = t[0]; bLo = t[1];
        }

        long qHi, qLo, rHi, rLo;

        // Quick path: if a < b, quotient=0, remainder=a
        if (compareUnsigned128(aHi, aLo, bHi, bLo) < 0) {
            qHi = 0L; qLo = 0L; rHi = aHi; rLo = aLo;
        } else if (bHi == 0L) {
            // Fast path: 128/64 division
            long[] qr = divRem128by64(aHi, aLo, bLo);
            qHi = qr[0]; qLo = qr[1]; rHi = 0L; rLo = qr[2];
        } else {
            // Full 128/128 division - use simple bit-by-bit for now
            // TODO: Implement optimized Knuth algorithm
            throw new UnsupportedOperationException(
                "128-bit division not yet implemented in FastInt128Arithmetic. " +
                "Use TwoLongsBaseline for division benchmarks, or port division from Int128.java");
        }

        // Restore signs
        if (negQ && (qHi != 0L || qLo != 0L)) {
            long[] t = negate128(qHi, qLo);
            qHi = t[0]; qLo = t[1];
        }
        if (negR && (rHi != 0L || rLo != 0L)) {
            long[] t = negate128(rHi, rLo);
            rHi = t[0]; rLo = t[1];
        }

        quotient.set(qHi, qLo);
        remainder.set(rHi, rLo);
    }

    private static long[] negate128(long hi, long lo) {
        long loN = ~lo + 1L;
        long carry = (loN == 0L) ? 1L : 0L;
        long hiN = ~hi + carry;
        return new long[] { hiN, loN };
    }

    private static int compareUnsigned128(long aHi, long aLo, long bHi, long bLo) {
        int ch = Long.compareUnsigned(aHi, bHi);
        if (ch != 0) return ch;
        return Long.compareUnsigned(aLo, bLo);
    }

    private static long[] divRem128by64(long aHi, long aLo, long divisor) {
        if (divisor == 0L) throw new ArithmeticException("divide by zero");

        long qHi = Long.divideUnsigned(aHi, divisor);
        long r = Long.remainderUnsigned(aHi, divisor);

        // Divide (r:aLo) by divisor using bit-by-bit
        long qLo = 0L;
        for (int i = 63; i >= 0; i--) {
            long bit = (aLo >>> i) & 1L;
            long r2 = (r << 1) | bit;
            if (r < 0L || Long.compareUnsigned(r2, divisor) >= 0) {
                r = r2 - divisor;
                qLo |= (1L << i);
            } else {
                r = r2;
            }
        }

        return new long[] { qHi, qLo, r };
    }
}

