package com.symguac.int128.impl.highperf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class FastInt128ValueSignTest {

    @Test
    void signRelatedHelpersTreatHighZeroWithNegativeLowAsPositive() {
        FastInt128Value value = FastInt128Value.of(0L, Long.MIN_VALUE);

        assertEquals(1, value.signum());
        assertFalse(value.isNegative());
        assertSame(value, value.abs());

        assertEquals("9223372036854775808", value.toString());
        assertEquals(value, FastInt128Value.fromString("9223372036854775808"));

        int[] limbs = value.toDecimalLimbs();
        FastInt128Value reconstructed = FastInt128Value.fromDecimalLimbs(limbs, false);
        assertEquals(value, reconstructed);
    }

    @Test
    void mutableAbsoluteValueRetainsPositiveMagnitude() {
        FastInt128Value.MutableFastInt128Value mutable =
                new FastInt128Value.MutableFastInt128Value(0L, Long.MIN_VALUE);

        mutable.absoluteValue();

        assertEquals(0L, mutable.high());
        assertEquals(Long.MIN_VALUE, mutable.low());
    }
}

