import java.util.Random;

/**
 * Comprehensive test suite for Int128 division correctness.
 *
 * This test suite verifies:
 * 1. Division identity: q*d + r == a
 * 2. Remainder bounds: |r| < |d|
 * 3. Remainder sign: sign(r) == sign(a) (truncating division)
 * 4. Near-boundary divisors
 * 5. Adversarial dividend/divisor combinations
 * 6. Powers of 10
 * 7. Unsigned division consistency
 * 8. Rounding correctness
 */
public class Int128DivisionTest {

    private static int passCount = 0;
    private static int failCount = 0;
    private static final Random rand = new Random(42L);

    public static void main(String[] args) {
        System.out.println("=== Int128 Division Correctness Test Suite ===\n");

        // Run all test categories
        testDivisionIdentity();
        testNearBoundaryDivisors();
        testAdversarialDividends();
        testPowersOfTen();
        testUnsignedDivision();
        testRounding();
        testSpecificBugCases();

        // Summary
        System.out.println("\n=== Test Summary ===");
        System.out.println("Passed: " + passCount);
        System.out.println("Failed: " + failCount);

        if (failCount == 0) {
            System.out.println("\n✓ All tests passed!");
            System.exit(0);
        } else {
            System.out.println("\n✗ Some tests failed!");
            System.exit(1);
        }
    }

    // =========================================================================
    // Test 1: Division Identity (q*d + r == a)
    // =========================================================================

    private static void testDivisionIdentity() {
        System.out.println("Test 1: Division Identity (10,000 random pairs)");

        for (int i = 0; i < 10_000; i++) {
            Int128 a = randomInt128();
            Int128 d = randomNonZeroInt128();

            try {
                Int128[] qr = a.divRem(d);
                Int128 q = qr[0];
                Int128 r = qr[1];

                // Identity: q*d + r == a
                Int128 recomposed = q.mul(d).add(r);
                if (!recomposed.equals(a)) {
                    fail("Identity failed: a=" + a.toDebugHex() + ", d=" + d.toDebugHex() +
                         ", q=" + q.toDebugHex() + ", r=" + r.toDebugHex() +
                         ", q*d+r=" + recomposed.toDebugHex());
                    continue;
                }

                // Remainder bounds: |r| < |d|
                if (r.abs().compareUnsigned(d.abs()) >= 0) {
                    fail("Remainder bounds failed: |r| >= |d|, r=" + r.toDebugHex() + ", d=" + d.toDebugHex());
                    continue;
                }

                // Remainder sign: sign(r) == sign(a) or r == 0 (truncating division)
                if (!r.isZero() && (r.signum() != a.signum())) {
                    fail("Remainder sign failed: sign(r) != sign(a), r=" + r.toDebugHex() +
                         " (sign " + r.signum() + "), a=" + a.toDebugHex() + " (sign " + a.signum() + ")");
                    continue;
                }

                pass();
            } catch (Exception e) {
                fail("Exception: a=" + a.toDebugHex() + ", d=" + d.toDebugHex() + ", error=" + e.getMessage());
            }
        }

        System.out.println("  Completed: " + passCount + " passed\n");
    }

    // =========================================================================
    // Test 2: Near-Boundary Divisors
    // =========================================================================

    private static void testNearBoundaryDivisors() {
        System.out.println("Test 2: Near-Boundary Divisors");

        // Divisors with high bits set and awkward low bits
        String[] divisors = {
            "0x80000000000000010000000000000001",
            "0xFFFFFFFFFFFFFFFF0000000000000001",
            "0x0000FFFF00000000FFFFFFFF00000001",
            "0x7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
            "0x80000000000000000000000000000000",
            "0x00000000000000010000000000000000",
            "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
        };

        for (String divHex : divisors) {
            Int128 d = Int128.parseHex(divHex);

            // Test with several dividends
            Int128[] dividends = {
                Int128.MAX_VALUE,
                Int128.MIN_VALUE,
                Int128.parseHex("0xFFFF000000000000FFFF000000000000"),
                d.mul(Int128.valueOf(100)),
                d.mul(Int128.valueOf(-50))
            };

            for (Int128 a : dividends) {
                try {
                    Int128[] qr = a.divRem(d);
                    Int128 q = qr[0];
                    Int128 r = qr[1];

                    Int128 recomposed = q.mul(d).add(r);
                    if (!recomposed.equals(a)) {
                        fail("Boundary divisor identity failed: a=" + a.toDebugHex() +
                             ", d=" + d.toDebugHex());
                        continue;
                    }

                    pass();
                } catch (Exception e) {
                    fail("Boundary divisor exception: a=" + a.toDebugHex() +
                         ", d=" + d.toDebugHex() + ", error=" + e.getMessage());
                }
            }
        }

        System.out.println("  Completed\n");
    }

    // =========================================================================
    // Test 3: Adversarial Dividends
    // =========================================================================

    private static void testAdversarialDividends() {
        System.out.println("Test 3: Adversarial Dividends");

        Int128[] dividends = {
            Int128.MAX_VALUE,
            Int128.MIN_VALUE,
            Int128.MIN_VALUE.add(Int128.ONE),
            Int128.MIN_VALUE.sub(Int128.ONE),
            Int128.parseHex("0xFFFF000000000000FFFF000000000000"),
            Int128.parseHex("0x0000FFFF000000000000FFFF00000000")
        };

        Int128[] divisors = {
            Int128.valueOf(2),
            Int128.valueOf(-3),
            Int128.valueOf(1000),
            Int128.parseHex("0x10000000000000000000000000000001"),
            Int128.parseHex("0x7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")
        };

        for (Int128 a : dividends) {
            for (Int128 d : divisors) {
                try {
                    Int128[] qr = a.divRem(d);
                    Int128 q = qr[0];
                    Int128 r = qr[1];

                    Int128 recomposed = q.mul(d).add(r);
                    if (!recomposed.equals(a)) {
                        fail("Adversarial identity failed: a=" + a.toDebugHex() +
                             ", d=" + d.toDebugHex());
                        continue;
                    }

                    pass();
                } catch (Exception e) {
                    fail("Adversarial exception: a=" + a.toDebugHex() +
                         ", d=" + d.toDebugHex() + ", error=" + e.getMessage());
                }
            }
        }

        System.out.println("  Completed\n");
    }

    // =========================================================================
    // Test 4: Powers of 10
    // =========================================================================

    private static void testPowersOfTen() {
        System.out.println("Test 4: Powers of 10 (division by 10^k)");

        for (int k = 0; k <= 38; k++) {
            Int128 d = Int128.tenPow(k);

            // Test several dividends
            for (int i = 0; i < 100; i++) {
                Int128 a = randomInt128();

                try {
                    Int128[] qr = a.divRem(d);
                    Int128 q = qr[0];
                    Int128 r = qr[1];

                    Int128 recomposed = q.mul(d).add(r);
                    if (!recomposed.equals(a)) {
                        fail("Power of 10 identity failed: a=" + a.toDebugHex() +
                             ", d=10^" + k);
                        continue;
                    }

                    // Also test divRemPow10 fast path
                    Int128[] qr2 = a.divRemPow10(k);
                    if (!qr2[0].equals(q) || !qr2[1].equals(r)) {
                        fail("divRemPow10 mismatch for k=" + k + ", a=" + a.toDebugHex());
                        continue;
                    }

                    pass();
                } catch (Exception e) {
                    fail("Power of 10 exception: a=" + a.toDebugHex() +
                         ", k=" + k + ", error=" + e.getMessage());
                }
            }
        }

        System.out.println("  Completed\n");
    }

    // =========================================================================
    // Test 5: Unsigned Division
    // =========================================================================

    private static void testUnsignedDivision() {
        System.out.println("Test 5: Unsigned Division Consistency");

        for (int i = 0; i < 5_000; i++) {
            Int128 a = randomInt128();
            Int128 d = randomNonZeroInt128();

            // Only test when a >= d (unsigned)
            if (a.compareUnsigned(d) < 0) continue;

            try {
                Int128 q = a.divideUnsigned(d);
                Int128 r = a.remainderUnsigned(d);

                // Identity: q*d + r == a (unsigned)
                Int128 recomposed = q.mul(d).add(r);
                if (!recomposed.equals(a)) {
                    fail("Unsigned identity failed: a=" + a.toDebugHex() +
                         ", d=" + d.toDebugHex());
                    continue;
                }

                // Remainder bounds: r < d (unsigned)
                if (r.compareUnsigned(d) >= 0) {
                    fail("Unsigned remainder bounds failed: r >= d, r=" + r.toDebugHex() +
                         ", d=" + d.toDebugHex());
                    continue;
                }

                pass();
            } catch (Exception e) {
                fail("Unsigned exception: a=" + a.toDebugHex() +
                     ", d=" + d.toDebugHex() + ", error=" + e.getMessage());
            }
        }

        System.out.println("  Completed\n");
    }

    // =========================================================================
    // Test 6: Rounding
    // =========================================================================

    private static void testRounding() {
        System.out.println("Test 6: Rounding Correctness");

        // Test half-even rounding for powers of 10
        testCase(Int128.valueOf(15), 1, Int128.valueOf(2));  // 15/10 rounds to 2 (to even)
        testCase(Int128.valueOf(25), 1, Int128.valueOf(2));  // 25/10 rounds to 2 (to even)
        testCase(Int128.valueOf(35), 1, Int128.valueOf(4));  // 35/10 rounds to 4 (to even)
        testCase(Int128.valueOf(16), 1, Int128.valueOf(2));  // 16/10 rounds to 2 (> 0.5)
        testCase(Int128.valueOf(14), 1, Int128.valueOf(1));  // 14/10 rounds to 1 (< 0.5)

        testCase(Int128.valueOf(-15), 1, Int128.valueOf(-2)); // -15/10 rounds to -2 (to even)
        testCase(Int128.valueOf(-25), 1, Int128.valueOf(-2)); // -25/10 rounds to -2 (to even)

        System.out.println("  Completed\n");
    }

    private static void testCase(Int128 a, int exp, Int128 expected) {
        try {
            Int128 result = a.divRoundHalfEvenPow10(exp);
            if (!result.equals(expected)) {
                fail("Rounding failed: " + a + " / 10^" + exp + " = " + result +
                     ", expected " + expected);
            } else {
                pass();
            }
        } catch (Exception e) {
            fail("Rounding exception: a=" + a + ", exp=" + exp + ", error=" + e.getMessage());
        }
    }

    // =========================================================================
    // Test 7: Specific Bug Cases from the Review
    // =========================================================================

    private static void testSpecificBugCases() {
        System.out.println("Test 7: Specific Bug Cases from Review Report");

        // The case that was commented out in quickSelfCheck
        Int128 a = Int128.parseHex("0xFFFF000000000000FFFF000000000000");
        Int128 d = Int128.parseHex("0x0000FFFF00000000FFFFFFFF00000001");

        try {
            Int128[] dr = a.divRem(d);
            Int128 recomposed = dr[0].mul(d).add(dr[1]);
            if (!recomposed.equals(a)) {
                fail("Commented-out test case failed: a = q*d + r identity broken");
            } else {
                System.out.println("  ✓ Previously problematic case now works!");
                pass();
            }
        } catch (Exception e) {
            fail("Commented-out test case threw exception: " + e.getMessage());
        }

        // Additional cases that might trigger the bug
        testSpecificCase(
            "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
            "0x80000000000000000000000000000001"
        );

        testSpecificCase(
            "0x80000000000000000000000000000000",
            "0x00000000000000010000000000000001"
        );

        System.out.println("  Completed\n");
    }

    private static void testSpecificCase(String aHex, String dHex) {
        Int128 a = Int128.parseHex(aHex);
        Int128 d = Int128.parseHex(dHex);

        try {
            Int128[] qr = a.divRem(d);
            Int128 recomposed = qr[0].mul(d).add(qr[1]);
            if (!recomposed.equals(a)) {
                fail("Specific case failed: a=" + aHex + ", d=" + dHex);
            } else {
                pass();
            }
        } catch (Exception e) {
            fail("Specific case exception: a=" + aHex + ", d=" + dHex + ", error=" + e.getMessage());
        }
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    private static Int128 randomInt128() {
        long hi = rand.nextLong();
        long lo = rand.nextLong();
        return Int128.of(hi, lo);
    }

    private static Int128 randomNonZeroInt128() {
        Int128 val;
        do {
            val = randomInt128();
        } while (val.isZero());
        return val;
    }

    private static void pass() {
        passCount++;
    }

    private static void fail(String msg) {
        System.err.println("  FAIL: " + msg);
        failCount++;
    }
}
