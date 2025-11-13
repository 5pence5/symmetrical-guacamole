import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for Int128 class.
 * Tests all functionality and reports any issues found.
 * Matches Maven Surefire discovery pattern (*Test.java).
 */
public class Int128Test {

    private static final List<String> issues = new ArrayList<>();
    private static int testsRun = 0;
    private static int testsPassed = 0;

    /**
     * JUnit test method that executes all Int128 tests.
     * This allows Maven Surefire to discover and run the comprehensive test suite.
     */
    @Test
    public void runAllTests() {
        // Reset state for clean test run
        issues.clear();
        testsRun = 0;
        testsPassed = 0;

        // Run all tests
        main(null);

        // Fail the JUnit test if any issues were found
        if (!issues.isEmpty()) {
            throw new AssertionError(
                String.format("Int128 test suite failed: %d out of %d tests failed. See output for details.",
                    testsRun - testsPassed, testsRun)
            );
        }
    }

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("COMPREHENSIVE INT128 REVIEW AND TEST REPORT");
        System.out.println("=".repeat(80));
        System.out.println();

        // Run all test categories
        testBasicConstruction();
        testArithmetic();
        testComparison();
        testBitwise();
        testShifts();
        testDivision();
        testStringConversion();
        testEdgeCases();
        testFinancialOperations();
        testSerialization();
        testPerformanceCriticalPaths();

        // Run built-in self-check
        try {
            Int128.quickSelfCheck();
            recordPass("Built-in quickSelfCheck");
        } catch (AssertionError e) {
            recordIssue("Built-in quickSelfCheck failed: " + e.getMessage());
        }

        // Print summary
        printReport();
    }

    // =========================================================================
    // Test Categories
    // =========================================================================

    private static void testBasicConstruction() {
        section("Basic Construction & Constants");

        // Test constants
        test("ZERO is actually zero", () -> {
            return Int128.ZERO.isZero();
        });

        test("ONE equals 1", () -> {
            return Int128.ONE.equals(Int128.valueOf(1));
        });

        test("DECIMAL_BASE equals 10", () -> {
            return Int128.DECIMAL_BASE.equals(Int128.valueOf(10));
        });

        test("MIN_VALUE is minimum", () -> {
            Int128 min = Int128.MIN_VALUE;
            return min.isNegative() && min.equals(Int128.of(Long.MIN_VALUE, 0L));
        });

        test("MAX_VALUE is maximum", () -> {
            Int128 max = Int128.MAX_VALUE;
            return max.isPositive() && max.equals(Int128.of(Long.MAX_VALUE, 0xFFFF_FFFF_FFFF_FFFFL));
        });

        // Test valueOf
        test("valueOf(long) positive", () -> {
            Int128 v = Int128.valueOf(12345L);
            return v.toLong() == 12345L;
        });

        test("valueOf(long) negative", () -> {
            Int128 v = Int128.valueOf(-12345L);
            return v.toLong() == -12345L && v.isNegative();
        });

        test("valueOf(int) sign extension", () -> {
            Int128 v = Int128.valueOf(-1);
            return v.isNegative() && v.toLongExact() == -1L;
        });

        test("fromUnsignedLong", () -> {
            Int128 v = Int128.fromUnsignedLong(0xFFFF_FFFF_FFFF_FFFFL);
            return v.isPositive() && v.toLong() == 0xFFFF_FFFF_FFFF_FFFFL;
        });
    }

    private static void testArithmetic() {
        section("Arithmetic Operations");

        // Addition
        test("Simple addition", () -> {
            Int128 a = Int128.valueOf(100);
            Int128 b = Int128.valueOf(200);
            return a.add(b).equals(Int128.valueOf(300));
        });

        test("Addition with carry", () -> {
            Int128 a = Int128.of(0, 0xFFFF_FFFF_FFFF_FFFFL);
            Int128 b = Int128.ONE;
            Int128 result = a.add(b);
            return result.equals(Int128.of(1, 0));
        });

        test("Addition overflow wraps", () -> {
            Int128 result = Int128.MAX_VALUE.add(Int128.ONE);
            return result.equals(Int128.MIN_VALUE);
        });

        // Subtraction
        test("Simple subtraction", () -> {
            Int128 a = Int128.valueOf(300);
            Int128 b = Int128.valueOf(100);
            return a.sub(b).equals(Int128.valueOf(200));
        });

        test("Subtraction with borrow", () -> {
            Int128 a = Int128.of(1, 0);
            Int128 b = Int128.ONE;
            Int128 result = a.sub(b);
            return result.equals(Int128.of(0, 0xFFFF_FFFF_FFFF_FFFFL));
        });

        // Negation
        test("Negate positive", () -> {
            Int128 a = Int128.valueOf(12345);
            return a.negate().equals(Int128.valueOf(-12345));
        });

        test("Negate negative", () -> {
            Int128 a = Int128.valueOf(-12345);
            return a.negate().equals(Int128.valueOf(12345));
        });

        test("Negate MIN_VALUE wraps to itself", () -> {
            return Int128.MIN_VALUE.negate().equals(Int128.MIN_VALUE);
        });

        test("Double negation", () -> {
            Int128 a = Int128.valueOf(999);
            return a.negate().negate().equals(a);
        });

        // Increment/Decrement
        test("Increment", () -> {
            Int128 a = Int128.valueOf(99);
            return a.inc().equals(Int128.valueOf(100));
        });

        test("Decrement", () -> {
            Int128 a = Int128.valueOf(100);
            return a.dec().equals(Int128.valueOf(99));
        });

        // Absolute value
        test("abs of positive", () -> {
            Int128 a = Int128.valueOf(100);
            return a.abs().equals(a);
        });

        test("abs of negative", () -> {
            Int128 a = Int128.valueOf(-100);
            return a.abs().equals(Int128.valueOf(100));
        });
    }

    private static void testComparison() {
        section("Comparison & Predicates");

        test("compareTo equal", () -> {
            return Int128.valueOf(100).compareTo(Int128.valueOf(100)) == 0;
        });

        test("compareTo less than", () -> {
            return Int128.valueOf(99).compareTo(Int128.valueOf(100)) < 0;
        });

        test("compareTo greater than", () -> {
            return Int128.valueOf(101).compareTo(Int128.valueOf(100)) > 0;
        });

        test("compareTo with negative", () -> {
            return Int128.valueOf(-1).compareTo(Int128.valueOf(1)) < 0;
        });

        test("isZero predicate", () -> {
            return Int128.ZERO.isZero() && !Int128.ONE.isZero();
        });

        test("isNegative predicate", () -> {
            return Int128.valueOf(-1).isNegative() && !Int128.valueOf(1).isNegative();
        });

        test("isPositive predicate", () -> {
            return Int128.valueOf(1).isPositive() && !Int128.valueOf(-1).isPositive();
        });

        test("signum of zero", () -> {
            return Int128.ZERO.signum() == 0;
        });

        test("signum of positive", () -> {
            return Int128.valueOf(100).signum() == 1;
        });

        test("signum of negative", () -> {
            return Int128.valueOf(-100).signum() == -1;
        });

        test("isPowerOfTwo for power of 2", () -> {
            return Int128.valueOf(16).isPowerOfTwo();
        });

        test("isPowerOfTwo for non-power of 2", () -> {
            return !Int128.valueOf(15).isPowerOfTwo();
        });

        test("isPowerOfTwo for negative", () -> {
            return !Int128.valueOf(-16).isPowerOfTwo();
        });

        test("compareUnsigned", () -> {
            Int128 a = Int128.valueOf(-1); // All bits set
            Int128 b = Int128.valueOf(1);
            return a.compareUnsigned(b) > 0; // -1 unsigned is very large
        });
    }

    private static void testBitwise() {
        section("Bitwise Operations");

        test("AND operation", () -> {
            Int128 a = Int128.valueOf(0b1111);
            Int128 b = Int128.valueOf(0b1010);
            return a.and(b).equals(Int128.valueOf(0b1010));
        });

        test("OR operation", () -> {
            Int128 a = Int128.valueOf(0b1100);
            Int128 b = Int128.valueOf(0b0110);
            return a.or(b).equals(Int128.valueOf(0b1110));
        });

        test("XOR operation", () -> {
            Int128 a = Int128.valueOf(0b1111);
            Int128 b = Int128.valueOf(0b1010);
            return a.xor(b).equals(Int128.valueOf(0b0101));
        });

        test("NOT operation", () -> {
            Int128 a = Int128.ZERO;
            Int128 result = a.not();
            return result.equals(Int128.valueOf(-1));
        });

        test("testBit", () -> {
            Int128 a = Int128.valueOf(5); // binary 101
            return a.testBit(0) && !a.testBit(1) && a.testBit(2);
        });

        test("setBit", () -> {
            Int128 a = Int128.valueOf(4); // binary 100
            Int128 result = a.setBit(0); // should be 101 = 5
            return result.equals(Int128.valueOf(5));
        });

        test("clearBit", () -> {
            Int128 a = Int128.valueOf(7); // binary 111
            Int128 result = a.clearBit(1); // should be 101 = 5
            return result.equals(Int128.valueOf(5));
        });

        test("bitLength of zero", () -> {
            return Int128.ZERO.bitLength() == 0;
        });

        test("bitLength of small positive", () -> {
            return Int128.valueOf(7).bitLength() == 3;
        });

        test("bitLength of -1", () -> {
            return Int128.valueOf(-1).bitLength() == 0;
        });
    }

    private static void testShifts() {
        section("Shift Operations");

        test("shiftLeft by 0", () -> {
            Int128 a = Int128.valueOf(100);
            return a.shiftLeft(0).equals(a);
        });

        test("shiftLeft small", () -> {
            Int128 a = Int128.valueOf(5);
            return a.shiftLeft(2).equals(Int128.valueOf(20));
        });

        test("shiftLeft by 64", () -> {
            Int128 a = Int128.valueOf(1);
            Int128 result = a.shiftLeft(64);
            return result.equals(Int128.of(1, 0));
        });

        test("shiftLeft large", () -> {
            Int128 a = Int128.valueOf(1);
            Int128 result = a.shiftLeft(100);
            return result.equals(Int128.of(1L << 36, 0));
        });

        test("shiftRight by 0", () -> {
            Int128 a = Int128.valueOf(100);
            return a.shiftRight(0).equals(a);
        });

        test("shiftRight small", () -> {
            Int128 a = Int128.valueOf(20);
            return a.shiftRight(2).equals(Int128.valueOf(5));
        });

        test("shiftRight negative (sign extension)", () -> {
            Int128 a = Int128.valueOf(-16);
            Int128 result = a.shiftRight(2);
            return result.equals(Int128.valueOf(-4));
        });

        test("shiftRightUnsigned", () -> {
            Int128 a = Int128.valueOf(-1);
            Int128 result = a.shiftRightUnsigned(64);
            return result.equals(Int128.of(0, 0xFFFF_FFFF_FFFF_FFFFL));
        });
    }

    private static void testDivision() {
        section("Division & Remainder");

        test("Simple division", () -> {
            Int128 a = Int128.valueOf(100);
            Int128 b = Int128.valueOf(10);
            return a.div(b).equals(Int128.valueOf(10));
        });

        test("Division with remainder", () -> {
            Int128 a = Int128.valueOf(107);
            Int128 b = Int128.valueOf(10);
            Int128 q = a.div(b);
            Int128 r = a.rem(b);
            return q.equals(Int128.valueOf(10)) && r.equals(Int128.valueOf(7));
        });

        test("divRem identity: a = q*d + r", () -> {
            Int128 a = Int128.valueOf(12345);
            Int128 d = Int128.valueOf(67);
            Int128[] qr = a.divRem(d);
            Int128 recomposed = qr[0].mul(d).add(qr[1]);
            // Verify identity and Euclidean property: |r| < |d|
            return recomposed.equals(a) && qr[1].abs().compareUnsigned(d.abs()) < 0;
        });

        test("Division by 1", () -> {
            Int128 a = Int128.valueOf(12345);
            return a.div(Int128.ONE).equals(a);
        });

        test("Division by -1", () -> {
            Int128 a = Int128.valueOf(12345);
            return a.div(Int128.valueOf(-1)).equals(a.negate());
        });

        test("Division negative / positive", () -> {
            Int128 a = Int128.valueOf(-100);
            Int128 b = Int128.valueOf(10);
            return a.div(b).equals(Int128.valueOf(-10));
        });

        test("Division positive / negative", () -> {
            Int128 a = Int128.valueOf(100);
            Int128 b = Int128.valueOf(-10);
            return a.div(b).equals(Int128.valueOf(-10));
        });

        test("Division negative / negative", () -> {
            Int128 a = Int128.valueOf(-100);
            Int128 b = Int128.valueOf(-10);
            return a.div(b).equals(Int128.valueOf(10));
        });

        test("Divide by zero throws", () -> {
            try {
                Int128.valueOf(100).div(Int128.ZERO);
                return false;
            } catch (ArithmeticException e) {
                return true;
            }
        });

        test("Large 128/64 division", () -> {
            Int128 a = Int128.of(5, 0);
            Int128 b = Int128.valueOf(2);
            Int128 q = a.div(b);
            // 5 * 2^64 / 2 = 2.5 * 2^64 = floor to 2 * 2^64 + 2^63
            return q.equals(Int128.of(2, 0x8000_0000_0000_0000L));
        });

        test("Unsigned division", () -> {
            Int128 a = Int128.valueOf(-1); // All bits set
            Int128 b = Int128.valueOf(2);
            Int128 q = a.divideUnsigned(b);
            // Should be (2^128 - 1) / 2 = 2^127 - 1 (approximately)
            return q.isPositive();
        });
    }

    private static void testStringConversion() {
        section("String Conversion & Parsing");

        test("toString decimal", () -> {
            Int128 a = Int128.valueOf(12345);
            return a.toString().equals("12345");
        });

        test("toString negative", () -> {
            Int128 a = Int128.valueOf(-12345);
            return a.toString().equals("-12345");
        });

        test("fromString decimal", () -> {
            Int128 a = Int128.fromString("12345");
            return a.equals(Int128.valueOf(12345));
        });

        test("fromString negative", () -> {
            Int128 a = Int128.fromString("-12345");
            return a.equals(Int128.valueOf(-12345));
        });

        test("Round-trip MAX_VALUE", () -> {
            String s = Int128.MAX_VALUE.toString();
            Int128 parsed = Int128.fromString(s);
            return parsed.equals(Int128.MAX_VALUE);
        });

        test("Round-trip MIN_VALUE", () -> {
            String s = Int128.MIN_VALUE.toString();
            Int128 parsed = Int128.fromString(s);
            return parsed.equals(Int128.MIN_VALUE);
        });

        test("parseHex simple", () -> {
            Int128 a = Int128.parseHex("FF");
            return a.equals(Int128.valueOf(255));
        });

        test("parseHex with 0x prefix", () -> {
            Int128 a = Int128.parseHex("0xFF");
            return a.equals(Int128.valueOf(255));
        });

        test("parseHex negative", () -> {
            Int128 a = Int128.parseHex("-0x10");
            return a.equals(Int128.valueOf(-16));
        });

        test("toHexString", () -> {
            Int128 a = Int128.valueOf(255);
            return a.toHexString().equals("ff");
        });

        test("toString with radix", () -> {
            Int128 a = Int128.valueOf(16);
            return a.toString(2).equals("10000");
        });
    }

    private static void testEdgeCases() {
        section("Edge Cases & Corner Conditions");

        test("Multiplication by 0", () -> {
            Int128 a = Int128.valueOf(12345);
            return a.mul(0).equals(Int128.ZERO);
        });

        test("Multiplication by 1", () -> {
            Int128 a = Int128.valueOf(12345);
            return a.mul(1).equals(a);
        });

        test("Multiplication by -1", () -> {
            Int128 a = Int128.valueOf(12345);
            return a.mul(-1).equals(a.negate());
        });

        test("Multiplication overflow wraps", () -> {
            Int128 a = Int128.MAX_VALUE;
            Int128 b = Int128.valueOf(2);
            Int128 result = a.mul(b);
            // Should wrap around
            return !result.equals(Int128.MAX_VALUE);
        });

        test("Equality reflexive", () -> {
            Int128 a = Int128.valueOf(12345);
            return a.equals(a);
        });

        test("Equality symmetric", () -> {
            Int128 a = Int128.valueOf(12345);
            Int128 b = Int128.valueOf(12345);
            return a.equals(b) && b.equals(a);
        });

        test("Equality null-safe", () -> {
            Int128 a = Int128.valueOf(12345);
            return !a.equals(null);
        });

        test("Hash code consistency", () -> {
            Int128 a = Int128.valueOf(12345);
            Int128 b = Int128.valueOf(12345);
            return a.hashCode() == b.hashCode();
        });

        test("fitsInLong positive", () -> {
            return Int128.valueOf(12345).fitsInLong();
        });

        test("fitsInLong negative", () -> {
            return Int128.valueOf(-12345).fitsInLong();
        });

        test("fitsInLong large false", () -> {
            return !Int128.of(1, 0).fitsInLong();
        });

        test("toLongExact throws when out of range", () -> {
            try {
                Int128.of(1, 0).toLongExact();
                return false;
            } catch (ArithmeticException e) {
                return true;
            }
        });
    }

    private static void testFinancialOperations() {
        section("Financial & Decimal Operations");

        test("scaleUpPow10", () -> {
            Int128 a = Int128.valueOf(123);
            Int128 result = a.scaleUpPow10(2); // * 100
            return result.equals(Int128.valueOf(12300));
        });

        test("divRemPow10", () -> {
            Int128 a = Int128.valueOf(12345);
            Int128[] qr = a.divRemPow10(2); // / 100
            return qr[0].equals(Int128.valueOf(123)) && qr[1].equals(Int128.valueOf(45));
        });

        test("divRoundHalfEvenPow10 round down", () -> {
            Int128 a = Int128.valueOf(1234); // 12.34
            Int128 result = a.divRoundHalfEvenPow10(2); // / 100
            return result.equals(Int128.valueOf(12)); // rounds down
        });

        test("divRoundHalfEvenPow10 round up", () -> {
            Int128 a = Int128.valueOf(1267); // 12.67
            Int128 result = a.divRoundHalfEvenPow10(2); // / 100
            return result.equals(Int128.valueOf(13)); // rounds up
        });

        test("divRoundHalfEvenPow10 half-even to even", () -> {
            Int128 a = Int128.valueOf(1250); // 12.50
            Int128 result = a.divRoundHalfEvenPow10(2); // / 100
            return result.equals(Int128.valueOf(12)); // rounds to even
        });

        test("divRoundHalfEvenPow10 half-odd to even", () -> {
            Int128 a = Int128.valueOf(1350); // 13.50
            Int128 result = a.divRoundHalfEvenPow10(2); // / 100
            return result.equals(Int128.valueOf(14)); // rounds to even
        });

        test("floorDivPow10 positive", () -> {
            Int128 a = Int128.valueOf(1234);
            return a.floorDivPow10(2).equals(Int128.valueOf(12));
        });

        test("floorDivPow10 negative", () -> {
            Int128 a = Int128.valueOf(-1234);
            Int128 result = a.floorDivPow10(2);
            return result.equals(Int128.valueOf(-13)); // floor towards -∞
        });

        test("ceilDivPow10 positive", () -> {
            Int128 a = Int128.valueOf(1234);
            Int128 result = a.ceilDivPow10(2);
            return result.equals(Int128.valueOf(13)); // ceil towards +∞
        });

        test("tenPow lookup", () -> {
            Int128 ten = Int128.tenPow(1);
            return ten.equals(Int128.valueOf(10));
        });
    }

    private static void testSerialization() {
        section("Serialization & Byte Operations");

        test("toBytesBE and fromBytesBE roundtrip", () -> {
            Int128 a = Int128.valueOf(12345);
            byte[] bytes = a.toBytesBE();
            Int128 b = Int128.fromBytesBE(bytes);
            return a.equals(b);
        });

        test("toBytesBE negative value", () -> {
            Int128 a = Int128.valueOf(-1);
            byte[] bytes = a.toBytesBE();
            // All bytes should be 0xFF
            for (byte b : bytes) {
                if (b != (byte) 0xFF) return false;
            }
            return true;
        });

        test("fromBytesBE null throws", () -> {
            try {
                Int128.fromBytesBE(null);
                return false;
            } catch (IllegalArgumentException e) {
                return true;
            }
        });

        test("fromBytesBE wrong size throws", () -> {
            try {
                Int128.fromBytesBE(new byte[8]);
                return false;
            } catch (IllegalArgumentException e) {
                return true;
            }
        });
    }

    private static void testPerformanceCriticalPaths() {
        section("Performance-Critical Paths & Algorithms");

        // Test the 128/64 fast path
        test("128/64 fast path with power of 10", () -> {
            Int128 a = Int128.valueOf(1000000000000000000L); // 10^18
            Int128 d = Int128.valueOf(1000000000L); // 10^9
            Int128[] qr = a.divRem(d);
            return qr[0].equals(Int128.valueOf(1000000000L)) && qr[1].equals(Int128.ZERO);
        });

        // Test the 128/128 division path
        test("128/128 division path", () -> {
            Int128 a = Int128.parseHex("0xFFFF000000000000FFFF000000000000");
            Int128 d = Int128.parseHex("0x0000FFFF00000000FFFFFFFF00000001");
            Int128[] qr = a.divRem(d);
            // Verify identity: a = q*d + r and Euclidean property: |r| < |d|
            Int128 recomposed = qr[0].mul(d).add(qr[1]);
            return recomposed.equals(a) && qr[1].abs().compareUnsigned(d.abs()) < 0;
        });

        // Test multiplication corner cases
        test("Large multiplication wrapping", () -> {
            Int128 a = Int128.of(0x1234_5678_9ABC_DEF0L, 0xFEDC_BA98_7654_3210L);
            Int128 b = Int128.valueOf(123456);
            Int128 result = a.mul(b);
            // Just verify it doesn't crash and returns something
            return result != null;
        });

        // Verify against BigInteger for correctness
        test("Division matches BigInteger (small)", () -> {
            Int128 a = Int128.valueOf(98765432123456789L);
            Int128 d = Int128.valueOf(123456789L);
            Int128[] qr = a.divRem(d);

            BigInteger ba = BigInteger.valueOf(98765432123456789L);
            BigInteger bd = BigInteger.valueOf(123456789L);
            BigInteger bq = ba.divide(bd);
            BigInteger br = ba.remainder(bd);

            return qr[0].equals(Int128.fromString(bq.toString())) &&
                   qr[1].equals(Int128.fromString(br.toString()));
        });

        test("Multiplication matches BigInteger", () -> {
            long x = 123456789012345L;
            long y = 987654321098765L;
            Int128 a = Int128.valueOf(x);
            Int128 b = Int128.valueOf(y);
            Int128 result = a.mul(b);

            BigInteger bx = BigInteger.valueOf(x);
            BigInteger by = BigInteger.valueOf(y);
            BigInteger expected = bx.multiply(by);

            return result.toString().equals(expected.toString());
        });
    }

    // =========================================================================
    // Test Framework
    // =========================================================================

    private static void section(String name) {
        System.out.println();
        System.out.println("-".repeat(80));
        System.out.println(name);
        System.out.println("-".repeat(80));
    }

    private static void test(String description, TestCase testCase) {
        testsRun++;
        try {
            boolean passed = testCase.run();
            if (passed) {
                testsPassed++;
                System.out.println("  [PASS] " + description);
            } else {
                recordIssue(description + " - assertion failed");
                System.out.println("  [FAIL] " + description);
            }
        } catch (Exception e) {
            recordIssue(description + " - exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("  [ERROR] " + description + ": " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    private static void recordIssue(String issue) {
        issues.add(issue);
    }

    private static void recordPass(String description) {
        testsRun++;
        testsPassed++;
        System.out.println("  [PASS] " + description);
    }

    private static void printReport() {
        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("TEST SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println("Total tests run: " + testsRun);
        System.out.println("Tests passed: " + testsPassed);
        System.out.println("Tests failed: " + (testsRun - testsPassed));
        System.out.println();

        if (issues.isEmpty()) {
            System.out.println("SUCCESS: No issues found! ✓");
        } else {
            System.out.println("ISSUES FOUND: " + issues.size());
            System.out.println();
            for (int i = 0; i < issues.size(); i++) {
                System.out.println((i + 1) + ". " + issues.get(i));
            }
        }
        System.out.println("=".repeat(80));
    }

    @FunctionalInterface
    interface TestCase {
        boolean run() throws Exception;
    }
}
