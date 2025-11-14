import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive property-based and adversarial tests for Int128 division.
 * Validates the fixes for robust underflow detection and ensures division semantics.
 */
public class Int128PropertyTest {

    private static Int128 rnd(Random r) {
        return Int128.of(r.nextLong(), r.nextLong());
    }

    /**
     * Test fundamental division identity: a = q*d + r
     * Test remainder bounds: |r| < |d| (unsigned magnitude)
     * Test remainder sign: sign(r) == sign(a) or r == 0
     */
    @Test
    public void identityAndBoundsRandom() {
        Random r = new Random(123);
        for (int i = 0; i < 20_000; i++) {
            Int128 a = rnd(r);
            Int128 d;
            do { d = rnd(r); } while (d.isZero());

            Int128[] dr = a.divRem(d);
            Int128 q = dr[0], rem = dr[1];

            // a = q*d + r
            assertEquals(a, q.mul(d).add(rem),
                "Identity failed for a=" + a.toDebugHex() + ", d=" + d.toDebugHex());

            // |r| < |d| (unsigned magnitude)
            assertTrue(rem.abs().compareUnsigned(d.abs()) < 0,
                "Remainder bounds violated: |r|=" + rem.abs().toDebugHex() +
                " >= |d|=" + d.abs().toDebugHex());

            // sign(r) == sign(a) or r == 0
            assertTrue(rem.isZero() || rem.signum() == a.signum(),
                "Remainder sign incorrect: rem.signum=" + rem.signum() +
                ", a.signum=" + a.signum());
        }
    }

    /**
     * Test the specific adversarial case that previously caused issues:
     * Division of large 128-bit values that exercise the underflow correction path.
     */
    @Test
    public void adversarialDiv128by128() {
        Int128 a = Int128.parseHex("0xFFFF000000000000FFFF000000000000");
        Int128 d = Int128.parseHex("0x0000FFFF00000000FFFFFFFF00000001");

        Int128[] dr = a.divRem(d);
        Int128 q = dr[0], r = dr[1];

        // Verify identity: a = q*d + r
        Int128 recomposed = q.mul(d).add(r);
        assertEquals(a, recomposed,
            "Adversarial case identity failed: a=" + a.toDebugHex() +
            ", q=" + q.toDebugHex() + ", d=" + d.toDebugHex() + ", r=" + r.toDebugHex());

        // Verify remainder bounds: |r| < |d|
        assertTrue(r.abs().compareUnsigned(d.abs()) < 0,
            "Adversarial case remainder bounds violated: |r|=" + r.abs().toDebugHex() +
            " >= |d|=" + d.abs().toDebugHex());
    }

    /**
     * Test division by powers of 10 (fast path for financial calculations).
     * Verifies divRemPow10 correctness and rounding modes.
     */
    @Test
    public void pow10Helpers() {
        // Test exact division for all powers 0..38
        for (int k = 0; k <= 38; k++) {
            Int128 x = Int128.tenPow(k).mul(Int128.valueOf(-37));
            Int128[] dr = x.divRemPow10(k);
            assertEquals(Int128.valueOf(-37), dr[0],
                "Power-10 division failed for k=" + k);
            assertTrue(dr[1].isZero(),
                "Power-10 division should have zero remainder for k=" + k);
        }

        // Banker's rounding (round half to even) sanity checks
        assertEquals(Int128.valueOf(2), Int128.valueOf(25).divRoundHalfEvenPow10(1),
            "25/10 banker's round should be 2");
        assertEquals(Int128.valueOf(2), Int128.valueOf(15).divRoundHalfEvenPow10(1),
            "15/10 banker's round should be 2 (tie to even)");
        assertEquals(Int128.valueOf(-2), Int128.valueOf(-25).divRoundHalfEvenPow10(1),
            "-25/10 banker's round should be -2");
        assertEquals(Int128.valueOf(-2), Int128.valueOf(-15).divRoundHalfEvenPow10(1),
            "-15/10 banker's round should be -2 (tie to even)");
    }

    /**
     * Test edge cases: MIN_VALUE, MAX_VALUE, and boundary conditions.
     */
    @Test
    public void edgeCases() {
        // MIN_VALUE / -1 wraps to MIN_VALUE (two's complement overflow)
        Int128 q1 = Int128.MIN_VALUE.div(Int128.valueOf(-1));
        assertEquals(Int128.MIN_VALUE, q1,
            "MIN_VALUE / -1 should wrap to MIN_VALUE");

        // MAX_VALUE / 1 = MAX_VALUE
        Int128 q2 = Int128.MAX_VALUE.div(Int128.ONE);
        assertEquals(Int128.MAX_VALUE, q2,
            "MAX_VALUE / 1 should equal MAX_VALUE");

        // 0 / anything = 0
        Int128 q3 = Int128.ZERO.div(Int128.valueOf(999));
        assertEquals(Int128.ZERO, q3,
            "0 / 999 should be 0");

        // Small / large = 0 with remainder = small
        Int128[] dr = Int128.valueOf(42).divRem(Int128.valueOf(100));
        assertEquals(Int128.ZERO, dr[0], "42 / 100 quotient should be 0");
        assertEquals(Int128.valueOf(42), dr[1], "42 / 100 remainder should be 42");
    }

    /**
     * Test unsigned division correctness.
     */
    @Test
    public void unsignedDivision() {
        Random r = new Random(456);
        for (int i = 0; i < 5000; i++) {
            Int128 a = rnd(r);
            Int128 d;
            do { d = rnd(r); } while (d.isZero());

            Int128 q = a.divideUnsigned(d);
            Int128 rem = a.remainderUnsigned(d);

            // Unsigned identity: a = q*d + r (all unsigned)
            Int128 recomposed = q.mul(d).add(rem);
            assertEquals(a, recomposed,
                "Unsigned identity failed for a=" + a.toDebugHex() + ", d=" + d.toDebugHex());

            // Unsigned remainder bounds: r < d (unsigned compare)
            assertTrue(rem.compareUnsigned(d) < 0,
                "Unsigned remainder >= divisor: r=" + rem.toDebugHex() + ", d=" + d.toDebugHex());
        }
    }

    /**
     * Test that division by zero throws ArithmeticException.
     */
    @Test
    public void divisionByZeroThrows() {
        assertThrows(ArithmeticException.class, () -> {
            Int128.valueOf(42).div(Int128.ZERO);
        }, "Division by zero should throw ArithmeticException");

        assertThrows(ArithmeticException.class, () -> {
            Int128.valueOf(42).divRem(Int128.ZERO);
        }, "divRem by zero should throw ArithmeticException");

        assertThrows(ArithmeticException.class, () -> {
            Int128.valueOf(42).divideUnsigned(Int128.ZERO);
        }, "divideUnsigned by zero should throw ArithmeticException");

        assertThrows(ArithmeticException.class, () -> {
            Int128.valueOf(42).remainderUnsigned(Int128.ZERO);
        }, "remainderUnsigned by zero should throw ArithmeticException");
    }

    /**
     * Test specific corner cases that exercise the 128/64 fast path.
     */
    @Test
    public void div128by64FastPath() {
        // Large dividend, 64-bit divisor (exercises fast path)
        Int128 a = Int128.parseHex("0x123456789ABCDEF0FEDCBA9876543210");
        Int128 d = Int128.valueOf(1000000000L); // 10^9, fits in 64 bits

        Int128[] dr = a.divRem(d);
        Int128 q = dr[0], r = dr[1];

        // Verify identity
        assertEquals(a, q.mul(d).add(r),
            "128/64 fast path identity failed");

        // Verify bounds
        assertTrue(r.abs().compareUnsigned(d.abs()) < 0,
            "128/64 fast path remainder bounds violated");
    }

    /**
     * Test that quickSelfCheck passes (integration smoke test).
     */
    @Test
    public void quickSelfCheckPasses() {
        assertDoesNotThrow(() -> Int128.quickSelfCheck(),
            "quickSelfCheck should pass without errors");
    }
}
