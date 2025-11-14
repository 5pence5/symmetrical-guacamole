package com.symguac.int128.impl.highperf;

import com.symguac.int128.impl.twolongs.TwoLongsBaselineValue;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FastInt128ValueAdditionalTest {

    @Test
    void sign_and_abs_boundary_2pow63() {
        FastInt128Value x = FastInt128Value.of(0L, Long.MIN_VALUE); // 2^63
        assertEquals(1, x.signum());
        assertFalse(x.isNegative());
        assertTrue(x.isPositive());
        assertSame(x, x.abs());
        assertEquals("9223372036854775808", x.toString());
    }

    @Test
    void shiftRight_ge128_behaviour() {
        FastInt128Value neg = FastInt128Value.MIN_VALUE; // (-2^127)
        FastInt128Value pos = FastInt128Value.ONE;
        assertEquals(FastInt128Value.MIN_VALUE, neg.shiftRight(128));
        assertEquals(FastInt128Value.ZERO, pos.shiftRight(128));
    }

    @Test
    void toDecimalLimbs_roundtrip() {
        FastInt128Value x = FastInt128Value.of(0L, Long.MIN_VALUE); // 2^63
        int[] limbs = x.toDecimalLimbs();
        FastInt128Value y = FastInt128Value.fromDecimalLimbs(limbs, false);
        assertEquals(x, y);
    }

    @Test
    void random_roundtrip_vs_BigInteger() {
        Random r = new Random(1);
        for (int i = 0; i < 1000; i++) {
            long hi = r.nextLong();
            long lo = r.nextLong();
            FastInt128Value x = FastInt128Value.of(hi, lo);

            TwoLongsBaselineValue baseline = new TwoLongsBaselineValue(hi, lo);
            assertEquals(baseline.toString(), x.toString());
        }
    }
}
