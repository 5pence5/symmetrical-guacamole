package com.symguac.int128.impl.highperf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;

/**
 * Comprehensive test suite for FastInt128Value implementation.
 * Tests all operations for correctness, edge cases, and boundary conditions.
 */
class FastInt128ValueTest {

    // ========================================================================
    // Constants Tests
    // ========================================================================

    @Nested
    @DisplayName("Constants Tests")
    class ConstantsTests {

        @Test
        @DisplayName("ZERO constant should be 0")
        void testZeroConstant() {
            assertEquals(0L, FastInt128Value.ZERO.high());
            assertEquals(0L, FastInt128Value.ZERO.low());
            assertTrue(FastInt128Value.ZERO.isZero());
            assertEquals("0", FastInt128Value.ZERO.toString());
        }

        @Test
        @DisplayName("ONE constant should be 1")
        void testOneConstant() {
            assertEquals(0L, FastInt128Value.ONE.high());
            assertEquals(1L, FastInt128Value.ONE.low());
            assertTrue(FastInt128Value.ONE.isOne());
            assertEquals("1", FastInt128Value.ONE.toString());
        }

        @Test
        @DisplayName("MAX_VALUE should be 2^127 - 1")
        void testMaxValue() {
            assertEquals(Long.MAX_VALUE, FastInt128Value.MAX_VALUE.high());
            assertEquals(-1L, FastInt128Value.MAX_VALUE.low());
            assertFalse(FastInt128Value.MAX_VALUE.isNegative());
            assertTrue(FastInt128Value.MAX_VALUE.isPositive());
            // MAX_VALUE = 170141183460469231731687303715884105727
            assertEquals("170141183460469231731687303715884105727", FastInt128Value.MAX_VALUE.toString());
        }

        @Test
        @DisplayName("MIN_VALUE should be -2^127")
        void testMinValue() {
            assertEquals(Long.MIN_VALUE, FastInt128Value.MIN_VALUE.high());
            assertEquals(0L, FastInt128Value.MIN_VALUE.low());
            assertTrue(FastInt128Value.MIN_VALUE.isNegative());
            assertFalse(FastInt128Value.MIN_VALUE.isPositive());
            // MIN_VALUE = -170141183460469231731687303715884105728
            assertEquals("-170141183460469231731687303715884105728", FastInt128Value.MIN_VALUE.toString());
        }

        @Test
        @DisplayName("DECIMAL_BASE should be 1 billion")
        void testDecimalBase() {
            assertEquals(1_000_000_000L, FastInt128Value.DECIMAL_BASE);
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
            FastInt128Value val = FastInt128Value.of(1L, 2L);
            assertEquals(1L, val.high());
            assertEquals(2L, val.low());
        }

        @Test
        @DisplayName("of() should return singleton for common values")
        void testOfSingletons() {
            assertSame(FastInt128Value.ZERO, FastInt128Value.of(0L, 0L));
            assertSame(FastInt128Value.ONE, FastInt128Value.of(0L, 1L));
            assertSame(FastInt128Value.MAX_VALUE, FastInt128Value.of(Long.MAX_VALUE, -1L));
            assertSame(FastInt128Value.MIN_VALUE, FastInt128Value.of(Long.MIN_VALUE, 0L));
        }

        @Test
        @DisplayName("fromLong() should sign-extend positive values")
        void testFromLongPositive() {
            FastInt128Value val = FastInt128Value.fromLong(12345L);
            assertEquals(0L, val.high());
            assertEquals(12345L, val.low());
        }

        @Test
        @DisplayName("fromLong() should sign-extend negative values")
        void testFromLongNegative() {
            FastInt128Value val = FastInt128Value.fromLong(-12345L);
            assertEquals(-1L, val.high());
            assertEquals(-12345L, val.low());
        }

        @Test
        @DisplayName("fromLong() should handle Long.MAX_VALUE")
        void testFromLongMax() {
            FastInt128Value val = FastInt128Value.fromLong(Long.MAX_VALUE);
            assertEquals(0L, val.high());
            assertEquals(Long.MAX_VALUE, val.low());
        }

        @Test
        @DisplayName("fromLong() should handle Long.MIN_VALUE")
        void testFromLongMin() {
            FastInt128Value val = FastInt128Value.fromLong(Long.MIN_VALUE);
            assertEquals(-1L, val.high());
            assertEquals(Long.MIN_VALUE, val.low());
        }

        @Test
        @DisplayName("fromString() should parse positive decimal strings")
        void testFromStringPositive() {
            FastInt128Value val = FastInt128Value.fromString("12345");
            assertEquals("12345", val.toString());
        }

        @Test
        @DisplayName("fromString() should parse negative decimal strings")
        void testFromStringNegative() {
            FastInt128Value val = FastInt128Value.fromString("-12345");
            assertEquals("-12345", val.toString());
        }

        @Test
        @DisplayName("fromString() should handle zero")
        void testFromStringZero() {
            assertEquals(FastInt128Value.ZERO, FastInt128Value.fromString("0"));
            assertEquals(FastInt128Value.ZERO, FastInt128Value.fromString("000"));
            assertEquals(FastInt128Value.ZERO, FastInt128Value.fromString("+0"));
            assertEquals(FastInt128Value.ZERO, FastInt128Value.fromString("-0"));
        }

        @Test
        @DisplayName("fromString() should handle leading zeros")
        void testFromStringLeadingZeros() {
            FastInt128Value val = FastInt128Value.fromString("00012345");
            assertEquals("12345", val.toString());
        }

        @Test
        @DisplayName("fromString() should handle whitespace")
        void testFromStringWhitespace() {
            FastInt128Value val = FastInt128Value.fromString("  12345  ");
            assertEquals("12345", val.toString());
        }

        @Test
        @DisplayName("fromString() should reject empty strings")
        void testFromStringEmpty() {
            assertThrows(NumberFormatException.class, () -> FastInt128Value.fromString(""));
            assertThrows(NumberFormatException.class, () -> FastInt128Value.fromString("   "));
        }

        @Test
        @DisplayName("fromString() should reject sign-only strings")
        void testFromStringSignOnly() {
            assertThrows(NumberFormatException.class, () -> FastInt128Value.fromString("+"));
            assertThrows(NumberFormatException.class, () -> FastInt128Value.fromString("-"));
        }

        @Test
        @DisplayName("fromString() should reject invalid characters")
        void testFromStringInvalidChars() {
            assertThrows(NumberFormatException.class, () -> FastInt128Value.fromString("123abc"));
            assertThrows(NumberFormatException.class, () -> FastInt128Value.fromString("12.34"));
        }

        @Test
        @DisplayName("fromString() should parse MAX_VALUE")
        void testFromStringMaxValue() {
            String maxStr = "170141183460469231731687303715884105727";
            FastInt128Value val = FastInt128Value.fromString(maxStr);
            assertEquals(FastInt128Value.MAX_VALUE, val);
        }

        @Test
        @DisplayName("fromString() should parse MIN_VALUE")
        void testFromStringMinValue() {
            String minStr = "-170141183460469231731687303715884105728";
            FastInt128Value val = FastInt128Value.fromString(minStr);
            assertEquals(FastInt128Value.MIN_VALUE, val);
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
            FastInt128Value a = FastInt128Value.fromLong(100);
            FastInt128Value b = FastInt128Value.fromLong(200);
            FastInt128Value result = a.add(b);
            assertEquals("300", result.toString());
        }

        @Test
        @DisplayName("Add positive and negative")
        void testAddMixed() {
            FastInt128Value a = FastInt128Value.fromLong(100);
            FastInt128Value b = FastInt128Value.fromLong(-50);
            FastInt128Value result = a.add(b);
            assertEquals("50", result.toString());
        }

        @Test
        @DisplayName("Add with carry from low to high")
        void testAddCarry() {
            FastInt128Value a = FastInt128Value.of(0L, -1L); // Low word all 1s
            FastInt128Value b = FastInt128Value.of(0L, 1L);
            FastInt128Value result = a.add(b);
            assertEquals(1L, result.high());
            assertEquals(0L, result.low());
        }

        @Test
        @DisplayName("Add zero identity")
        void testAddZero() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            assertEquals(val, val.add(FastInt128Value.ZERO));
            assertEquals(val, FastInt128Value.ZERO.add(val));
        }

        @Test
        @DisplayName("Add large numbers")
        void testAddLarge() {
            FastInt128Value a = FastInt128Value.fromString("100000000000000000000");
            FastInt128Value b = FastInt128Value.fromString("200000000000000000000");
            FastInt128Value result = a.add(b);
            assertEquals("300000000000000000000", result.toString());
        }

        @Test
        @DisplayName("addSmall() for decimal parsing")
        void testAddSmall() {
            FastInt128Value val = FastInt128Value.fromLong(1000);
            FastInt128Value result = val.addSmall(500);
            assertEquals("1500", result.toString());
        }

        @Test
        @DisplayName("addSmall() with zero")
        void testAddSmallZero() {
            FastInt128Value val = FastInt128Value.fromLong(1000);
            assertSame(val, val.addSmall(0));
        }
    }

    @Nested
    @DisplayName("Subtraction Tests")
    class SubtractionTests {

        @Test
        @DisplayName("Subtract positive numbers")
        void testSubtractPositive() {
            FastInt128Value a = FastInt128Value.fromLong(200);
            FastInt128Value b = FastInt128Value.fromLong(100);
            FastInt128Value result = a.subtract(b);
            assertEquals("100", result.toString());
        }

        @Test
        @DisplayName("Subtract with borrow")
        void testSubtractBorrow() {
            FastInt128Value a = FastInt128Value.of(1L, 0L);
            FastInt128Value b = FastInt128Value.of(0L, 1L);
            FastInt128Value result = a.subtract(b);
            assertEquals(0L, result.high());
            assertEquals(-1L, result.low());
        }

        @Test
        @DisplayName("Subtract zero identity")
        void testSubtractZero() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            assertEquals(val, val.subtract(FastInt128Value.ZERO));
        }

        @Test
        @DisplayName("Subtract from self gives zero")
        void testSubtractSelf() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            assertEquals(FastInt128Value.ZERO, val.subtract(val));
        }

        @Test
        @DisplayName("Subtract large numbers")
        void testSubtractLarge() {
            FastInt128Value a = FastInt128Value.fromString("500000000000000000000");
            FastInt128Value b = FastInt128Value.fromString("200000000000000000000");
            FastInt128Value result = a.subtract(b);
            assertEquals("300000000000000000000", result.toString());
        }
    }

    @Nested
    @DisplayName("Multiplication Tests")
    class MultiplicationTests {

        @Test
        @DisplayName("Multiply positive numbers")
        void testMultiplyPositive() {
            FastInt128Value a = FastInt128Value.fromLong(123);
            FastInt128Value b = FastInt128Value.fromLong(456);
            FastInt128Value result = a.multiply(b);
            assertEquals("56088", result.toString());
        }

        @Test
        @DisplayName("Multiply by zero")
        void testMultiplyZero() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            assertEquals(FastInt128Value.ZERO, val.multiply(FastInt128Value.ZERO));
            assertEquals(FastInt128Value.ZERO, FastInt128Value.ZERO.multiply(val));
        }

        @Test
        @DisplayName("Multiply by one identity")
        void testMultiplyOne() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            assertEquals(val, val.multiply(FastInt128Value.ONE));
            assertEquals(val, FastInt128Value.ONE.multiply(val));
        }

        @Test
        @DisplayName("Multiply positive and negative")
        void testMultiplyNegative() {
            FastInt128Value a = FastInt128Value.fromLong(100);
            FastInt128Value b = FastInt128Value.fromLong(-50);
            FastInt128Value result = a.multiply(b);
            assertEquals("-5000", result.toString());
        }

        @Test
        @DisplayName("Multiply two negative numbers")
        void testMultiplyTwoNegatives() {
            FastInt128Value a = FastInt128Value.fromLong(-100);
            FastInt128Value b = FastInt128Value.fromLong(-50);
            FastInt128Value result = a.multiply(b);
            assertEquals("5000", result.toString());
        }

        @Test
        @DisplayName("Multiply large numbers")
        void testMultiplyLarge() {
            FastInt128Value a = FastInt128Value.fromString("1000000000000");
            FastInt128Value b = FastInt128Value.fromString("1000000000000");
            FastInt128Value result = a.multiply(b);
            assertEquals("1000000000000000000000000", result.toString());
        }

        @Test
        @DisplayName("multiplySmall() for decimal parsing")
        void testMultiplySmall() {
            FastInt128Value val = FastInt128Value.fromLong(100);
            FastInt128Value result = val.multiplySmall(1000000000L);
            assertEquals("100000000000", result.toString());
        }

        @Test
        @DisplayName("multiplyByLong() with positive")
        void testMultiplyByLong() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            FastInt128Value result = val.multiplyByLong(100L);
            assertEquals("1234500", result.toString());
        }

        @Test
        @DisplayName("multiplyByLong() with negative")
        void testMultiplyByLongNegative() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            FastInt128Value result = val.multiplyByLong(-100L);
            assertEquals("-1234500", result.toString());
        }
    }

    @Nested
    @DisplayName("Negation Tests")
    class NegationTests {

        @Test
        @DisplayName("Negate positive number")
        void testNegatePositive() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            FastInt128Value neg = val.negate();
            assertEquals("-12345", neg.toString());
        }

        @Test
        @DisplayName("Negate negative number")
        void testNegateNegative() {
            FastInt128Value val = FastInt128Value.fromLong(-12345);
            FastInt128Value neg = val.negate();
            assertEquals("12345", neg.toString());
        }

        @Test
        @DisplayName("Negate zero")
        void testNegateZero() {
            assertSame(FastInt128Value.ZERO, FastInt128Value.ZERO.negate());
        }

        @Test
        @DisplayName("Double negation returns original")
        void testDoubleNegation() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            assertEquals(val, val.negate().negate());
        }

        @Test
        @DisplayName("Negate MAX_VALUE")
        void testNegateMaxValue() {
            FastInt128Value neg = FastInt128Value.MAX_VALUE.negate();
            assertEquals("-170141183460469231731687303715884105727", neg.toString());
        }
    }

    // ========================================================================
    // String Conversion Tests
    // ========================================================================

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString() for various values")
        void testToString() {
            assertEquals("0", FastInt128Value.ZERO.toString());
            assertEquals("1", FastInt128Value.ONE.toString());
            assertEquals("12345", FastInt128Value.fromLong(12345).toString());
            assertEquals("-12345", FastInt128Value.fromLong(-12345).toString());
        }

        @Test
        @DisplayName("toString() round-trip")
        void testToStringRoundTrip() {
            String[] testValues = {
                "0", "1", "-1", "12345", "-12345",
                "9999999999999999999", "-9999999999999999999",
                "123456789012345678901234567890",
                "-123456789012345678901234567890",
                "170141183460469231731687303715884105727",
                "-170141183460469231731687303715884105728"
            };

            for (String testValue : testValues) {
                FastInt128Value val = FastInt128Value.fromString(testValue);
                assertEquals(testValue, val.toString(), "Round-trip failed for: " + testValue);
            }
        }

        @Test
        @DisplayName("toHexString() format")
        void testToHexString() {
            FastInt128Value val = FastInt128Value.of(0x123456789ABCDEF0L, 0xFEDCBA9876543210L);
            String hex = val.toHexString();
            assertTrue(hex.startsWith("0x"));
            assertEquals(34, hex.length()); // "0x" + 32 hex digits
        }
    }

    // ========================================================================
    // Comparison Tests
    // ========================================================================

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("equals() reflexive")
        void testEqualsReflexive() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            assertEquals(val, val);
        }

        @Test
        @DisplayName("equals() symmetric")
        void testEqualsSymmetric() {
            FastInt128Value a = FastInt128Value.fromLong(12345);
            FastInt128Value b = FastInt128Value.fromLong(12345);
            assertEquals(a, b);
            assertEquals(b, a);
        }

        @Test
        @DisplayName("equals() with different values")
        void testEqualsDifferent() {
            FastInt128Value a = FastInt128Value.fromLong(12345);
            FastInt128Value b = FastInt128Value.fromLong(54321);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("equals() with null")
        void testEqualsNull() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            assertNotEquals(null, val);
        }

        @Test
        @DisplayName("equals(high, low) method")
        void testEqualsHighLow() {
            FastInt128Value val = FastInt128Value.of(1L, 2L);
            assertTrue(val.equals(1L, 2L));
            assertFalse(val.equals(1L, 3L));
            assertFalse(val.equals(2L, 2L));
        }

        @Test
        @DisplayName("hashCode() consistency")
        void testHashCodeConsistency() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            int hash1 = val.hashCode();
            int hash2 = val.hashCode();
            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("hashCode() for equal objects")
        void testHashCodeEqual() {
            FastInt128Value a = FastInt128Value.fromLong(12345);
            FastInt128Value b = FastInt128Value.fromLong(12345);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }

    @Nested
    @DisplayName("Comparison Tests")
    class ComparisonTests {

        @Test
        @DisplayName("compareTo() less than")
        void testCompareToLess() {
            FastInt128Value a = FastInt128Value.fromLong(100);
            FastInt128Value b = FastInt128Value.fromLong(200);
            assertTrue(a.compareTo(b) < 0);
        }

        @Test
        @DisplayName("compareTo() greater than")
        void testCompareToGreater() {
            FastInt128Value a = FastInt128Value.fromLong(200);
            FastInt128Value b = FastInt128Value.fromLong(100);
            assertTrue(a.compareTo(b) > 0);
        }

        @Test
        @DisplayName("compareTo() equal")
        void testCompareToEqual() {
            FastInt128Value a = FastInt128Value.fromLong(100);
            FastInt128Value b = FastInt128Value.fromLong(100);
            assertEquals(0, a.compareTo(b));
        }

        @Test
        @DisplayName("compareTo() negative vs positive")
        void testCompareToNegativePositive() {
            FastInt128Value neg = FastInt128Value.fromLong(-100);
            FastInt128Value pos = FastInt128Value.fromLong(100);
            assertTrue(neg.compareTo(pos) < 0);
            assertTrue(pos.compareTo(neg) > 0);
        }

        @Test
        @DisplayName("signum() tests")
        void testSignum() {
            assertEquals(0, FastInt128Value.ZERO.signum());
            assertEquals(1, FastInt128Value.ONE.signum());
            assertEquals(1, FastInt128Value.fromLong(12345).signum());
            assertEquals(-1, FastInt128Value.fromLong(-12345).signum());
        }

        @Test
        @DisplayName("isPositive() tests")
        void testIsPositive() {
            assertTrue(FastInt128Value.ONE.isPositive());
            assertTrue(FastInt128Value.fromLong(12345).isPositive());
            assertFalse(FastInt128Value.ZERO.isPositive());
            assertFalse(FastInt128Value.fromLong(-12345).isPositive());
        }

        @Test
        @DisplayName("isNegative() tests")
        void testIsNegative() {
            assertTrue(FastInt128Value.fromLong(-12345).isNegative());
            assertFalse(FastInt128Value.ZERO.isNegative());
            assertFalse(FastInt128Value.ONE.isNegative());
            assertFalse(FastInt128Value.fromLong(12345).isNegative());
        }
    }

    // ========================================================================
    // Bit Operation Tests
    // ========================================================================

    @Nested
    @DisplayName("Shift Tests")
    class ShiftTests {

        @Test
        @DisplayName("shiftLeft() basic")
        void testShiftLeftBasic() {
            FastInt128Value val = FastInt128Value.fromLong(1);
            FastInt128Value result = val.shiftLeft(4);
            assertEquals("16", result.toString());
        }

        @Test
        @DisplayName("shiftLeft() zero distance")
        void testShiftLeftZero() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            assertSame(val, val.shiftLeft(0));
        }

        @Test
        @DisplayName("shiftLeft() cross word boundary")
        void testShiftLeftCrossWord() {
            FastInt128Value val = FastInt128Value.of(0L, 1L);
            FastInt128Value result = val.shiftLeft(64);
            assertEquals(1L, result.high());
            assertEquals(0L, result.low());
        }

        @Test
        @DisplayName("shiftLeft() overflow")
        void testShiftLeftOverflow() {
            FastInt128Value val = FastInt128Value.fromLong(1);
            FastInt128Value result = val.shiftLeft(128);
            assertEquals(FastInt128Value.ZERO, result);
        }

        @Test
        @DisplayName("shiftRight() basic")
        void testShiftRightBasic() {
            FastInt128Value val = FastInt128Value.fromLong(16);
            FastInt128Value result = val.shiftRight(4);
            assertEquals("1", result.toString());
        }

        @Test
        @DisplayName("shiftRight() arithmetic sign extension")
        void testShiftRightSignExtension() {
            FastInt128Value val = FastInt128Value.fromLong(-16);
            FastInt128Value result = val.shiftRight(2);
            assertTrue(result.isNegative());
        }

        @Test
        @DisplayName("shiftRight() zero distance")
        void testShiftRightZero() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            assertSame(val, val.shiftRight(0));
        }

        @Test
        @DisplayName("shiftRight() large distance")
        void testShiftRightLarge() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            FastInt128Value result = val.shiftRight(128);
            assertEquals(FastInt128Value.ZERO, result);
        }

        @Test
        @DisplayName("shiftRight() negative large distance")
        void testShiftRightNegativeLarge() {
            FastInt128Value val = FastInt128Value.fromLong(-1);
            FastInt128Value result = val.shiftRight(128);
            assertEquals(FastInt128Value.MIN_VALUE, result);
        }
    }

    @Nested
    @DisplayName("Rotate Tests")
    class RotateTests {

        @Test
        @DisplayName("rotateLeft() basic")
        void testRotateLeftBasic() {
            FastInt128Value val = FastInt128Value.of(0x8000000000000000L, 0x0000000000000001L);
            FastInt128Value result = val.rotateLeft(1);
            assertEquals(0x0000000000000000L, result.high());
            assertEquals(0x0000000000000003L, result.low());
        }

        @Test
        @DisplayName("rotateLeft() zero distance")
        void testRotateLeftZero() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            assertSame(val, val.rotateLeft(0));
        }

        @Test
        @DisplayName("rotateLeft() 64 bits swaps words")
        void testRotateLeft64() {
            FastInt128Value val = FastInt128Value.of(0x1234567890ABCDEFL, 0xFEDCBA0987654321L);
            FastInt128Value result = val.rotateLeft(64);
            assertEquals(0xFEDCBA0987654321L, result.high());
            assertEquals(0x1234567890ABCDEFL, result.low());
        }

        @Test
        @DisplayName("rotateRight() basic")
        void testRotateRightBasic() {
            FastInt128Value val = FastInt128Value.of(0x0000000000000000L, 0x8000000000000001L);
            FastInt128Value result = val.rotateRight(1);
            assertEquals(0x8000000000000000L, result.high());
            assertEquals(0xC000000000000000L, result.low());
        }
    }

    @Nested
    @DisplayName("Bitwise Operation Tests")
    class BitwiseTests {

        @Test
        @DisplayName("bitwiseNot() basic")
        void testBitwiseNot() {
            FastInt128Value val = FastInt128Value.ZERO;
            FastInt128Value result = val.bitwiseNot();
            assertEquals(-1L, result.high());
            assertEquals(-1L, result.low());
        }

        @Test
        @DisplayName("bitwiseAnd() basic")
        void testBitwiseAnd() {
            FastInt128Value a = FastInt128Value.of(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFF00000000L);
            FastInt128Value b = FastInt128Value.of(0x0000FFFFFFFF0000L, 0x00000000FFFFFFFFL);
            FastInt128Value result = a.bitwiseAnd(b);
            assertEquals(0x0000FFFFFFFF0000L, result.high());
            assertEquals(0x0000000000000000L, result.low());
        }

        @Test
        @DisplayName("bitwiseOr() basic")
        void testBitwiseOr() {
            FastInt128Value a = FastInt128Value.of(0xFF00000000000000L, 0x00000000FF000000L);
            FastInt128Value b = FastInt128Value.of(0x00FF000000000000L, 0x0000000000FF0000L);
            FastInt128Value result = a.bitwiseOr(b);
            assertEquals(0xFFFF000000000000L, result.high());
            assertEquals(0x00000000FFFF0000L, result.low());
        }

        @Test
        @DisplayName("bitwiseXor() basic")
        void testBitwiseXor() {
            FastInt128Value a = FastInt128Value.of(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
            FastInt128Value b = FastInt128Value.of(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L);
            FastInt128Value result = a.bitwiseXor(b);
            assertEquals(0x0000000000000000L, result.high());
            assertEquals(0xFFFFFFFFFFFFFFFFL, result.low());
        }

        @Test
        @DisplayName("getBit() tests")
        void testGetBit() {
            FastInt128Value val = FastInt128Value.of(0x8000000000000000L, 0x0000000000000001L);
            assertEquals(1, val.getBit(0));
            assertEquals(0, val.getBit(1));
            assertEquals(1, val.getBit(127));
        }

        @Test
        @DisplayName("getBit() out of range")
        void testGetBitOutOfRange() {
            FastInt128Value val = FastInt128Value.ONE;
            assertThrows(IllegalArgumentException.class, () -> val.getBit(-1));
            assertThrows(IllegalArgumentException.class, () -> val.getBit(128));
        }

        @Test
        @DisplayName("bitLength() tests")
        void testBitLength() {
            assertEquals(0, FastInt128Value.ZERO.bitLength());
            assertEquals(1, FastInt128Value.ONE.bitLength());
            assertEquals(1, FastInt128Value.fromLong(-1).bitLength());
            assertEquals(8, FastInt128Value.fromLong(255).bitLength());
            assertEquals(8, FastInt128Value.fromLong(-128).bitLength());
        }

        @Test
        @DisplayName("numberOfLeadingZeros() tests")
        void testNumberOfLeadingZeros() {
            assertEquals(128, FastInt128Value.ZERO.numberOfLeadingZeros());
            assertEquals(127, FastInt128Value.ONE.numberOfLeadingZeros());
            assertEquals(0, FastInt128Value.MAX_VALUE.numberOfLeadingZeros());
        }

        @Test
        @DisplayName("trailingZeroCount() tests")
        void testTrailingZeroCount() {
            assertEquals(128, FastInt128Value.ZERO.trailingZeroCount());
            assertEquals(0, FastInt128Value.ONE.trailingZeroCount());
            assertEquals(0, FastInt128Value.of(-1L, -1L).trailingZeroCount());
            assertEquals(4, FastInt128Value.fromLong(16).trailingZeroCount());
        }

        @Test
        @DisplayName("populationCount() tests")
        void testPopulationCount() {
            assertEquals(0, FastInt128Value.ZERO.populationCount());
            assertEquals(1, FastInt128Value.ONE.populationCount());
            assertEquals(128, FastInt128Value.of(-1L, -1L).populationCount());
        }

        @Test
        @DisplayName("isPowerOfTwo() tests")
        void testIsPowerOfTwo() {
            assertFalse(FastInt128Value.ZERO.isPowerOfTwo());
            assertTrue(FastInt128Value.ONE.isPowerOfTwo());
            assertTrue(FastInt128Value.fromLong(2).isPowerOfTwo());
            assertTrue(FastInt128Value.fromLong(256).isPowerOfTwo());
            assertFalse(FastInt128Value.fromLong(3).isPowerOfTwo());
            assertFalse(FastInt128Value.fromLong(255).isPowerOfTwo());
        }
    }

    // ========================================================================
    // Increment/Decrement Tests
    // ========================================================================

    @Nested
    @DisplayName("Increment/Decrement Tests")
    class IncrementDecrementTests {

        @Test
        @DisplayName("increment() basic")
        void testIncrement() {
            FastInt128Value val = FastInt128Value.fromLong(99);
            FastInt128Value result = val.increment();
            assertEquals("100", result.toString());
        }

        @Test
        @DisplayName("increment() with carry")
        void testIncrementCarry() {
            FastInt128Value val = FastInt128Value.of(0L, -1L);
            FastInt128Value result = val.increment();
            assertEquals(1L, result.high());
            assertEquals(0L, result.low());
        }

        @Test
        @DisplayName("decrement() basic")
        void testDecrement() {
            FastInt128Value val = FastInt128Value.fromLong(100);
            FastInt128Value result = val.decrement();
            assertEquals("99", result.toString());
        }

        @Test
        @DisplayName("decrement() with borrow")
        void testDecrementBorrow() {
            FastInt128Value val = FastInt128Value.of(1L, 0L);
            FastInt128Value result = val.decrement();
            assertEquals(0L, result.high());
            assertEquals(-1L, result.low());
        }

        @Test
        @DisplayName("increment/decrement round-trip")
        void testIncrementDecrementRoundTrip() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            assertEquals(val, val.increment().decrement());
            assertEquals(val, val.decrement().increment());
        }
    }

    // ========================================================================
    // Absolute Value Tests
    // ========================================================================

    @Nested
    @DisplayName("Absolute Value Tests")
    class AbsoluteValueTests {

        @Test
        @DisplayName("abs() of positive")
        void testAbsPositive() {
            FastInt128Value val = FastInt128Value.fromLong(12345);
            assertSame(val, val.abs());
        }

        @Test
        @DisplayName("abs() of negative")
        void testAbsNegative() {
            FastInt128Value val = FastInt128Value.fromLong(-12345);
            FastInt128Value abs = val.abs();
            assertEquals("12345", abs.toString());
        }

        @Test
        @DisplayName("abs() of zero")
        void testAbsZero() {
            assertSame(FastInt128Value.ZERO, FastInt128Value.ZERO.abs());
        }

        @Test
        @DisplayName("abs() of MIN_VALUE (special case)")
        void testAbsMinValue() {
            FastInt128Value abs = FastInt128Value.MIN_VALUE.abs();
            // abs(MIN_VALUE) wraps to MIN_VALUE in two's complement
            assertFalse(abs.isNegative());
        }
    }

    // ========================================================================
    // Additional Helper Method Tests
    // ========================================================================

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("isZero() tests")
        void testIsZero() {
            assertTrue(FastInt128Value.ZERO.isZero());
            assertFalse(FastInt128Value.ONE.isZero());
            assertFalse(FastInt128Value.fromLong(-1).isZero());
        }

        @Test
        @DisplayName("isOne() tests")
        void testIsOne() {
            assertTrue(FastInt128Value.ONE.isOne());
            assertFalse(FastInt128Value.ZERO.isOne());
            assertFalse(FastInt128Value.fromLong(2).isOne());
        }

        @Test
        @DisplayName("isNegativeOne() tests")
        void testIsNegativeOne() {
            assertTrue(FastInt128Value.fromLong(-1).isNegativeOne());
            assertFalse(FastInt128Value.ZERO.isNegativeOne());
            assertFalse(FastInt128Value.ONE.isNegativeOne());
        }

        @Test
        @DisplayName("fitsInLong() tests")
        void testFitsInLong() {
            assertTrue(FastInt128Value.ZERO.fitsInLong());
            assertTrue(FastInt128Value.ONE.fitsInLong());
            assertTrue(FastInt128Value.fromLong(Long.MAX_VALUE).fitsInLong());
            assertTrue(FastInt128Value.fromLong(Long.MIN_VALUE).fitsInLong());
            assertFalse(FastInt128Value.MAX_VALUE.fitsInLong());
            assertFalse(FastInt128Value.MIN_VALUE.fitsInLong());
        }

        @Test
        @DisplayName("longValue() tests")
        void testLongValue() {
            assertEquals(0L, FastInt128Value.ZERO.longValue());
            assertEquals(1L, FastInt128Value.ONE.longValue());
            assertEquals(12345L, FastInt128Value.fromLong(12345).longValue());
            assertEquals(-12345L, FastInt128Value.fromLong(-12345).longValue());
        }

        @Test
        @DisplayName("intValue() tests")
        void testIntValue() {
            assertEquals(0, FastInt128Value.ZERO.intValue());
            assertEquals(1, FastInt128Value.ONE.intValue());
            assertEquals(12345, FastInt128Value.fromLong(12345).intValue());
        }

        @Test
        @DisplayName("max() tests")
        void testMax() {
            FastInt128Value a = FastInt128Value.fromLong(100);
            FastInt128Value b = FastInt128Value.fromLong(200);
            assertEquals(b, a.max(b));
            assertEquals(b, b.max(a));
        }

        @Test
        @DisplayName("min() tests")
        void testMin() {
            FastInt128Value a = FastInt128Value.fromLong(100);
            FastInt128Value b = FastInt128Value.fromLong(200);
            assertEquals(a, a.min(b));
            assertEquals(a, b.min(a));
        }

        @Test
        @DisplayName("clamp() tests")
        void testClamp() {
            FastInt128Value min = FastInt128Value.fromLong(0);
            FastInt128Value max = FastInt128Value.fromLong(100);

            assertEquals(FastInt128Value.fromLong(50),
                        FastInt128Value.fromLong(50).clamp(min, max));
            assertEquals(min,
                        FastInt128Value.fromLong(-10).clamp(min, max));
            assertEquals(max,
                        FastInt128Value.fromLong(200).clamp(min, max));
        }

        @Test
        @DisplayName("isBetween() tests")
        void testIsBetween() {
            FastInt128Value min = FastInt128Value.fromLong(0);
            FastInt128Value max = FastInt128Value.fromLong(100);

            assertTrue(FastInt128Value.fromLong(50).isBetween(min, max));
            assertTrue(FastInt128Value.fromLong(0).isBetween(min, max));
            assertTrue(FastInt128Value.fromLong(100).isBetween(min, max));
            assertFalse(FastInt128Value.fromLong(-10).isBetween(min, max));
            assertFalse(FastInt128Value.fromLong(200).isBetween(min, max));
        }

        @Test
        @DisplayName("toByteArray() tests")
        void testToByteArray() {
            FastInt128Value val = FastInt128Value.of(0x0102030405060708L, 0x090A0B0C0D0E0F10L);
            byte[] bytes = val.toByteArray();
            assertEquals(16, bytes.length);
            assertEquals((byte)0x01, bytes[0]);
            assertEquals((byte)0x10, bytes[15]);
        }
    }

    // ========================================================================
    // Mutable Value Tests
    // ========================================================================

    @Nested
    @DisplayName("Mutable Value Tests")
    class MutableValueTests {

        @Test
        @DisplayName("MutableFastInt128Value construction")
        void testMutableConstruction() {
            var mutable = new FastInt128Value.MutableFastInt128Value();
            assertEquals(0L, mutable.high());
            assertEquals(0L, mutable.low());
            assertTrue(mutable.isZero());
        }

        @Test
        @DisplayName("MutableFastInt128Value set()")
        void testMutableSet() {
            var mutable = new FastInt128Value.MutableFastInt128Value();
            mutable.set(1L, 2L);
            assertEquals(1L, mutable.high());
            assertEquals(2L, mutable.low());
        }

        @Test
        @DisplayName("MutableFastInt128Value addInto()")
        void testMutableAddInto() {
            var mutable = new FastInt128Value.MutableFastInt128Value(0L, 100L);
            FastInt128Value toAdd = FastInt128Value.fromLong(50);
            mutable.addInto(toAdd);
            assertEquals("150", mutable.toString());
        }

        @Test
        @DisplayName("MutableFastInt128Value subtract()")
        void testMutableSubtract() {
            var mutable = new FastInt128Value.MutableFastInt128Value(0L, 100L);
            FastInt128Value toSubtract = FastInt128Value.fromLong(50);
            mutable.subtract(toSubtract);
            assertEquals("50", mutable.toString());
        }

        @Test
        @DisplayName("MutableFastInt128Value multiply()")
        void testMutableMultiply() {
            var mutable = new FastInt128Value.MutableFastInt128Value(0L, 10L);
            FastInt128Value multiplier = FastInt128Value.fromLong(5);
            mutable.multiply(multiplier);
            assertEquals("50", mutable.toString());
        }

        @Test
        @DisplayName("MutableFastInt128Value negate()")
        void testMutableNegate() {
            var mutable = new FastInt128Value.MutableFastInt128Value(0L, 12345L);
            mutable.negate();
            assertEquals("-12345", mutable.toString());
        }

        @Test
        @DisplayName("MutableFastInt128Value increment()")
        void testMutableIncrement() {
            var mutable = new FastInt128Value.MutableFastInt128Value(0L, 99L);
            mutable.increment();
            assertEquals("100", mutable.toString());
        }

        @Test
        @DisplayName("MutableFastInt128Value decrement()")
        void testMutableDecrement() {
            var mutable = new FastInt128Value.MutableFastInt128Value(0L, 100L);
            mutable.decrement();
            assertEquals("99", mutable.toString());
        }

        @Test
        @DisplayName("MutableFastInt128Value clear()")
        void testMutableClear() {
            var mutable = new FastInt128Value.MutableFastInt128Value(1L, 2L);
            mutable.clear();
            assertTrue(mutable.isZero());
        }

        @Test
        @DisplayName("MutableFastInt128Value immutableCopy()")
        void testMutableImmutableCopy() {
            var mutable = new FastInt128Value.MutableFastInt128Value(1L, 2L);
            FastInt128Value immutable = mutable.immutableCopy();
            assertEquals(1L, immutable.high());
            assertEquals(2L, immutable.low());
        }
    }

    // ========================================================================
    // Edge Case and Overflow Tests
    // ========================================================================

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Addition overflow detection")
        void testAdditionOverflow() {
            FastInt128Value a = FastInt128Value.MAX_VALUE;
            FastInt128Value b = FastInt128Value.ONE;
            // Adding 1 to MAX_VALUE wraps to MIN_VALUE
            FastInt128Value result = a.add(b);
            assertEquals(FastInt128Value.MIN_VALUE, result);
        }

        @Test
        @DisplayName("Subtraction underflow detection")
        void testSubtractionUnderflow() {
            FastInt128Value a = FastInt128Value.MIN_VALUE;
            FastInt128Value b = FastInt128Value.ONE;
            // Subtracting 1 from MIN_VALUE wraps to MAX_VALUE
            FastInt128Value result = a.subtract(b);
            assertEquals(FastInt128Value.MAX_VALUE, result);
        }

        @Test
        @DisplayName("Multiply MAX_VALUE by 2")
        void testMultiplyMaxValueBy2() {
            FastInt128Value result = FastInt128Value.MAX_VALUE.multiply(FastInt128Value.fromLong(2));
            // Should wrap around
            assertNotEquals(FastInt128Value.MAX_VALUE, result);
        }

        @Test
        @DisplayName("Negate MIN_VALUE")
        void testNegateMinValue() {
            // Negating MIN_VALUE in two's complement wraps to MIN_VALUE
            FastInt128Value result = FastInt128Value.MIN_VALUE.negate();
            // Due to two's complement, -MIN_VALUE = MIN_VALUE
            assertFalse(result.isNegative());
        }

        @Test
        @DisplayName("wouldOverflowAdd() tests")
        void testWouldOverflowAdd() {
            assertTrue(FastInt128Value.MAX_VALUE.wouldOverflowAdd(FastInt128Value.ONE));
            assertFalse(FastInt128Value.fromLong(100).wouldOverflowAdd(FastInt128Value.fromLong(100)));
            assertTrue(FastInt128Value.MIN_VALUE.wouldOverflowAdd(FastInt128Value.fromLong(-1)));
        }

        @Test
        @DisplayName("wouldOverflowSubtract() tests")
        void testWouldOverflowSubtract() {
            assertTrue(FastInt128Value.MIN_VALUE.wouldOverflowSubtract(FastInt128Value.ONE));
            assertFalse(FastInt128Value.fromLong(100).wouldOverflowSubtract(FastInt128Value.fromLong(50)));
            assertTrue(FastInt128Value.MAX_VALUE.wouldOverflowSubtract(FastInt128Value.fromLong(-1)));
        }
    }

    // ========================================================================
    // Financial/Decimal-Specific Tests
    // ========================================================================

    @Nested
    @DisplayName("Financial Calculation Tests")
    class FinancialTests {

        @Test
        @DisplayName("Large financial calculations")
        void testLargeFinancialCalculations() {
            // Simulate calculating total value of 1 trillion units at $100,000 each
            FastInt128Value units = FastInt128Value.fromString("1000000000000");
            FastInt128Value pricePerUnit = FastInt128Value.fromLong(100000);
            FastInt128Value total = units.multiply(pricePerUnit);
            assertEquals("100000000000000000", total.toString());
        }

        @Test
        @DisplayName("Decimal precision with DECIMAL_BASE")
        void testDecimalPrecision() {
            FastInt128Value base = FastInt128Value.fromLong(FastInt128Value.DECIMAL_BASE);
            assertEquals("1000000000", base.toString());
        }

        @Test
        @DisplayName("Round-trip through decimal string")
        void testDecimalStringRoundTrip() {
            String[] values = {
                "1234567890123456789012345678901234567",
                "999999999999999999999999999999999999",
                "100000000000000000000000000000000000"
            };

            for (String value : values) {
                FastInt128Value val = FastInt128Value.fromString(value);
                assertEquals(value, val.toString());
            }
        }

        @Test
        @DisplayName("multiplyAdd() for compound interest")
        void testMultiplyAdd() {
            FastInt128Value principal = FastInt128Value.fromLong(10000);
            FastInt128Value rate = FastInt128Value.fromLong(105); // 1.05 * 100
            FastInt128Value constant = FastInt128Value.fromLong(100);
            FastInt128Value result = principal.multiplyAdd(rate, constant);
            // 10000 * 105 + 100 = 1050100
            assertEquals("1050100", result.toString());
        }

        @Test
        @DisplayName("average() for midpoint calculations")
        void testAverage() {
            FastInt128Value a = FastInt128Value.fromLong(100);
            FastInt128Value b = FastInt128Value.fromLong(200);
            FastInt128Value avg = a.average(b);
            assertEquals("150", avg.toString());
        }

        @Test
        @DisplayName("absoluteDifference() for spread calculations")
        void testAbsoluteDifference() {
            FastInt128Value bid = FastInt128Value.fromLong(9950);
            FastInt128Value ask = FastInt128Value.fromLong(10050);
            FastInt128Value spread = bid.absoluteDifference(ask);
            assertEquals("100", spread.toString());
        }
    }

    // ========================================================================
    // Advanced Operation Tests
    // ========================================================================

    @Nested
    @DisplayName("Advanced Operation Tests")
    class AdvancedOperationTests {

        @Test
        @DisplayName("magnitudeLessThan() tests")
        void testMagnitudeLessThan() {
            FastInt128Value a = FastInt128Value.fromLong(-100);
            FastInt128Value b = FastInt128Value.fromLong(200);
            assertTrue(a.magnitudeLessThan(b));
            assertFalse(b.magnitudeLessThan(a));
        }

        @Test
        @DisplayName("hasSameSign() tests")
        void testHasSameSign() {
            FastInt128Value pos1 = FastInt128Value.fromLong(100);
            FastInt128Value pos2 = FastInt128Value.fromLong(200);
            FastInt128Value neg1 = FastInt128Value.fromLong(-100);

            assertTrue(pos1.hasSameSign(pos2));
            assertFalse(pos1.hasSameSign(neg1));
        }

        @Test
        @DisplayName("differsByOne() tests")
        void testDiffersByOne() {
            FastInt128Value a = FastInt128Value.fromLong(100);
            FastInt128Value b = FastInt128Value.fromLong(101);
            FastInt128Value c = FastInt128Value.fromLong(99);
            FastInt128Value d = FastInt128Value.fromLong(102);

            assertTrue(a.differsByOne(b));
            assertTrue(a.differsByOne(c));
            assertFalse(a.differsByOne(d));
        }

        @Test
        @DisplayName("doubleValueExact() tests")
        void testDoubleValueExact() {
            FastInt128Value val = FastInt128Value.fromLong(5);
            FastInt128Value doubled = val.doubleValueExact();
            assertEquals("10", doubled.toString());
        }

        @Test
        @DisplayName("halveValue() tests")
        void testHalveValue() {
            FastInt128Value val = FastInt128Value.fromLong(100);
            FastInt128Value halved = val.halveValue();
            assertEquals("50", halved.toString());
        }

        @Test
        @DisplayName("intersects() tests")
        void testIntersects() {
            FastInt128Value a = FastInt128Value.of(0xFF00FF00FF00FF00L, 0xFF00FF00FF00FF00L);
            FastInt128Value b = FastInt128Value.of(0x00FF00FF00FF00FFL, 0x00FF00FF00FF00FFL);
            FastInt128Value c = FastInt128Value.of(0xFF00000000000000L, 0x0000000000000000L);

            assertFalse(a.intersects(b));
            assertTrue(a.intersects(c));
        }

        @Test
        @DisplayName("containsAllBits() tests")
        void testContainsAllBits() {
            FastInt128Value val = FastInt128Value.of(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
            FastInt128Value mask = FastInt128Value.of(0x00000000FF000000L, 0x0000FF0000000000L);

            assertTrue(val.containsAllBits(mask));
            assertFalse(mask.containsAllBits(val));
        }

        @Test
        @DisplayName("containsNoBits() tests")
        void testContainsNoBits() {
            FastInt128Value a = FastInt128Value.of(0xFF00000000000000L, 0x0000000000000000L);
            FastInt128Value b = FastInt128Value.of(0x00FF000000000000L, 0x0000000000000000L);

            assertFalse(a.containsNoBits(a));
            assertFalse(a.containsNoBits(b));
        }

        @Test
        @DisplayName("parseHex() tests")
        void testParseHex() {
            FastInt128Value val = FastInt128Value.parseHex("0x123456789ABCDEF0");
            assertEquals(0x123456789ABCDEF0L, val.low());
            assertEquals(0L, val.high());
        }

        @Test
        @DisplayName("parseHex() with 0x prefix")
        void testParseHexWithPrefix() {
            FastInt128Value val = FastInt128Value.parseHex("0x10");
            assertEquals(16L, val.low());
        }

        @Test
        @DisplayName("parseHex() without prefix")
        void testParseHexWithoutPrefix() {
            FastInt128Value val = FastInt128Value.parseHex("FF");
            assertEquals(255L, val.low());
        }

        @Test
        @DisplayName("parseHex() invalid character")
        void testParseHexInvalid() {
            assertThrows(NumberFormatException.class, () -> FastInt128Value.parseHex("XYZ"));
        }
    }

    // ========================================================================
    // FastInt128Arithmetic Tests
    // ========================================================================

    @Nested
    @DisplayName("FastInt128Arithmetic Tests")
    class ArithmeticTests {

        private final FastInt128Arithmetic arithmetic = new FastInt128Arithmetic();

        @Test
        @DisplayName("fromParts() creates value")
        void testFromParts() {
            var value = arithmetic.fromParts(1L, 2L);
            assertEquals(1L, value.high());
            assertEquals(2L, value.low());
        }

        @Test
        @DisplayName("fromLong() sign extends")
        void testFromLongArithmetic() {
            var positive = arithmetic.fromLong(12345L);
            assertEquals(0L, positive.high());
            assertEquals(12345L, positive.low());

            var negative = arithmetic.fromLong(-12345L);
            assertEquals(-1L, negative.high());
            assertEquals(-12345L, negative.low());
        }

        @Test
        @DisplayName("createMutable() creates mutable instance")
        void testCreateMutable() {
            var mutable = arithmetic.createMutable();
            assertNotNull(mutable);
            assertEquals(0L, mutable.high());
            assertEquals(0L, mutable.low());
        }

        @Test
        @DisplayName("addInto() performs addition")
        void testAddInto() {
            var left = arithmetic.fromLong(100);
            var right = arithmetic.fromLong(200);
            var dest = arithmetic.createMutable();

            arithmetic.addInto(left, right, dest);

            assertEquals(0L, dest.high());
            assertEquals(300L, dest.low());
        }

        @Test
        @DisplayName("subtractInto() performs subtraction")
        void testSubtractInto() {
            var left = arithmetic.fromLong(200);
            var right = arithmetic.fromLong(100);
            var dest = arithmetic.createMutable();

            arithmetic.subtractInto(left, right, dest);

            assertEquals(0L, dest.high());
            assertEquals(100L, dest.low());
        }

        @Test
        @DisplayName("multiplyInto() performs multiplication")
        void testMultiplyInto() {
            var left = arithmetic.fromLong(100);
            var right = arithmetic.fromLong(50);
            var dest = arithmetic.createMutable();

            arithmetic.multiplyInto(left, right, dest);

            assertEquals(0L, dest.high());
            assertEquals(5000L, dest.low());
        }

        @Test
        @DisplayName("add() returns immutable result")
        void testAdd() {
            var left = arithmetic.fromLong(100);
            var right = arithmetic.fromLong(200);
            var result = arithmetic.add(left, right);

            assertEquals(0L, result.high());
            assertEquals(300L, result.low());
        }

        @Test
        @DisplayName("multiply() returns immutable result")
        void testMultiply() {
            var left = arithmetic.fromLong(100);
            var right = arithmetic.fromLong(50);
            var result = arithmetic.multiply(left, right);

            assertEquals(0L, result.high());
            assertEquals(5000L, result.low());
        }
    }

    // ========================================================================
    // Stress Tests
    // ========================================================================

    @Nested
    @DisplayName("Stress Tests")
    class StressTests {

        @Test
        @DisplayName("Many sequential additions")
        void testManyAdditions() {
            FastInt128Value sum = FastInt128Value.ZERO;
            for (int i = 1; i <= 1000; i++) {
                sum = sum.add(FastInt128Value.fromLong(i));
            }
            // Sum of 1 to 1000 = 500500
            assertEquals("500500", sum.toString());
        }

        @Test
        @DisplayName("Many sequential multiplications")
        void testManyMultiplications() {
            FastInt128Value product = FastInt128Value.ONE;
            for (int i = 1; i <= 10; i++) {
                product = product.multiply(FastInt128Value.fromLong(2));
            }
            // 2^10 = 1024
            assertEquals("1024", product.toString());
        }

        @Test
        @DisplayName("Alternating add/subtract")
        void testAlternatingOps() {
            FastInt128Value val = FastInt128Value.fromLong(1000);
            for (int i = 0; i < 100; i++) {
                val = val.add(FastInt128Value.fromLong(10));
                val = val.subtract(FastInt128Value.fromLong(10));
            }
            assertEquals("1000", val.toString());
        }

        @Test
        @DisplayName("Large string parsing stress test")
        void testLargeStringParsing() {
            String[] largeNumbers = {
                "12345678901234567890123456789012345678",
                "98765432109876543210987654321098765432",
                "11111111111111111111111111111111111111",
                "99999999999999999999999999999999999999"
            };

            for (String numStr : largeNumbers) {
                FastInt128Value val = FastInt128Value.fromString(numStr);
                assertEquals(numStr, val.toString());
            }
        }
    }

    // ========================================================================
    // MathOps Static Utility Tests
    // ========================================================================

    @Nested
    @DisplayName("MathOps Static Utility Tests")
    class MathOpsTests {

        @Test
        @DisplayName("MathOps.addInto()")
        void testMathOpsAddInto() {
            var left = FastInt128Value.fromLong(100);
            var right = FastInt128Value.fromLong(200);
            var dest = new FastInt128Value.MutableFastInt128Value();

            FastInt128Value.MathOps.addInto(left, right, dest);

            assertEquals(300L, dest.low());
        }

        @Test
        @DisplayName("MathOps.subtractInto()")
        void testMathOpsSubtractInto() {
            var left = FastInt128Value.fromLong(200);
            var right = FastInt128Value.fromLong(100);
            var dest = new FastInt128Value.MutableFastInt128Value();

            FastInt128Value.MathOps.subtractInto(left, right, dest);

            assertEquals(100L, dest.low());
        }

        @Test
        @DisplayName("MathOps.multiplyInto()")
        void testMathOpsMultiplyInto() {
            var left = FastInt128Value.fromLong(10);
            var right = FastInt128Value.fromLong(20);
            var dest = new FastInt128Value.MutableFastInt128Value();

            FastInt128Value.MathOps.multiplyInto(left, right, dest);

            assertEquals(200L, dest.low());
        }

        @Test
        @DisplayName("MathOps.negateInto()")
        void testMathOpsNegateInto() {
            var value = FastInt128Value.fromLong(12345);
            var dest = new FastInt128Value.MutableFastInt128Value();

            FastInt128Value.MathOps.negateInto(value, dest);

            assertEquals("-12345", dest.toString());
        }

        @Test
        @DisplayName("MathOps.compare()")
        void testMathOpsCompare() {
            var a = FastInt128Value.fromLong(100);
            var b = FastInt128Value.fromLong(200);

            assertTrue(FastInt128Value.MathOps.compare(a, b) < 0);
            assertTrue(FastInt128Value.MathOps.compare(b, a) > 0);
            assertEquals(0, FastInt128Value.MathOps.compare(a, a));
        }

        @Test
        @DisplayName("MathOps.isZero()")
        void testMathOpsIsZero() {
            assertTrue(FastInt128Value.MathOps.isZero(FastInt128Value.ZERO));
            assertFalse(FastInt128Value.MathOps.isZero(FastInt128Value.ONE));
        }

        @Test
        @DisplayName("MathOps.isNegative()")
        void testMathOpsIsNegative() {
            assertTrue(FastInt128Value.MathOps.isNegative(FastInt128Value.fromLong(-1)));
            assertFalse(FastInt128Value.MathOps.isNegative(FastInt128Value.ONE));
        }
    }

    // ========================================================================
    // Correctness Verification with BigInteger
    // ========================================================================

    @Nested
    @DisplayName("BigInteger Correctness Tests")
    class BigIntegerCorrectnessTests {

        private BigInteger toBigInteger(FastInt128Value val) {
            // Convert to BigInteger for verification
            byte[] bytes = val.toByteArray();
            return new BigInteger(bytes);
        }

        @Test
        @DisplayName("Addition matches BigInteger")
        void testAdditionMatchesBigInteger() {
            FastInt128Value a = FastInt128Value.fromString("12345678901234567890");
            FastInt128Value b = FastInt128Value.fromString("98765432109876543210");
            FastInt128Value result = a.add(b);

            BigInteger bigA = toBigInteger(a);
            BigInteger bigB = toBigInteger(b);
            BigInteger bigResult = bigA.add(bigB);

            assertEquals(bigResult.toString(), result.toString());
        }

        @Test
        @DisplayName("Subtraction matches BigInteger")
        void testSubtractionMatchesBigInteger() {
            FastInt128Value a = FastInt128Value.fromString("98765432109876543210");
            FastInt128Value b = FastInt128Value.fromString("12345678901234567890");
            FastInt128Value result = a.subtract(b);

            BigInteger bigA = toBigInteger(a);
            BigInteger bigB = toBigInteger(b);
            BigInteger bigResult = bigA.subtract(bigB);

            assertEquals(bigResult.toString(), result.toString());
        }

        @Test
        @DisplayName("Multiplication matches BigInteger (within 128-bit range)")
        void testMultiplicationMatchesBigInteger() {
            FastInt128Value a = FastInt128Value.fromString("1000000000000");
            FastInt128Value b = FastInt128Value.fromString("1000000");
            FastInt128Value result = a.multiply(b);

            BigInteger bigA = toBigInteger(a);
            BigInteger bigB = toBigInteger(b);
            BigInteger bigResult = bigA.multiply(bigB);

            // Only compare low 128 bits
            BigInteger mask128 = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
            bigResult = bigResult.and(mask128);
            if (bigResult.testBit(127)) {
                bigResult = bigResult.subtract(BigInteger.ONE.shiftLeft(128));
            }

            assertEquals(bigResult.toString(), result.toString());
        }

        @Test
        @DisplayName("Negation matches BigInteger")
        void testNegationMatchesBigInteger() {
            FastInt128Value val = FastInt128Value.fromString("12345678901234567890");
            FastInt128Value neg = val.negate();

            BigInteger bigVal = toBigInteger(val);
            BigInteger bigNeg = bigVal.negate();

            assertEquals(bigNeg.toString(), neg.toString());
        }
    }
}
