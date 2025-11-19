package com.symguac.int128.impl.highperf;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class FastInt128ValueTest {

    @Test
    void tenPowTableProvidesExpectedValues() {
        assertEquals(FastInt128Value.ONE, FastInt128Value.tenPow(0));
        assertEquals(FastInt128Value.of(0L, 10L), FastInt128Value.tenPow(1));
        assertEquals("1000", FastInt128Value.tenPow(3).toString());
        assertEquals("100000000000000000000", FastInt128Value.tenPow(20).toString());
        assertEquals("100000000000000000000000000000000000000", FastInt128Value.tenPow(38).toString());
    }

    @Test
    void tenPowRejectsOutOfRangeExponents() {
        assertThrows(IllegalArgumentException.class, () -> FastInt128Value.tenPow(-1));
        assertThrows(IllegalArgumentException.class, () -> FastInt128Value.tenPow(39));
    }

    @Test
    void tenPowTableMatchesBigIntegerAcrossRange() {
        for (int i = 0; i <= 38; i++) {
            BigInteger expected = BigInteger.TEN.pow(i);
            FastInt128Value actual = FastInt128Value.tenPow(i);
            assertEquals(expected.toString(), actual.toString(), "Mismatch at power " + i);
        }
    }

    @Test
    void isMultipleOfPowerOfTenUsesTableDivisibility() {
        FastInt128Value thousand = FastInt128Value.fromLong(1_000L);
        assertTrue(thousand.isMultipleOfPowerOfTen(3));
        assertFalse(thousand.isMultipleOfPowerOfTen(4));

        FastInt128Value hugePower = FastInt128Value.fromString("100000000000000000000"); // 10^20
        assertTrue(hugePower.isMultipleOfPowerOfTen(20));
        assertFalse(hugePower.isMultipleOfPowerOfTen(21));

        FastInt128Value negative = FastInt128Value.fromLong(-10_000L);
        assertTrue(negative.isMultipleOfPowerOfTen(4));
    }
}
