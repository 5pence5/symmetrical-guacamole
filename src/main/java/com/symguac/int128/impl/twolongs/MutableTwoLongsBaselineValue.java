package com.symguac.int128.impl.twolongs;

import com.symguac.int128.api.Int128Value;
import com.symguac.int128.api.MutableInt128Value;

/**
 * Mutable counterpart to {@link TwoLongsBaselineValue}.
 */
public final class MutableTwoLongsBaselineValue implements MutableInt128Value {
    private long high;
    private long low;

    public MutableTwoLongsBaselineValue() {
        this(0L, 0L);
    }

    public MutableTwoLongsBaselineValue(long high, long low) {
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

    @Override
    public void set(long high, long low) {
        this.high = high;
        this.low = low;
    }

    @Override
    public Int128Value immutableCopy() {
        return new TwoLongsBaselineValue(high, low);
    }

    @Override
    public MutableInt128Value copy() {
        return new MutableTwoLongsBaselineValue(high, low);
    }
}
