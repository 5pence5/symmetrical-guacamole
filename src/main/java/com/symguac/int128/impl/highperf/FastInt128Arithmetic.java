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
}

