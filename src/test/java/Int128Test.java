import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Comprehensive test suite for Int128 implementation.
 * Tests all operations for correctness, edge cases, and financial calculation scenarios.
 */
class Int128Test {

    // ========================================================================
    // Constants Tests
    // ========================================================================

    @Nested
    @DisplayName("Constants Tests")
    class ConstantsTests {

        @Test
        @DisplayName("ZERO constant should be 0")
        void testZeroConstant() {
            assertTrue(Int128.ZERO.isZero());
            assertEquals("0", Int128.ZERO.toString());
            assertEquals(0, Int128.ZERO.signum());
        }

        @Test
        @DisplayName("ONE constant should be 1")
        void testOneConstant() {
            assertFalse(Int128.ONE.isZero());
            assertEquals("1", Int128.ONE.toString());
            assertEquals(1, Int128.ONE.signum());
            assertTrue(Int128.ONE.isPositive());
        }

        @Test
        @DisplayName("DECIMAL_BASE should be 10")
        void testDecimalBase() {
            assertEquals("10", Int128.DECIMAL_BASE.toString());
        }

        @Test
        @DisplayName("MAX_VALUE should be 2^127 - 1")
        void testMaxValue() {
            assertFalse(Int128.MAX_VALUE.isNegative());
            assertTrue(Int128.MAX_VALUE.isPositive());
            assertEquals("170141183460469231731687303715884105727", Int128.MAX_VALUE.toString());
        }

        @Test
        @DisplayName("MIN_VALUE should be -2^127")
        void testMinValue() {
            assertTrue(Int128.MIN_VALUE.isNegative());
            assertFalse(Int128.MIN_VALUE.isPositive());
            assertEquals("-170141183460469231731687303715884105728", Int128.MIN_VALUE.toString());
        }
    }

    // ========================================================================
    // Factory Methods Tests
    // ========================================================================

    @Nested
    @DisplayName("Factory Methods Tests")
    class FactoryMethodsTests {

        @Test
        @DisplayName("of() should create value from high and low parts")
        void testOf() {
            Int128 val = Int128.of(1L, 2L);
            assertNotNull(val);
            assertFalse(val.isZero());
        }

        @Test
        @DisplayName("valueOf(long) should sign-extend positive values")
        void testValueOfLongPositive() {
            Int128 val = Int128.valueOf(12345L);
            assertEquals("12345", val.toString());
            assertTrue(val.isPositive());
        }

        @Test
        @DisplayName("valueOf(long) should sign-extend negative values")
        void testValueOfLongNegative() {
            Int128 val = Int128.valueOf(-12345L);
            assertEquals("-12345", val.toString());
            assertTrue(val.isNegative());
        }

        @Test
        @DisplayName("valueOf(int) should work")
        void testValueOfInt() {
            Int128 val = Int128.valueOf(12345);
            assertEquals("12345", val.toString());
        }

        @Test
        @DisplayName("fromUnsignedLong() should treat as unsigned")
        void testFromUnsignedLong() {
            Int128 val = Int128.fromUnsignedLong(-1L); // All bits set in low word
            assertTrue(val.isPositive());
            assertEquals("18446744073709551615", val.toString());
        }

        @Test
        @DisplayName("fromString() should parse positive decimal strings")
        void testFromStringPositive() {
            Int128 val = Int128.fromString("12345");
            assertEquals("12345", val.toString());
        }

        @Test
        @DisplayName("fromString() should parse negative decimal strings")
        void testFromStringNegative() {
            Int128 val = Int128.fromString("-12345");
            assertEquals("-12345", val.toString());
        }

        @Test
        @DisplayName("fromString() should handle zero")
        void testFromStringZero() {
            assertEquals(Int128.ZERO, Int128.fromString("0"));
        }

        @Test
        @DisplayName("fromString() with radix")
        void testFromStringRadix() {
            Int128 hex = Int128.fromString("FF", 16);
            assertEquals("255", hex.toString());
        }

        @Test
        @DisplayName("fromString() should parse MAX_VALUE")
        void testFromStringMaxValue() {
            String maxStr = "170141183460469231731687303715884105727";
            Int128 val = Int128.fromString(maxStr);
            assertEquals(Int128.MAX_VALUE, val);
        }

        @Test
        @DisplayName("fromString() should parse MIN_VALUE")
        void testFromStringMinValue() {
            String minStr = "-170141183460469231731687303715884105728";
            Int128 val = Int128.fromString(minStr);
            assertEquals(Int128.MIN_VALUE, val);
        }

        @Test
        @DisplayName("fromString() should reject invalid radix")
        void testFromStringInvalidRadix() {
            assertThrows(NumberFormatException.class, () -> Int128.fromString("123", 1));
            assertThrows(NumberFormatException.class, () -> Int128.fromString("123", 37));
        }

        @Test
        @DisplayName("fromString() should handle numeric separators")
        void testFromStringNumericSeparators() {
            Int128 val = Int128.fromString("1_000_000");
            assertEquals("1000000", val.toString());
        }

        @Test
        @DisplayName("parseHex() should parse hex strings")
        void testParseHex() {
            Int128 val = Int128.parseHex("FF");
            assertEquals("255", val.toString());
        }

        @Test
        @DisplayName("parseHex() should handle 0x prefix")
        void testParseHexWithPrefix() {
            Int128 val = Int128.parseHex("0xFF");
            assertEquals("255", val.toString());
        }

        @Test
        @DisplayName("parseHex() should handle negative sign")
        void testParseHexNegative() {
            Int128 val = Int128.parseHex("-0x10");
            assertEquals("-16", val.toString());
        }

        @Test
        @DisplayName("parseHex() should reject empty string")
        void testParseHexEmpty() {
            assertThrows(NumberFormatException.class, () -> Int128.parseHex(""));
        }

        @Test
        @DisplayName("parseHex() should reject too long hex")
        void testParseHexTooLong() {
            assertThrows(NumberFormatException.class,
                () -> Int128.parseHex("0x123456789ABCDEF0123456789ABCDEF012"));
        }
    }

    // ========================================================================
    // Basic Arithmetic Tests
    // ========================================================================

    @Nested
    @DisplayName("Addition Tests")
    class AdditionTests {

        @Test
        @DisplayName("Add two positive numbers")
        void testAddPositive() {
            Int128 a = Int128.valueOf(100);
            Int128 b = Int128.valueOf(200);
            Int128 result = a.add(b);
            assertEquals("300", result.toString());
        }

        @Test
        @DisplayName("Add positive and negative")
        void testAddMixed() {
            Int128 a = Int128.valueOf(100);
            Int128 b = Int128.valueOf(-50);
            Int128 result = a.add(b);
            assertEquals("50", result.toString());
        }

        @Test
        @DisplayName("Add with carry from low to high")
        void testAddCarry() {
            Int128 a = Int128.of(0L, -1L); // Low word all 1s
            Int128 b = Int128.of(0L, 1L);
            Int128 result = a.add(b);
            assertEquals(Int128.of(1L, 0L), result);
        }

        @Test
        @DisplayName("Add zero identity")
        void testAddZero() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(val, val.add(Int128.ZERO));
            assertEquals(val, Int128.ZERO.add(val));
        }

        @Test
        @DisplayName("Add long convenience method")
        void testAddLong() {
            Int128 val = Int128.valueOf(100);
            Int128 result = val.add(50);
            assertEquals("150", result.toString());
        }

        @Test
        @DisplayName("Addition overflow wraps")
        void testAdditionOverflow() {
            Int128 result = Int128.MAX_VALUE.add(Int128.ONE);
            assertEquals(Int128.MIN_VALUE, result);
        }
    }

    @Nested
    @DisplayName("Subtraction Tests")
    class SubtractionTests {

        @Test
        @DisplayName("Subtract positive numbers")
        void testSubtractPositive() {
            Int128 a = Int128.valueOf(200);
            Int128 b = Int128.valueOf(100);
            Int128 result = a.sub(b);
            assertEquals("100", result.toString());
        }

        @Test
        @DisplayName("Subtract with borrow")
        void testSubtractBorrow() {
            Int128 a = Int128.of(1L, 0L);
            Int128 b = Int128.of(0L, 1L);
            Int128 result = a.sub(b);
            assertEquals(Int128.of(0L, -1L), result);
        }

        @Test
        @DisplayName("Subtract zero identity")
        void testSubtractZero() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(val, val.sub(Int128.ZERO));
        }

        @Test
        @DisplayName("Subtract from self gives zero")
        void testSubtractSelf() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(Int128.ZERO, val.sub(val));
        }

        @Test
        @DisplayName("Subtract long convenience method")
        void testSubLong() {
            Int128 val = Int128.valueOf(100);
            Int128 result = val.sub(50);
            assertEquals("50", result.toString());
        }

        @Test
        @DisplayName("Subtraction underflow wraps")
        void testSubtractionUnderflow() {
            Int128 result = Int128.MIN_VALUE.sub(Int128.ONE);
            assertEquals(Int128.MAX_VALUE, result);
        }
    }

    @Nested
    @DisplayName("Multiplication Tests")
    class MultiplicationTests {

        @Test
        @DisplayName("Multiply positive numbers")
        void testMultiplyPositive() {
            Int128 a = Int128.valueOf(123);
            Int128 b = Int128.valueOf(456);
            Int128 result = a.mul(b);
            assertEquals("56088", result.toString());
        }

        @Test
        @DisplayName("Multiply by zero")
        void testMultiplyZero() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(Int128.ZERO, val.mul(Int128.ZERO));
            assertEquals(Int128.ZERO, Int128.ZERO.mul(val));
        }

        @Test
        @DisplayName("Multiply by one identity")
        void testMultiplyOne() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(val, val.mul(Int128.ONE));
            assertEquals(val, Int128.ONE.mul(val));
        }

        @Test
        @DisplayName("Multiply by -1")
        void testMultiplyNegativeOne() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(val.negate(), val.mul(-1));
        }

        @Test
        @DisplayName("Multiply positive and negative")
        void testMultiplyNegative() {
            Int128 a = Int128.valueOf(100);
            Int128 b = Int128.valueOf(-50);
            Int128 result = a.mul(b);
            assertEquals("-5000", result.toString());
        }

        @Test
        @DisplayName("Multiply two negative numbers")
        void testMultiplyTwoNegatives() {
            Int128 a = Int128.valueOf(-100);
            Int128 b = Int128.valueOf(-50);
            Int128 result = a.mul(b);
            assertEquals("5000", result.toString());
        }

        @Test
        @DisplayName("Multiply large numbers")
        void testMultiplyLarge() {
            Int128 a = Int128.fromString("1000000000000");
            Int128 b = Int128.fromString("1000000");
            Int128 result = a.mul(b);
            assertEquals("1000000000000000000", result.toString());
        }

        @Test
        @DisplayName("Multiply by long")
        void testMultiplyLong() {
            Int128 val = Int128.valueOf(100);
            Int128 result = val.mul(50L);
            assertEquals("5000", result.toString());
        }

        @Test
        @DisplayName("times10() convenience method")
        void testTimes10() {
            Int128 val = Int128.valueOf(123);
            assertEquals("1230", val.times10().toString());
        }

        @Test
        @DisplayName("times100() convenience method")
        void testTimes100() {
            Int128 val = Int128.valueOf(123);
            assertEquals("12300", val.times100().toString());
        }

        @Test
        @DisplayName("times1000() convenience method")
        void testTimes1000() {
            Int128 val = Int128.valueOf(123);
            assertEquals("123000", val.times1000().toString());
        }

        @Test
        @DisplayName("scaleUpPow10() tests")
        void testScaleUpPow10() {
            Int128 val = Int128.valueOf(123);
            assertEquals("123000", val.scaleUpPow10(3).toString());
            assertEquals("123000000000", val.scaleUpPow10(9).toString());
        }

        @Test
        @DisplayName("scaleUpPow10() with exp=0")
        void testScaleUpPow10Zero() {
            Int128 val = Int128.valueOf(123);
            assertEquals(val, val.scaleUpPow10(0));
        }

        @Test
        @DisplayName("scaleUpPow10() throws on invalid exp")
        void testScaleUpPow10Invalid() {
            Int128 val = Int128.valueOf(123);
            assertThrows(IllegalArgumentException.class, () -> val.scaleUpPow10(-1));
            assertThrows(IllegalArgumentException.class, () -> val.scaleUpPow10(39));
        }
    }

    @Nested
    @DisplayName("Division Tests")
    class DivisionTests {

        @Test
        @DisplayName("Divide positive numbers")
        void testDividePositive() {
            Int128 a = Int128.valueOf(100);
            Int128 b = Int128.valueOf(5);
            Int128 result = a.div(b);
            assertEquals("20", result.toString());
        }

        @Test
        @DisplayName("Divide by one")
        void testDivideOne() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(val, val.div(Int128.ONE));
        }

        @Test
        @DisplayName("Divide by self")
        void testDivideSelf() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(Int128.ONE, val.div(val));
        }

        @Test
        @DisplayName("Divide by zero throws")
        void testDivideZero() {
            Int128 val = Int128.valueOf(12345);
            assertThrows(ArithmeticException.class, () -> val.div(Int128.ZERO));
        }

        @Test
        @DisplayName("Divide with remainder")
        void testDivideWithRemainder() {
            Int128 a = Int128.valueOf(23);
            Int128 b = Int128.valueOf(5);
            assertEquals("4", a.div(b).toString());
            assertEquals("3", a.rem(b).toString());
        }

        @Test
        @DisplayName("divRem() returns both quotient and remainder")
        void testDivRem() {
            Int128 a = Int128.valueOf(23);
            Int128 b = Int128.valueOf(5);
            Int128[] result = a.divRem(b);
            assertEquals("4", result[0].toString());
            assertEquals("3", result[1].toString());
        }

        @Test
        @DisplayName("Division identity: a = q*b + r")
        void testDivisionIdentity() {
            Int128 a = Int128.valueOf(12345);
            Int128 b = Int128.valueOf(789);
            Int128[] dr = a.divRem(b);
            Int128 reconstructed = dr[0].mul(b).add(dr[1]);
            assertEquals(a, reconstructed);
        }

        @Test
        @DisplayName("Negative dividend")
        void testDivideNegativeDividend() {
            Int128 a = Int128.valueOf(-100);
            Int128 b = Int128.valueOf(5);
            assertEquals("-20", a.div(b).toString());
        }

        @Test
        @DisplayName("Negative divisor")
        void testDivideNegativeDivisor() {
            Int128 a = Int128.valueOf(100);
            Int128 b = Int128.valueOf(-5);
            assertEquals("-20", a.div(b).toString());
        }

        @Test
        @DisplayName("Both negative")
        void testDivideBothNegative() {
            Int128 a = Int128.valueOf(-100);
            Int128 b = Int128.valueOf(-5);
            assertEquals("20", a.div(b).toString());
        }

        @Test
        @DisplayName("MIN_VALUE / -1 wraps to MIN_VALUE")
        void testDivideMinValueByNegOne() {
            Int128 result = Int128.MIN_VALUE.div(Int128.valueOf(-1));
            assertEquals(Int128.MIN_VALUE, result);
        }

        @Test
        @DisplayName("Unsigned division")
        void testDivideUnsigned() {
            Int128 a = Int128.fromUnsignedLong(-1L); // Large positive number
            Int128 b = Int128.valueOf(2);
            Int128 result = a.divideUnsigned(b);
            assertTrue(result.isPositive());
        }

        @Test
        @DisplayName("Unsigned remainder")
        void testRemainderUnsigned() {
            Int128 a = Int128.fromUnsignedLong(19L);
            Int128 b = Int128.valueOf(5);
            Int128 result = a.remainderUnsigned(b);
            assertEquals("4", result.toString());
        }

        @Test
        @DisplayName("divRemPow10() with small exponent")
        void testDivRemPow10Small() {
            Int128 val = Int128.valueOf(12345);
            Int128[] result = val.divRemPow10(2);
            assertEquals("123", result[0].toString());
            assertEquals("45", result[1].toString());
        }

        @Test
        @DisplayName("divRemPow10() with large exponent")
        void testDivRemPow10Large() {
            Int128 val = Int128.fromString("123456789012345678901234567890");
            Int128[] result = val.divRemPow10(20);
            assertNotNull(result[0]);
            assertNotNull(result[1]);
        }

        @Test
        @DisplayName("divRemPow10() throws on invalid exp")
        void testDivRemPow10Invalid() {
            Int128 val = Int128.valueOf(123);
            assertThrows(IllegalArgumentException.class, () -> val.divRemPow10(-1));
            assertThrows(IllegalArgumentException.class, () -> val.divRemPow10(39));
        }
    }

    @Nested
    @DisplayName("Negation and Absolute Value Tests")
    class NegationTests {

        @Test
        @DisplayName("Negate positive number")
        void testNegatePositive() {
            Int128 val = Int128.valueOf(12345);
            Int128 neg = val.negate();
            assertEquals("-12345", neg.toString());
        }

        @Test
        @DisplayName("Negate negative number")
        void testNegateNegative() {
            Int128 val = Int128.valueOf(-12345);
            Int128 neg = val.negate();
            assertEquals("12345", neg.toString());
        }

        @Test
        @DisplayName("Negate zero")
        void testNegateZero() {
            assertEquals(Int128.ZERO, Int128.ZERO.negate());
        }

        @Test
        @DisplayName("Double negation returns original")
        void testDoubleNegation() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(val, val.negate().negate());
        }

        @Test
        @DisplayName("Negate MIN_VALUE wraps to MIN_VALUE")
        void testNegateMinValue() {
            Int128 neg = Int128.MIN_VALUE.negate();
            assertEquals(Int128.MIN_VALUE, neg);
        }

        @Test
        @DisplayName("abs() of positive")
        void testAbsPositive() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(val, val.abs());
        }

        @Test
        @DisplayName("abs() of negative")
        void testAbsNegative() {
            Int128 val = Int128.valueOf(-12345);
            Int128 abs = val.abs();
            assertEquals("12345", abs.toString());
        }

        @Test
        @DisplayName("abs() of zero")
        void testAbsZero() {
            assertEquals(Int128.ZERO, Int128.ZERO.abs());
        }

        @Test
        @DisplayName("abs() of MIN_VALUE stays MIN_VALUE")
        void testAbsMinValue() {
            Int128 abs = Int128.MIN_VALUE.abs();
            assertEquals(Int128.MIN_VALUE, abs);
        }
    }

    @Nested
    @DisplayName("Increment/Decrement Tests")
    class IncrementDecrementTests {

        @Test
        @DisplayName("inc() increments by one")
        void testIncrement() {
            Int128 val = Int128.valueOf(99);
            Int128 result = val.inc();
            assertEquals("100", result.toString());
        }

        @Test
        @DisplayName("dec() decrements by one")
        void testDecrement() {
            Int128 val = Int128.valueOf(100);
            Int128 result = val.dec();
            assertEquals("99", result.toString());
        }

        @Test
        @DisplayName("inc() and dec() are inverses")
        void testIncDecInverse() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(val, val.inc().dec());
            assertEquals(val, val.dec().inc());
        }

        @Test
        @DisplayName("inc() on MAX_VALUE wraps to MIN_VALUE")
        void testIncrementMax() {
            assertEquals(Int128.MIN_VALUE, Int128.MAX_VALUE.inc());
        }

        @Test
        @DisplayName("dec() on MIN_VALUE wraps to MAX_VALUE")
        void testDecrementMin() {
            assertEquals(Int128.MAX_VALUE, Int128.MIN_VALUE.dec());
        }
    }

    // ========================================================================
    // Shift and Rotate Tests
    // ========================================================================

    @Nested
    @DisplayName("Shift Tests")
    class ShiftTests {

        @Test
        @DisplayName("shiftLeft() basic")
        void testShiftLeftBasic() {
            Int128 val = Int128.valueOf(1);
            Int128 result = val.shiftLeft(4);
            assertEquals("16", result.toString());
        }

        @Test
        @DisplayName("shiftLeft() by zero")
        void testShiftLeftZero() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(val, val.shiftLeft(0));
        }

        @Test
        @DisplayName("shiftLeft() by 64 moves low to high")
        void testShiftLeft64() {
            Int128 val = Int128.of(0L, 1L);
            Int128 result = val.shiftLeft(64);
            assertEquals(Int128.of(1L, 0L), result);
        }

        @Test
        @DisplayName("shiftLeft() masks to 0..127")
        void testShiftLeftMasking() {
            Int128 val = Int128.valueOf(1);
            assertEquals(val, val.shiftLeft(128)); // Should be same as shiftLeft(0)
        }

        @Test
        @DisplayName("shiftRight() basic")
        void testShiftRightBasic() {
            Int128 val = Int128.valueOf(16);
            Int128 result = val.shiftRight(4);
            assertEquals("1", result.toString());
        }

        @Test
        @DisplayName("shiftRight() arithmetic sign extension")
        void testShiftRightSignExtension() {
            Int128 val = Int128.valueOf(-16);
            Int128 result = val.shiftRight(2);
            assertTrue(result.isNegative());
        }

        @Test
        @DisplayName("shiftRight() by 64")
        void testShiftRight64() {
            Int128 val = Int128.of(123L, 0L);
            Int128 result = val.shiftRight(64);
            assertEquals(Int128.valueOf(123), result);
        }

        @Test
        @DisplayName("shiftRightUnsigned() zero extension")
        void testShiftRightUnsigned() {
            Int128 val = Int128.of(-1L, -1L); // All bits set
            Int128 result = val.shiftRightUnsigned(1);
            assertTrue(result.isPositive()); // Top bit should be zero
        }

        @Test
        @DisplayName("shiftRightUnsigned() by 64")
        void testShiftRightUnsigned64() {
            Int128 val = Int128.of(123L, 456L);
            Int128 result = val.shiftRightUnsigned(64);
            assertEquals(Int128.of(0L, 123L), result);
        }
    }

    // ========================================================================
    // Bitwise Operation Tests
    // ========================================================================

    @Nested
    @DisplayName("Bitwise Operation Tests")
    class BitwiseTests {

        @Test
        @DisplayName("not() inverts all bits")
        void testNot() {
            Int128 val = Int128.ZERO;
            Int128 result = val.not();
            assertEquals(Int128.of(-1L, -1L), result);
        }

        @Test
        @DisplayName("and() basic")
        void testAnd() {
            Int128 a = Int128.of(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFF00000000L);
            Int128 b = Int128.of(0x0000FFFFFFFF0000L, 0x00000000FFFFFFFFL);
            Int128 result = a.and(b);
            assertEquals(Int128.of(0x0000FFFFFFFF0000L, 0x0000000000000000L), result);
        }

        @Test
        @DisplayName("or() basic")
        void testOr() {
            Int128 a = Int128.of(0xFF00000000000000L, 0x00000000FF000000L);
            Int128 b = Int128.of(0x00FF000000000000L, 0x0000000000FF0000L);
            Int128 result = a.or(b);
            assertEquals(Int128.of(0xFFFF000000000000L, 0x00000000FFFF0000L), result);
        }

        @Test
        @DisplayName("xor() basic")
        void testXor() {
            Int128 a = Int128.of(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
            Int128 b = Int128.of(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L);
            Int128 result = a.xor(b);
            assertEquals(Int128.of(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL), result);
        }

        @Test
        @DisplayName("testBit() tests")
        void testTestBit() {
            Int128 val = Int128.of(0x8000000000000000L, 0x0000000000000001L);
            assertTrue(val.testBit(0));
            assertFalse(val.testBit(1));
            assertTrue(val.testBit(127));
        }

        @Test
        @DisplayName("testBit() out of range")
        void testTestBitOutOfRange() {
            Int128 val = Int128.ONE;
            assertThrows(IllegalArgumentException.class, () -> val.testBit(-1));
            assertThrows(IllegalArgumentException.class, () -> val.testBit(128));
        }

        @Test
        @DisplayName("setBit() tests")
        void testSetBit() {
            Int128 val = Int128.ZERO;
            Int128 result = val.setBit(5);
            assertTrue(result.testBit(5));
            assertEquals("32", result.toString());
        }

        @Test
        @DisplayName("clearBit() tests")
        void testClearBit() {
            Int128 val = Int128.of(-1L, -1L); // All bits set
            Int128 result = val.clearBit(0);
            assertFalse(result.testBit(0));
        }

        @Test
        @DisplayName("bitLength() tests")
        void testBitLength() {
            assertEquals(0, Int128.ZERO.bitLength());
            assertEquals(1, Int128.ONE.bitLength());
            assertEquals(8, Int128.valueOf(255).bitLength());
            assertEquals(127, Int128.MAX_VALUE.bitLength());
        }

        @Test
        @DisplayName("isPowerOfTwo() tests")
        void testIsPowerOfTwo() {
            assertFalse(Int128.ZERO.isPowerOfTwo());
            assertTrue(Int128.ONE.isPowerOfTwo());
            assertTrue(Int128.valueOf(2).isPowerOfTwo());
            assertTrue(Int128.valueOf(256).isPowerOfTwo());
            assertFalse(Int128.valueOf(3).isPowerOfTwo());
            assertFalse(Int128.valueOf(255).isPowerOfTwo());
            assertFalse(Int128.valueOf(-1).isPowerOfTwo());
        }
    }

    // ========================================================================
    // Comparison Tests
    // ========================================================================

    @Nested
    @DisplayName("Comparison Tests")
    class ComparisonTests {

        @Test
        @DisplayName("compareTo() less than")
        void testCompareToLess() {
            Int128 a = Int128.valueOf(100);
            Int128 b = Int128.valueOf(200);
            assertTrue(a.compareTo(b) < 0);
        }

        @Test
        @DisplayName("compareTo() greater than")
        void testCompareToGreater() {
            Int128 a = Int128.valueOf(200);
            Int128 b = Int128.valueOf(100);
            assertTrue(a.compareTo(b) > 0);
        }

        @Test
        @DisplayName("compareTo() equal")
        void testCompareToEqual() {
            Int128 a = Int128.valueOf(100);
            Int128 b = Int128.valueOf(100);
            assertEquals(0, a.compareTo(b));
        }

        @Test
        @DisplayName("compareTo() negative vs positive")
        void testCompareToNegativePositive() {
            Int128 neg = Int128.valueOf(-100);
            Int128 pos = Int128.valueOf(100);
            assertTrue(neg.compareTo(pos) < 0);
            assertTrue(pos.compareTo(neg) > 0);
        }

        @Test
        @DisplayName("compareUnsigned() tests")
        void testCompareUnsigned() {
            Int128 a = Int128.of(-1L, 0L); // Large unsigned
            Int128 b = Int128.valueOf(100);
            assertTrue(a.compareUnsigned(b) > 0); // Unsigned comparison
            assertTrue(a.compareTo(b) < 0);        // Signed comparison
        }

        @Test
        @DisplayName("signum() tests")
        void testSignum() {
            assertEquals(0, Int128.ZERO.signum());
            assertEquals(1, Int128.ONE.signum());
            assertEquals(1, Int128.valueOf(12345).signum());
            assertEquals(-1, Int128.valueOf(-12345).signum());
        }

        @Test
        @DisplayName("isPositive() tests")
        void testIsPositive() {
            assertTrue(Int128.ONE.isPositive());
            assertTrue(Int128.valueOf(12345).isPositive());
            assertFalse(Int128.ZERO.isPositive());
            assertFalse(Int128.valueOf(-12345).isPositive());
        }

        @Test
        @DisplayName("isNegative() tests")
        void testIsNegative() {
            assertTrue(Int128.valueOf(-12345).isNegative());
            assertFalse(Int128.ZERO.isNegative());
            assertFalse(Int128.ONE.isNegative());
            assertFalse(Int128.valueOf(12345).isNegative());
        }

        @Test
        @DisplayName("isZero() tests")
        void testIsZero() {
            assertTrue(Int128.ZERO.isZero());
            assertFalse(Int128.ONE.isZero());
            assertFalse(Int128.valueOf(-1).isZero());
        }

        @Test
        @DisplayName("min() tests")
        void testMin() {
            Int128 a = Int128.valueOf(100);
            Int128 b = Int128.valueOf(200);
            assertEquals(a, Int128.min(a, b));
            assertEquals(a, Int128.min(b, a));
        }

        @Test
        @DisplayName("max() tests")
        void testMax() {
            Int128 a = Int128.valueOf(100);
            Int128 b = Int128.valueOf(200);
            assertEquals(b, Int128.max(a, b));
            assertEquals(b, Int128.max(b, a));
        }

        @Test
        @DisplayName("clamp() tests")
        void testClamp() {
            Int128 min = Int128.valueOf(0);
            Int128 max = Int128.valueOf(100);

            assertEquals(Int128.valueOf(50), Int128.clamp(Int128.valueOf(50), min, max));
            assertEquals(min, Int128.clamp(Int128.valueOf(-10), min, max));
            assertEquals(max, Int128.clamp(Int128.valueOf(200), min, max));
        }

        @Test
        @DisplayName("clamp() throws on invalid bounds")
        void testClampInvalidBounds() {
            assertThrows(IllegalArgumentException.class,
                () -> Int128.clamp(Int128.ZERO, Int128.valueOf(100), Int128.valueOf(0)));
        }
    }

    // ========================================================================
    // Equality and Hashing Tests
    // ========================================================================

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("equals() reflexive")
        void testEqualsReflexive() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(val, val);
        }

        @Test
        @DisplayName("equals() symmetric")
        void testEqualsSymmetric() {
            Int128 a = Int128.valueOf(12345);
            Int128 b = Int128.valueOf(12345);
            assertEquals(a, b);
            assertEquals(b, a);
        }

        @Test
        @DisplayName("equals() with different values")
        void testEqualsDifferent() {
            Int128 a = Int128.valueOf(12345);
            Int128 b = Int128.valueOf(54321);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("equals() with null")
        void testEqualsNull() {
            Int128 val = Int128.valueOf(12345);
            assertNotEquals(null, val);
        }

        @Test
        @DisplayName("hashCode() consistency")
        void testHashCodeConsistency() {
            Int128 val = Int128.valueOf(12345);
            int hash1 = val.hashCode();
            int hash2 = val.hashCode();
            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("hashCode() for equal objects")
        void testHashCodeEqual() {
            Int128 a = Int128.valueOf(12345);
            Int128 b = Int128.valueOf(12345);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }

    // ========================================================================
    // String Conversion Tests
    // ========================================================================

    @Nested
    @DisplayName("String Conversion Tests")
    class StringConversionTests {

        @Test
        @DisplayName("toString() basic")
        void testToString() {
            assertEquals("0", Int128.ZERO.toString());
            assertEquals("1", Int128.ONE.toString());
            assertEquals("12345", Int128.valueOf(12345).toString());
            assertEquals("-12345", Int128.valueOf(-12345).toString());
        }

        @Test
        @DisplayName("toString() round-trip")
        void testToStringRoundTrip() {
            String[] testValues = {
                "0", "1", "-1", "12345", "-12345",
                "9999999999999999999",
                "123456789012345678901234567890",
                "170141183460469231731687303715884105727",
                "-170141183460469231731687303715884105728"
            };

            for (String testValue : testValues) {
                Int128 val = Int128.fromString(testValue);
                assertEquals(testValue, val.toString(), "Round-trip failed for: " + testValue);
            }
        }

        @Test
        @DisplayName("toString(radix) tests")
        void testToStringRadix() {
            Int128 val = Int128.valueOf(255);
            assertEquals("ff", val.toString(16).toLowerCase());
            assertEquals("377", val.toString(8));
            assertEquals("11111111", val.toString(2));
        }

        @Test
        @DisplayName("toHexString() tests")
        void testToHexString() {
            assertEquals("0", Int128.ZERO.toHexString());
            assertEquals("ff", Int128.valueOf(255).toHexString());
            assertFalse(Int128.valueOf(255).toHexString().startsWith("0x"));
        }

        @Test
        @DisplayName("toDebugHex() format")
        void testToDebugHex() {
            Int128 val = Int128.of(0x123456789ABCDEF0L, 0xFEDCBA9876543210L);
            String hex = val.toDebugHex();
            assertTrue(hex.startsWith("0x"));
            assertTrue(hex.contains("_"));
        }
    }

    // ========================================================================
    // Financial/Decimal Operations Tests
    // ========================================================================

    @Nested
    @DisplayName("Financial Operations Tests")
    class FinancialOperationsTests {

        @Test
        @DisplayName("divRoundHalfEvenPow10() rounds correctly")
        void testDivRoundHalfEven() {
            // 15 / 10 = 1.5 → rounds to 2 (nearest even)
            Int128 val = Int128.valueOf(15);
            Int128 result = val.divRoundHalfEvenPow10(1);
            assertEquals("2", result.toString());

            // 25 / 10 = 2.5 → rounds to 2 (nearest even)
            val = Int128.valueOf(25);
            result = val.divRoundHalfEvenPow10(1);
            assertEquals("2", result.toString());

            // 35 / 10 = 3.5 → rounds to 4 (nearest even)
            val = Int128.valueOf(35);
            result = val.divRoundHalfEvenPow10(1);
            assertEquals("4", result.toString());
        }

        @Test
        @DisplayName("floorDivPow10() rounds toward negative infinity")
        void testFloorDivPow10() {
            Int128 val = Int128.valueOf(23);
            assertEquals("2", val.floorDivPow10(1).toString());

            val = Int128.valueOf(-23);
            assertEquals("-3", val.floorDivPow10(1).toString());
        }

        @Test
        @DisplayName("ceilDivPow10() rounds toward positive infinity")
        void testCeilDivPow10() {
            Int128 val = Int128.valueOf(23);
            assertEquals("3", val.ceilDivPow10(1).toString());

            val = Int128.valueOf(-23);
            assertEquals("-2", val.ceilDivPow10(1).toString());
        }

        @Test
        @DisplayName("mulAndDivPow10RoundHalfEven() for fixed-point calculations")
        void testMulAndDivPow10() {
            Int128 principal = Int128.valueOf(10000);
            Int128 rate = Int128.valueOf(105); // 1.05 represented as 105/100
            Int128 result = principal.mulAndDivPow10RoundHalfEven(rate, 2);
            assertEquals("10500", result.toString()); // 10000 * 1.05
        }

        @Test
        @DisplayName("mulLongAndDivPow10RoundHalfEven() convenience method")
        void testMulLongAndDivPow10() {
            Int128 principal = Int128.valueOf(10000);
            Int128 result = principal.mulLongAndDivPow10RoundHalfEven(105, 2);
            assertEquals("10500", result.toString());
        }

        @Test
        @DisplayName("tenPow() generates powers of 10")
        void testTenPow() {
            assertEquals("1", Int128.tenPow(0).toString());
            assertEquals("10", Int128.tenPow(1).toString());
            assertEquals("100", Int128.tenPow(2).toString());
            assertEquals("1000000000", Int128.tenPow(9).toString());
        }

        @Test
        @DisplayName("tenPow() throws on invalid exponent")
        void testTenPowInvalid() {
            assertThrows(IllegalArgumentException.class, () -> Int128.tenPow(-1));
            assertThrows(IllegalArgumentException.class, () -> Int128.tenPow(39));
        }

        @Test
        @DisplayName("decimalBase() returns 10")
        void testDecimalBase() {
            assertEquals(Int128.valueOf(10), Int128.decimalBase());
        }
    }

    // ========================================================================
    // Conversion Tests
    // ========================================================================

    @Nested
    @DisplayName("Conversion Tests")
    class ConversionTests {

        @Test
        @DisplayName("toLong() returns low word")
        void testToLong() {
            Int128 val = Int128.valueOf(12345);
            assertEquals(12345L, val.toLong());
        }

        @Test
        @DisplayName("toLongExact() succeeds for values in long range")
        void testToLongExact() {
            assertEquals(0L, Int128.ZERO.toLongExact());
            assertEquals(12345L, Int128.valueOf(12345).toLongExact());
            assertEquals(-12345L, Int128.valueOf(-12345).toLongExact());
            assertEquals(Long.MAX_VALUE, Int128.valueOf(Long.MAX_VALUE).toLongExact());
            assertEquals(Long.MIN_VALUE, Int128.valueOf(Long.MIN_VALUE).toLongExact());
        }

        @Test
        @DisplayName("toLongExact() throws for values out of range")
        void testToLongExactOutOfRange() {
            assertThrows(ArithmeticException.class, () -> Int128.MAX_VALUE.toLongExact());
            assertThrows(ArithmeticException.class, () -> Int128.MIN_VALUE.toLongExact());
        }

        @Test
        @DisplayName("toIntExact() succeeds for values in int range")
        void testToIntExact() {
            assertEquals(0, Int128.ZERO.toIntExact());
            assertEquals(12345, Int128.valueOf(12345).toIntExact());
            assertEquals(-12345, Int128.valueOf(-12345).toIntExact());
        }

        @Test
        @DisplayName("toIntExact() throws for values out of range")
        void testToIntExactOutOfRange() {
            assertThrows(ArithmeticException.class, () -> Int128.valueOf(Long.MAX_VALUE).toIntExact());
            assertThrows(ArithmeticException.class, () -> Int128.MAX_VALUE.toIntExact());
        }

        @Test
        @DisplayName("fitsInLong() tests")
        void testFitsInLong() {
            assertTrue(Int128.ZERO.fitsInLong());
            assertTrue(Int128.ONE.fitsInLong());
            assertTrue(Int128.valueOf(Long.MAX_VALUE).fitsInLong());
            assertTrue(Int128.valueOf(Long.MIN_VALUE).fitsInLong());
            assertFalse(Int128.MAX_VALUE.fitsInLong());
            assertFalse(Int128.MIN_VALUE.fitsInLong());
        }

        @Test
        @DisplayName("fitsInUnsignedLong() tests")
        void testFitsInUnsignedLong() {
            assertTrue(Int128.ZERO.fitsInUnsignedLong());
            assertTrue(Int128.ONE.fitsInUnsignedLong());
            assertTrue(Int128.fromUnsignedLong(-1L).fitsInUnsignedLong());
            assertFalse(Int128.of(1L, 0L).fitsInUnsignedLong());
        }

        @Test
        @DisplayName("toBytesBE() produces 16 bytes")
        void testToBytesBE() {
            Int128 val = Int128.valueOf(12345);
            byte[] bytes = val.toBytesBE();
            assertEquals(16, bytes.length);
        }

        @Test
        @DisplayName("fromBytesBE() round-trip")
        void testFromBytesBERoundTrip() {
            Int128 original = Int128.valueOf(12345);
            byte[] bytes = original.toBytesBE();
            Int128 reconstructed = Int128.fromBytesBE(bytes);
            assertEquals(original, reconstructed);
        }

        @Test
        @DisplayName("fromBytesBE() throws on wrong size")
        void testFromBytesBEWrongSize() {
            assertThrows(IllegalArgumentException.class, () -> Int128.fromBytesBE(new byte[15]));
            assertThrows(IllegalArgumentException.class, () -> Int128.fromBytesBE(new byte[17]));
            assertThrows(IllegalArgumentException.class, () -> Int128.fromBytesBE(null));
        }

        @Test
        @DisplayName("ByteBuffer putTo/getFrom round-trip")
        void testByteBufferRoundTrip() {
            Int128 original = Int128.valueOf(12345);
            ByteBuffer buffer = ByteBuffer.allocate(16);
            original.putTo(buffer);
            buffer.flip();
            Int128 reconstructed = Int128.getFrom(buffer);
            assertEquals(original, reconstructed);
        }
    }

    // ========================================================================
    // Edge Cases and Stress Tests
    // ========================================================================

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Addition overflow wraps correctly")
        void testAdditionOverflow() {
            Int128 result = Int128.MAX_VALUE.add(Int128.ONE);
            assertEquals(Int128.MIN_VALUE, result);
        }

        @Test
        @DisplayName("Subtraction underflow wraps correctly")
        void testSubtractionUnderflow() {
            Int128 result = Int128.MIN_VALUE.sub(Int128.ONE);
            assertEquals(Int128.MAX_VALUE, result);
        }

        @Test
        @DisplayName("Multiplication overflow wraps")
        void testMultiplicationOverflow() {
            Int128 result = Int128.MAX_VALUE.mul(Int128.valueOf(2));
            assertNotEquals(Int128.MAX_VALUE, result);
        }

        @Test
        @DisplayName("Division by MIN_VALUE")
        void testDivisionByMinValue() {
            Int128 result = Int128.MAX_VALUE.div(Int128.MIN_VALUE);
            assertEquals(Int128.ZERO, result);
        }

        @Test
        @DisplayName("Large value string parsing and printing")
        void testLargeValues() {
            String largeNum = "123456789012345678901234567890123456789";
            Int128 val = Int128.fromString(largeNum);
            assertEquals(largeNum, val.toString());
        }

        @Test
        @DisplayName("Negative values with high bit set in low word")
        void testNegativeHighBit() {
            Int128 val = Int128.of(-1L, 0x8000000000000000L);
            assertTrue(val.isNegative());
        }

        @Test
        @DisplayName("Division identity with large numbers")
        void testDivisionIdentityLarge() {
            Int128 a = Int128.fromString("123456789012345678901234567890");
            Int128 b = Int128.fromString("987654321");
            Int128[] dr = a.divRem(b);
            Int128 reconstructed = dr[0].mul(b).add(dr[1]);
            assertEquals(a, reconstructed);
        }
    }

    @Nested
    @DisplayName("Stress Tests")
    class StressTests {

        @Test
        @DisplayName("Many sequential additions")
        void testManyAdditions() {
            Int128 sum = Int128.ZERO;
            for (int i = 1; i <= 1000; i++) {
                sum = sum.add(Int128.valueOf(i));
            }
            // Sum of 1 to 1000 = 500500
            assertEquals("500500", sum.toString());
        }

        @Test
        @DisplayName("Many sequential multiplications")
        void testManyMultiplications() {
            Int128 product = Int128.ONE;
            for (int i = 1; i <= 10; i++) {
                product = product.mul(Int128.valueOf(2));
            }
            // 2^10 = 1024
            assertEquals("1024", product.toString());
        }

        @Test
        @DisplayName("Alternating add/subtract")
        void testAlternatingOps() {
            Int128 val = Int128.valueOf(1000);
            for (int i = 0; i < 100; i++) {
                val = val.add(Int128.valueOf(10));
                val = val.sub(Int128.valueOf(10));
            }
            assertEquals("1000", val.toString());
        }

        @Test
        @DisplayName("Round-trip through all decimal exponents")
        void testDecimalExponents() {
            for (int exp = 0; exp < 20; exp++) {
                Int128 val = Int128.ONE.scaleUpPow10(exp);
                Int128[] dr = val.divRemPow10(exp);
                assertEquals(Int128.ONE, dr[0]);
                assertEquals(Int128.ZERO, dr[1]);
            }
        }
    }

    // ========================================================================
    // Self-Check Test
    // ========================================================================

    @Nested
    @DisplayName("Quick Self Check")
    class SelfCheckTests {

        @Test
        @DisplayName("quickSelfCheck() should pass")
        void testQuickSelfCheck() {
            assertDoesNotThrow(() -> Int128.quickSelfCheck());
        }
    }

    // ========================================================================
    // Real-World Financial Scenarios
    // ========================================================================

    @Nested
    @DisplayName("Financial Calculation Scenarios")
    class FinancialScenarioTests {

        @Test
        @DisplayName("Currency conversion with high precision")
        void testCurrencyConversion() {
            // Convert $1,000,000.00 at rate 1.234567
            Int128 amount = Int128.valueOf(100000000); // In cents
            Int128 rate = Int128.valueOf(1234567); // Rate * 1000000
            Int128 result = amount.mul(rate).div(Int128.valueOf(1000000));
            assertEquals("123456700000", result.toString());
        }

        @Test
        @DisplayName("Compound interest calculation")
        void testCompoundInterest() {
            // Principal: $10,000, Rate: 5% annually, Period: 1 year
            Int128 principal = Int128.valueOf(1000000); // In cents
            Int128 rate = Int128.valueOf(105); // 1.05 * 100
            Int128 result = principal.mulLongAndDivPow10RoundHalfEven(rate, 2);
            assertEquals("1050000", result.toString());
        }

        @Test
        @DisplayName("Portfolio value calculation")
        void testPortfolioValue() {
            // 1 trillion shares at $100.50 each
            Int128 shares = Int128.fromString("1000000000000");
            Int128 priceInCents = Int128.valueOf(10050);
            Int128 totalValue = shares.mul(priceInCents);
            assertEquals("10050000000000000", totalValue.toString());
        }

        @Test
        @DisplayName("Fee calculation with rounding")
        void testFeeCalculation() {
            // Transaction: $1,234.56, Fee: 0.25%
            Int128 amount = Int128.valueOf(123456); // In cents
            Int128 feeRate = Int128.valueOf(25); // 0.25 * 100
            Int128 fee = amount.mulLongAndDivPow10RoundHalfEven(feeRate, 4);
            assertEquals("309", fee.toString()); // $3.09
        }

        @Test
        @DisplayName("Spread calculation in trading")
        void testSpreadCalculation() {
            Int128 bid = Int128.valueOf(9950);
            Int128 ask = Int128.valueOf(10050);
            Int128 spread = ask.sub(bid);
            Int128 midpoint = bid.add(ask).div(Int128.valueOf(2));
            assertEquals("100", spread.toString());
            assertEquals("10000", midpoint.toString());
        }

        @Test
        @DisplayName("Price-per-unit with high volume")
        void testPricePerUnit() {
            // Total cost: $1,000,000, Units: 987,654
            Int128 totalCost = Int128.valueOf(100000000); // In cents
            Int128 units = Int128.valueOf(987654);
            Int128 pricePerUnit = totalCost.mul(100).div(units);
            assertNotNull(pricePerUnit);
            assertTrue(pricePerUnit.isPositive());
        }
    }

    // ========================================================================
    // Correctness Verification with BigInteger
    // ========================================================================

    @Nested
    @DisplayName("BigInteger Correctness Tests")
    class BigIntegerCorrectnessTests {

        private BigInteger toBigInteger(Int128 val) {
            return new BigInteger(val.toBytesBE());
        }

        @Test
        @DisplayName("Addition matches BigInteger")
        void testAdditionMatchesBigInteger() {
            Int128 a = Int128.fromString("12345678901234567890");
            Int128 b = Int128.fromString("98765432109876543210");
            Int128 result = a.add(b);

            BigInteger bigA = toBigInteger(a);
            BigInteger bigB = toBigInteger(b);
            BigInteger bigResult = bigA.add(bigB);

            assertEquals(bigResult.toString(), result.toString());
        }

        @Test
        @DisplayName("Subtraction matches BigInteger")
        void testSubtractionMatchesBigInteger() {
            Int128 a = Int128.fromString("98765432109876543210");
            Int128 b = Int128.fromString("12345678901234567890");
            Int128 result = a.sub(b);

            BigInteger bigA = toBigInteger(a);
            BigInteger bigB = toBigInteger(b);
            BigInteger bigResult = bigA.subtract(bigB);

            assertEquals(bigResult.toString(), result.toString());
        }

        @Test
        @DisplayName("Multiplication matches BigInteger (within 128-bit range)")
        void testMultiplicationMatchesBigInteger() {
            Int128 a = Int128.fromString("1000000000");
            Int128 b = Int128.fromString("1000000");
            Int128 result = a.mul(b);

            BigInteger bigA = toBigInteger(a);
            BigInteger bigB = toBigInteger(b);
            BigInteger bigResult = bigA.multiply(bigB);

            // Mask to 128 bits and handle sign
            BigInteger mask = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
            bigResult = bigResult.and(mask);
            if (bigResult.testBit(127)) {
                bigResult = bigResult.subtract(BigInteger.ONE.shiftLeft(128));
            }

            assertEquals(bigResult.toString(), result.toString());
        }

        @Test
        @DisplayName("Division matches BigInteger")
        void testDivisionMatchesBigInteger() {
            Int128 a = Int128.fromString("123456789012345");
            Int128 b = Int128.fromString("987654");
            Int128 result = a.div(b);

            BigInteger bigA = toBigInteger(a);
            BigInteger bigB = toBigInteger(b);
            BigInteger bigResult = bigA.divide(bigB);

            assertEquals(bigResult.toString(), result.toString());
        }

        @Test
        @DisplayName("Remainder matches BigInteger")
        void testRemainderMatchesBigInteger() {
            Int128 a = Int128.fromString("123456789012345");
            Int128 b = Int128.fromString("987654");
            Int128 result = a.rem(b);

            BigInteger bigA = toBigInteger(a);
            BigInteger bigB = toBigInteger(b);
            BigInteger bigResult = bigA.remainder(bigB);

            assertEquals(bigResult.toString(), result.toString());
        }

        @Test
        @DisplayName("Negation matches BigInteger")
        void testNegationMatchesBigInteger() {
            Int128 val = Int128.fromString("12345678901234567890");
            Int128 neg = val.negate();

            BigInteger bigVal = toBigInteger(val);
            BigInteger bigNeg = bigVal.negate();

            assertEquals(bigNeg.toString(), neg.toString());
        }
    }
}
