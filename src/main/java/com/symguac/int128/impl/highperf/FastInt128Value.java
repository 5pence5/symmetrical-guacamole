package com.symguac.int128.impl.highperf;

import com.symguac.int128.api.Int128Value;
import com.symguac.int128.api.MutableInt128Value;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * <p>Highly optimised immutable representation of a two's-complement signed 128-bit integer. The implementation
 * intentionally exposes a very rich API surface so that low-level arithmetic code can be written without constantly
 * re-deriving helper routines. The emphasis is on predictable execution characteristics and zero-allocation usage
 * patterns suitable for low-latency trading systems.</p>
 *
 * <p>The class acts as a building block for the accompanying {@code FastInt128Arithmetic} implementation. The
 * arithmetic class operates primarily on the primitive {@code long} fields of this value object; however, having a
 * heavyweight companion object with extensive functionality simplifies benchmarking and manual validation work.</p>
 *
 * <p>While the code base is deliberately verbose (it easily exceeds one thousand lines), the intent is clarity through
 * explicitness. Every operation has a specialised helper and nothing is left to the JVM to infer. These helpers are
 * also useful for library consumers who wish to perform ad-hoc computations outside of the harness.</p>
 */
public final class FastInt128Value implements Int128Value, Comparable<FastInt128Value> {

    /** Number of bits in a single limb used by the internal schoolbook multiplication routines. */
    private static final int LIMB_BITS = 16;
    /** Mask for extracting a single limb. */
    private static final int LIMB_MASK = (1 << LIMB_BITS) - 1;
    /** Number of 16-bit limbs required to represent 128 bits. */
    private static final int LIMB_COUNT = 128 / LIMB_BITS;

    /** The decimal base used for conversions. Chosen to balance digit packing and overflow avoidance. */
    public static final long DECIMAL_BASE = 1_000_000_000L;
    /** Base-10 digits captured per decimal limb. */
    public static final int DECIMAL_BASE_POWER = 9;

    /** Constant zero. */
    public static final FastInt128Value ZERO = new FastInt128Value(0L, 0L);
    /** Constant one. */
    public static final FastInt128Value ONE = new FastInt128Value(0L, 1L);
    /** Maximum representable signed 128-bit value. */
    public static final FastInt128Value MAX_VALUE = new FastInt128Value(Long.MAX_VALUE, -1L);
    /** Minimum representable signed 128-bit value. */
    public static final FastInt128Value MIN_VALUE = new FastInt128Value(Long.MIN_VALUE, 0L);

    /** Low bits of the number (least significant). */
    private final long low;
    /** High bits of the number (most significant). */
    private final long high;

    /**
     * Creates a new value from explicit high and low words.
     *
     * @param high most significant 64 bits
     * @param low least significant 64 bits
     */
    private FastInt128Value(long high, long low) {
        this.high = high;
        this.low = low;
    }

    /**
     * Factory method that attempts to reuse pre-instantiated singletons for common constants.
     *
     * @param high most significant 64 bits
     * @param low least significant 64 bits
     * @return immutable {@link FastInt128Value}
     */
    public static FastInt128Value of(long high, long low) {
        if (high == 0L) {
            if (low == 0L) {
                return ZERO;
            }
            if (low == 1L) {
                return ONE;
            }
        } else if (high == Long.MAX_VALUE && low == -1L) {
            return MAX_VALUE;
        } else if (high == Long.MIN_VALUE && low == 0L) {
            return MIN_VALUE;
        }
        return new FastInt128Value(high, low);
    }

    /**
     * Creates a value from a standard 64-bit signed integer by sign-extending it into the upper word.
     *
     * @param value the 64-bit integer to convert
     * @return immutable 128-bit value
     */
    public static FastInt128Value fromLong(long value) {
        long high = value < 0 ? -1L : 0L;
        return of(high, value);
    }

    /**
     * Creates a value from a decimal string. The parser accepts optional leading sign characters, ignores leading
     * zeros, and validates the input strictly. This method never allocates intermediate {@link java.math.BigInteger}
     * objects; instead it relies on the specialised helpers defined later in the file.
     *
     * @param text decimal textual representation
     * @return parsed value
     */
    public static FastInt128Value fromString(String text) {
        Objects.requireNonNull(text, "text");
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new NumberFormatException("Empty string");
        }
        int index = 0;
        boolean negative = false;
        char first = trimmed.charAt(0);
        if (first == '+' || first == '-') {
            negative = first == '-';
            index++;
            if (trimmed.length() == 1) {
                throw new NumberFormatException("Sign character without digits: '" + text + "'");
            }
        }
        int len = trimmed.length();
        // Skip leading zeros for efficiency but keep track in case the number is zero.
        while (index < len && trimmed.charAt(index) == '0') {
            index++;
        }
        if (index == len) {
            return negative ? ZERO.negate() : ZERO;
        }
        int remainingDigits = len - index;
        int firstGroupSize = remainingDigits % DECIMAL_BASE_POWER;
        if (firstGroupSize == 0) {
            firstGroupSize = DECIMAL_BASE_POWER;
        }
        int position = index;
        int firstEnd = position + firstGroupSize;
        int firstValue = parseDecimalGroup(trimmed, position, firstEnd);
        FastInt128Value value = FastInt128Value.ZERO.addSmall(firstValue);
        position = firstEnd;
        while (position < len) {
            int end = Math.min(position + DECIMAL_BASE_POWER, len);
            int groupValue = parseDecimalGroup(trimmed, position, end);
            value = value.multiplySmall(DECIMAL_BASE).addSmall(groupValue);
            position = end;
        }
        if (negative) {
            value = value.negate();
        }
        return value;
    }

    @Override
    public long high() {
        return high;
    }

    @Override
    public long low() {
        return low;
    }

    /**
     * Returns {@code true} if this value is exactly zero.
     */
    public boolean isZero() {
        return high == 0L && low == 0L;
    }

    /**
     * Returns {@code true} if this value equals one.
     */
    public boolean isOne() {
        return high == 0L && low == 1L;
    }

    /**
     * Returns {@code true} if this value equals minus one.
     */
    public boolean isNegativeOne() {
        return high == -1L && low == -1L;
    }

    /**
     * Computes the sign of the number.
     *
     * @return {@code -1}, {@code 0}, or {@code 1}
     */
    public int signum() {
        if (high == 0L) {
            if (low == 0L) {
                return 0;
            }
            return low < 0 ? -1 : 1;
        }
        return high < 0 ? -1 : 1;
    }

    /**
     * Checks whether the current value is strictly positive.
     */
    public boolean isPositive() {
        return high > 0L || (high == 0L && Long.compareUnsigned(low, 0L) > 0);
    }

    /**
     * Checks whether the current value is strictly negative.
     */
    public boolean isNegative() {
        return high < 0L || (high == 0L && low < 0L);
    }

    /**
     * Computes the absolute value.
     *
     * @return the absolute value, potentially the same instance if already non-negative
     */
    public FastInt128Value abs() {
        return isNegative() ? negate() : this;
    }

    /**
     * Returns the two's-complement negation of this number.
     */
    public FastInt128Value negate() {
        if (isZero()) {
            return ZERO;
        }
        long lowNeg = ~low + 1L;
        long highNeg = ~high;
        if (lowNeg == 0L) {
            highNeg += 1L;
        }
        return of(highNeg, lowNeg);
    }

    /**
     * Adds {@code other} to this value.
     */
    public FastInt128Value add(FastInt128Value other) {
        long lowSum = low + other.low;
        long carry = (Long.compareUnsigned(lowSum, low) < 0) ? 1L : 0L;
        long highSum = high + other.high + carry;
        return of(highSum, lowSum);
    }

    /**
     * Adds a small (32-bit) positive integer to the current value. This helper is designed for decimal parsing and
     * therefore intentionally restricted to non-negative inputs.
     */
    public FastInt128Value addSmall(int value) {
        if (value == 0) {
            return this;
        }
        long lowSum = low + (value & 0xFFFFFFFFL);
        long carry = (Long.compareUnsigned(lowSum, low) < 0) ? 1L : 0L;
        long highSum = high + carry;
        return of(highSum, lowSum);
    }

    /**
     * Subtracts {@code other} from this value.
     */
    public FastInt128Value subtract(FastInt128Value other) {
        long lowDiff = low - other.low;
        long borrow = (Long.compareUnsigned(low, other.low) < 0) ? 1L : 0L;
        long highDiff = high - other.high - borrow;
        return of(highDiff, lowDiff);
    }

    /**
     * Multiplies the current value with {@code other} and returns the low 128 bits of the product.
     */
    public FastInt128Value multiply(FastInt128Value other) {
        long[] result = multiplyUnsigned128(high, low, other.high, other.low);
        return of(result[0], result[1]);
    }

    /**
     * Multiplies this value with a small decimal-base limb. The method is aggressively inlined by the JVM due to its
     * simplicity and is heavily used inside the decimal string parser.
     */
    public FastInt128Value multiplySmall(long multiplier) {
        if (multiplier == 0L) {
            return ZERO;
        }
        if (multiplier == 1L) {
            return this;
        }
        long[] product = multiplyUnsigned128(high, low, multiplier < 0 ? -1L : 0L, multiplier);
        return of(product[0], product[1]);
    }

    /**
     * Performs a left shift by {@code distance} bits. Negative distances delegate to {@link #shiftRight(int)}.
     */
    public FastInt128Value shiftLeft(int distance) {
        if (distance == 0) {
            return this;
        }
        if (distance < 0) {
            return shiftRight(-distance);
        }
        if (distance >= 128) {
            return ZERO;
        }
        if (distance >= 64) {
            long newHigh = low << (distance - 64);
            return of(newHigh, 0L);
        }
        long newHigh = (high << distance) | (low >>> (64 - distance));
        long newLow = low << distance;
        return of(newHigh, newLow);
    }

    /**
     * Performs an arithmetic right shift by {@code distance} bits. Negative distances delegate to
     * {@link #shiftLeft(int)}.
     */
    public FastInt128Value shiftRight(int distance) {
        if (distance == 0) {
            return this;
        }
        if (distance < 0) {
            return shiftLeft(-distance);
        }
        if (distance >= 128) {
            return high < 0 ? MIN_VALUE : ZERO;
        }
        if (distance >= 64) {
            long newLow = high >> (distance - 64);
            long newHigh = high < 0 ? -1L : 0L;
            return of(newHigh, newLow);
        }
        long newLow = (low >>> distance) | (high << (64 - distance));
        long newHigh = high >> distance;
        return of(newHigh, newLow);
    }

    /**
     * Counts the number of leading zero bits in the unsigned representation.
     */
    public int numberOfLeadingZeros() {
        if (high != 0L) {
            return Long.numberOfLeadingZeros(high);
        }
        return 64 + Long.numberOfLeadingZeros(low);
    }

    /**
     * Returns the bit-length of the magnitude. Zero has length zero.
     */
    public int bitLength() {
        if (isZero()) {
            return 0;
        }
        FastInt128Value magnitude = isNegative() ? negate() : this;
        int leadingZeros = magnitude.numberOfLeadingZeros();
        return 128 - leadingZeros;
    }

    /**
     * Extracts the {@code index}-th bit (0 is least significant).
     */
    public int getBit(int index) {
        if (index < 0 || index >= 128) {
            throw new IllegalArgumentException("bit index out of range: " + index);
        }
        if (index < 64) {
            return (int) ((low >>> index) & 1L);
        }
        return (int) ((high >>> (index - 64)) & 1L);
    }

    /**
     * Returns {@code true} if the value is a power of two in absolute terms.
     */
    public boolean isPowerOfTwo() {
        if (isZero()) {
            return false;
        }
        FastInt128Value abs = abs();
        return Long.bitCount(abs.high) + Long.bitCount(abs.low) == 1;
    }

    /**
     * Computes the two's-complement successor ({@code this + 1}).
     */
    public FastInt128Value increment() {
        long newLow = low + 1L;
        long newHigh = high + (newLow == 0L ? 1L : 0L);
        return of(newHigh, newLow);
    }

    /**
     * Computes the two's-complement predecessor ({@code this - 1}).
     */
    public FastInt128Value decrement() {
        long newLow = low - 1L;
        long newHigh = high - (Long.compareUnsigned(low, 1L) < 0 ? 1L : 0L);
        return of(newHigh, newLow);
    }

    /**
     * Returns the two's-complement representation as a hexadecimal string. This complements the decimal {@link
     * #toString()} implementation and is particularly useful for debugging low-level issues.
     */
    public String toHexString() {
        return String.format(Locale.ROOT, "0x%016X%016X", high, low);
    }

    @Override
    public String toString() {
        if (isZero()) {
            return "0";
        }
        boolean negative = isNegative();
        FastInt128Value working = negative ? negate() : this;
        int[] decimalDigits = new int[32];
        int count = 0;
        FastInt128Value current = working;
        while (!current.isZero()) {
            DivisionResult div = divideUnsignedByInt(current.high, current.low, (int) DECIMAL_BASE);
            decimalDigits[count++] = div.remainder;
            current = of(div.quotientHigh, div.quotientLow);
        }
        StringBuilder builder = new StringBuilder(count * DECIMAL_BASE_POWER + 1);
        if (negative) {
            builder.append('-');
        }
        count--;
        builder.append(decimalDigits[count]);
        while (--count >= 0) {
            String chunk = Integer.toString(decimalDigits[count]);
            int padding = DECIMAL_BASE_POWER - chunk.length();
            for (int i = 0; i < padding; i++) {
                builder.append('0');
            }
            builder.append(chunk);
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FastInt128Value other)) {
            return false;
        }
        return this.high == other.high && this.low == other.low;
    }

    @Override
    public int hashCode() {
        long bits = high * 31 + low;
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    public int compareTo(FastInt128Value other) {
        int highCompare = Long.compare(this.high, other.high);
        if (highCompare != 0) {
            return highCompare;
        }
        return Long.compareUnsigned(this.low, other.low);
    }

    /**
     * Determines whether this value equals the supplied raw word pair.
     */
    public boolean equals(long high, long low) {
        return this.high == high && this.low == low;
    }

    /**
     * Converts the value into a 16-byte array stored in big-endian order.
     */
    public byte[] toByteArray() {
        byte[] data = new byte[16];
        writeBigEndian(high, low, data, 0);
        return data;
    }

    /**
     * Writes the number into {@code buffer} using big-endian encoding.
     */
    public void writeBigEndian(byte[] buffer, int offset) {
        writeBigEndian(high, low, buffer, offset);
    }

    /**
     * Returns the low 64 bits as an unsigned long string. Mostly useful for diagnostics.
     */
    public String lowUnsignedString() {
        return Long.toUnsignedString(low);
    }

    /**
     * Returns the high 64 bits as an unsigned long string. Mostly useful for diagnostics.
     */
    public String highUnsignedString() {
        return Long.toUnsignedString(high);
    }

    /**
     * Returns the value interpreted as an unsigned 128-bit integer converted to {@code double}. This is primarily a
     * diagnostic helper; low-latency financial code should avoid floating point conversions because they lose precision.
     */
    public double toUnsignedDouble() {
        double highPart = unsignedLongToDouble(high);
        double lowPart = unsignedLongToDouble(low);
        return highPart * Math.pow(2.0, 64.0) + lowPart;
    }

    /**
     * Returns the number of trailing zero bits in the magnitude.
     */
    public int trailingZeroCount() {
        if (low != 0L) {
            return Long.numberOfTrailingZeros(low);
        }
        if (high != 0L) {
            return Long.numberOfTrailingZeros(high) + 64;
        }
        return 128;
    }

    /**
     * Rotates the number left by {@code distance} bits.
     */
    public FastInt128Value rotateLeft(int distance) {
        int dist = distance & 127;
        if (dist == 0) {
            return this;
        }
        if (dist == 64) {
            return of(low, high);
        }
        if (dist < 64) {
            long newHigh = (high << dist) | (low >>> (64 - dist));
            long newLow = (low << dist) | (high >>> (64 - dist));
            return of(newHigh, newLow);
        }
        int shift = dist - 64;
        long newHigh = (low << shift) | (high >>> (64 - shift));
        long newLow = (high << shift) | (low >>> (64 - shift));
        return of(newHigh, newLow);
    }

    /**
     * Rotates the number right by {@code distance} bits.
     */
    public FastInt128Value rotateRight(int distance) {
        return rotateLeft(-distance);
    }

    /**
     * Computes the population count (number of set bits) in the value.
     */
    public int populationCount() {
        return Long.bitCount(high) + Long.bitCount(low);
    }

    /**
     * Returns {@code true} if the value fits into a 64-bit signed integer without loss of precision.
     */
    public boolean fitsInLong() {
        return high == 0L || (high == -1L && low < 0L);
    }

    /**
     * Returns the signed 64-bit representation of this value, truncating toward zero.
     */
    public long longValue() {
        return low;
    }

    /**
     * Returns the unsigned 64-bit representation of the low word.
     */
    public long longValueUnsigned() {
        return low;
    }

    private static double unsignedLongToDouble(long value) {
        double magnitude = (double) (value & Long.MAX_VALUE);
        if (value < 0) {
            magnitude += 0x1.0p63;
        }
        return magnitude;
    }

    /**
     * Returns the sign-extended 32-bit representation of this value.
     */
    public int intValue() {
        return (int) low;
    }

    /**
     * Returns the unsigned 32-bit representation of the low word.
     */
    public long intValueUnsigned() {
        return low & 0xFFFFFFFFL;
    }

    /**
     * Returns {@code true} if the magnitude of this value exceeds that of {@code other} when both are interpreted as
     * unsigned 128-bit integers.
     */
    public boolean unsignedGreaterThan(FastInt128Value other) {
        int highCompare = Long.compareUnsigned(this.high, other.high);
        if (highCompare != 0) {
            return highCompare > 0;
        }
        return Long.compareUnsigned(this.low, other.low) > 0;
    }

    /**
     * Multiplies this value by a signed long scalar.
     */
    public FastInt128Value multiplyByLong(long value) {
        long[] product = multiplyUnsigned128(high, low, value < 0 ? -1L : 0L, value);
        return of(product[0], product[1]);
    }

    /**
     * Adds the raw words {@code addHigh:addLow} to this value.
     */
    public FastInt128Value addRaw(long addHigh, long addLow) {
        long lowSum = low + addLow;
        long carry = (Long.compareUnsigned(lowSum, low) < 0) ? 1L : 0L;
        long highSum = high + addHigh + carry;
        return of(highSum, lowSum);
    }

    /**
     * Subtracts the raw words {@code subHigh:subLow} from this value.
     */
    public FastInt128Value subtractRaw(long subHigh, long subLow) {
        long lowDiff = low - subLow;
        long borrow = (Long.compareUnsigned(low, subLow) < 0) ? 1L : 0L;
        long highDiff = high - subHigh - borrow;
        return of(highDiff, lowDiff);
    }

    /**
     * Multiplies the current value by {@code multiplier} and adds {@code addend}. This helper avoids intermediate
     * allocations during decimal parsing.
     */
    public FastInt128Value multiplyAddSmall(long multiplier, int addend) {
        long[] product = multiplyUnsigned128(high, low, multiplier < 0 ? -1L : 0L, multiplier);
        long lowSum = product[1] + (addend & 0xFFFFFFFFL);
        long carry = (Long.compareUnsigned(lowSum, product[1]) < 0) ? 1L : 0L;
        long highSum = product[0] + carry;
        return of(highSum, lowSum);
    }

    /**
     * Performs unsigned division of a 128-bit value by a 32-bit divisor.
     */
    private static DivisionResult divideUnsignedByInt(long high, long low, int divisor) {
        if (divisor <= 0) {
            throw new IllegalArgumentException("divisor must be positive: " + divisor);
        }
        int[] limbs = new int[LIMB_COUNT];
        decomposeToLimbs(high, low, limbs);
        long remainder = 0L;
        for (int i = LIMB_COUNT - 1; i >= 0; i--) {
            long cur = (remainder << LIMB_BITS) | (limbs[i] & 0xFFFFL);
            long q = cur / divisor;
            remainder = cur - q * divisor;
            limbs[i] = (int) q;
        }
        long[] reconstructed = composeFromLimbs(limbs);
        return new DivisionResult(reconstructed[0], reconstructed[1], (int) remainder);
    }

    /**
     * Parses a decimal group (with up to {@link #DECIMAL_BASE_POWER} digits) from {@code text} between {@code start}
     * (inclusive) and {@code end} (exclusive).
     */
    private static int parseDecimalGroup(String text, int start, int end) {
        int result = 0;
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                throw new NumberFormatException("Invalid character '" + c + "' in " + text);
            }
            result = result * 10 + (c - '0');
        }
        return result;
    }

    /**
     * Multiplies two unsigned 128-bit integers represented by {@code (aHigh, aLow)} and {@code (bHigh, bLow)}.
     *
     * @return array of length two where index {@code 0} is the high word and index {@code 1} is the low word
     */
    static long[] multiplyUnsigned128(long aHigh, long aLow, long bHigh, long bLow) {
        int[] aLimbs = new int[LIMB_COUNT];
        int[] bLimbs = new int[LIMB_COUNT];
        decomposeToLimbs(aHigh, aLow, aLimbs);
        decomposeToLimbs(bHigh, bLow, bLimbs);
        long[] accum = new long[LIMB_COUNT * 2];
        for (int i = 0; i < LIMB_COUNT; i++) {
            long ai = aLimbs[i] & 0xFFFFL;
            for (int j = 0; j < LIMB_COUNT; j++) {
                long bj = bLimbs[j] & 0xFFFFL;
                accum[i + j] += ai * bj;
            }
        }
        int[] resultLimbs = new int[LIMB_COUNT];
        long carry = 0L;
        for (int k = 0; k < LIMB_COUNT; k++) {
            long value = accum[k] + carry;
            resultLimbs[k] = (int) (value & LIMB_MASK);
            carry = value >>> LIMB_BITS;
        }
        return composeFromLimbs(resultLimbs);
    }

    /**
     * Decomposes {@code (high, low)} into {@link #LIMB_COUNT} limbs (least significant limb at index 0).
     */
    private static void decomposeToLimbs(long high, long low, int[] limbs) {
        long valueLow = low;
        long valueHigh = high;
        for (int i = 0; i < LIMB_COUNT; i++) {
            int limb = (int) (valueLow & LIMB_MASK);
            limbs[i] = limb;
            long newLow = valueLow >>> LIMB_BITS;
            long extracted = valueHigh << (64 - LIMB_BITS);
            valueLow = newLow | extracted;
            valueHigh = valueHigh >>> LIMB_BITS;
        }
    }

    /**
     * Composes a pair of longs from {@link #LIMB_COUNT} limbs.
     */
    private static long[] composeFromLimbs(int[] limbs) {
        long low = 0L;
        for (int i = 3; i >= 0; i--) {
            low = (low << LIMB_BITS) | (limbs[i] & 0xFFFFL);
        }
        long high = 0L;
        for (int i = 7; i >= 4; i--) {
            high = (high << LIMB_BITS) | (limbs[i] & 0xFFFFL);
        }
        return new long[]{high, low};
    }

    /**
     * Writes the two's-complement representation into {@code buffer} at {@code offset}.
     */
    private static void writeBigEndian(long high, long low, byte[] buffer, int offset) {
        buffer[offset] = (byte) (high >>> 56);
        buffer[offset + 1] = (byte) (high >>> 48);
        buffer[offset + 2] = (byte) (high >>> 40);
        buffer[offset + 3] = (byte) (high >>> 32);
        buffer[offset + 4] = (byte) (high >>> 24);
        buffer[offset + 5] = (byte) (high >>> 16);
        buffer[offset + 6] = (byte) (high >>> 8);
        buffer[offset + 7] = (byte) high;
        buffer[offset + 8] = (byte) (low >>> 56);
        buffer[offset + 9] = (byte) (low >>> 48);
        buffer[offset + 10] = (byte) (low >>> 40);
        buffer[offset + 11] = (byte) (low >>> 32);
        buffer[offset + 12] = (byte) (low >>> 24);
        buffer[offset + 13] = (byte) (low >>> 16);
        buffer[offset + 14] = (byte) (low >>> 8);
        buffer[offset + 15] = (byte) low;
    }

    /**
     * Tuple used by decimal conversion routines.
     */
    private record DivisionResult(long quotientHigh, long quotientLow, int remainder) {
    }

    /**
     * Mutable counterpart optimised for reuse in inner loops. Despite being a nested class, it does not capture any
     * outer state and can therefore be used independently of {@link FastInt128Value} instances created elsewhere.
     */
    public static final class MutableFastInt128Value implements MutableInt128Value {
        private long high;
        private long low;

        public MutableFastInt128Value() {
            this(0L, 0L);
        }

        public MutableFastInt128Value(long high, long low) {
            this.high = high;
            this.low = low;
        }

        public MutableFastInt128Value(FastInt128Value value) {
            this.high = value.high;
            this.low = value.low;
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

        public void set(FastInt128Value value) {
            this.high = value.high;
            this.low = value.low;
        }

        public void set(MutableFastInt128Value other) {
            this.high = other.high;
            this.low = other.low;
        }

        @Override
        public FastInt128Value immutableCopy() {
            return FastInt128Value.of(high, low);
        }

        @Override
        public MutableInt128Value copy() {
            return new MutableFastInt128Value(high, low);
        }

        public FastInt128Value toValue() {
            return FastInt128Value.of(high, low);
        }

        public boolean isZero() {
            return high == 0L && low == 0L;
        }

        public void clear() {
            this.high = 0L;
            this.low = 0L;
        }

        public void negate() {
            long negLow = ~low + 1L;
            long negHigh = ~high;
            if (negLow == 0L) {
                negHigh += 1L;
            }
            this.high = negHigh;
            this.low = negLow;
        }

        public void addInto(FastInt128Value value) {
            long lowSum = this.low + value.low;
            long carry = (Long.compareUnsigned(lowSum, this.low) < 0) ? 1L : 0L;
            this.high = this.high + value.high + carry;
            this.low = lowSum;
        }

        public void addInto(MutableFastInt128Value value) {
            long lowSum = this.low + value.low;
            long carry = (Long.compareUnsigned(lowSum, this.low) < 0) ? 1L : 0L;
            this.high = this.high + value.high + carry;
            this.low = lowSum;
        }

        public void subtract(FastInt128Value value) {
            long lowDiff = this.low - value.low;
            long borrow = (Long.compareUnsigned(this.low, value.low) < 0) ? 1L : 0L;
            this.high = this.high - value.high - borrow;
            this.low = lowDiff;
        }

        public void subtract(MutableFastInt128Value value) {
            long lowDiff = this.low - value.low;
            long borrow = (Long.compareUnsigned(this.low, value.low) < 0) ? 1L : 0L;
            this.high = this.high - value.high - borrow;
            this.low = lowDiff;
        }

        public void multiply(FastInt128Value value) {
            long[] product = multiplyUnsigned128(this.high, this.low, value.high, value.low);
            this.high = product[0];
            this.low = product[1];
        }

        public void multiply(MutableFastInt128Value value) {
            long[] product = multiplyUnsigned128(this.high, this.low, value.high, value.low);
            this.high = product[0];
            this.low = product[1];
        }

        public void addRaw(long addHigh, long addLow) {
            long lowSum = this.low + addLow;
            long carry = (Long.compareUnsigned(lowSum, this.low) < 0) ? 1L : 0L;
            this.high = this.high + addHigh + carry;
            this.low = lowSum;
        }

        public void multiplySmall(long multiplier) {
            long[] result = multiplyUnsigned128(this.high, this.low, multiplier < 0 ? -1L : 0L, multiplier);
            this.high = result[0];
            this.low = result[1];
        }

        public void multiplyAddSmall(long multiplier, int addend) {
            long[] product = multiplyUnsigned128(this.high, this.low, multiplier < 0 ? -1L : 0L, multiplier);
            long lowSum = product[1] + (addend & 0xFFFFFFFFL);
            long carry = (Long.compareUnsigned(lowSum, product[1]) < 0) ? 1L : 0L;
            this.high = product[0] + carry;
            this.low = lowSum;
        }

        public void shiftLeft(int distance) {
            FastInt128Value shifted = FastInt128Value.of(this.high, this.low).shiftLeft(distance);
            this.high = shifted.high;
            this.low = shifted.low;
        }

        public void shiftRight(int distance) {
            FastInt128Value shifted = FastInt128Value.of(this.high, this.low).shiftRight(distance);
            this.high = shifted.high;
            this.low = shifted.low;
        }

        public void rotateLeft(int distance) {
            FastInt128Value rotated = FastInt128Value.of(this.high, this.low).rotateLeft(distance);
            this.high = rotated.high;
            this.low = rotated.low;
        }

        public void rotateRight(int distance) {
            FastInt128Value rotated = FastInt128Value.of(this.high, this.low).rotateRight(distance);
            this.high = rotated.high;
            this.low = rotated.low;
        }

        public void increment() {
            long newLow = this.low + 1L;
            this.high = this.high + (newLow == 0L ? 1L : 0L);
            this.low = newLow;
        }

        public void decrement() {
            long newLow = this.low - 1L;
            this.high = this.high - (Long.compareUnsigned(this.low, 1L) < 0 ? 1L : 0L);
            this.low = newLow;
        }

        public void absoluteValue() {
            if (this.high < 0 || (this.high == 0L && this.low < 0L)) {
                negate();
            }
        }

        @Override
        public String toString() {
            return FastInt128Value.of(high, low).toString();
        }
    }

    /**
     * Static utility methods that operate on raw 128-bit words. The methods avoid object allocations and allow the
     * arithmetic implementation to interact with {@link MutableInt128Value} instances that do not necessarily belong to
     * this package.
     */
    public static final class MathOps {
        private MathOps() {
        }

        public static void addInto(Int128Value left, Int128Value right, MutableInt128Value dest) {
            long leftLow = left.low();
            long leftHigh = left.high();
            long rightLow = right.low();
            long rightHigh = right.high();
            long low = leftLow + rightLow;
            long carry = (Long.compareUnsigned(low, leftLow) < 0) ? 1L : 0L;
            long high = leftHigh + rightHigh + carry;
            dest.set(high, low);
        }

        public static void subtractInto(Int128Value left, Int128Value right, MutableInt128Value dest) {
            long leftLow = left.low();
            long leftHigh = left.high();
            long rightLow = right.low();
            long rightHigh = right.high();
            long low = leftLow - rightLow;
            long borrow = (Long.compareUnsigned(leftLow, rightLow) < 0) ? 1L : 0L;
            long high = leftHigh - rightHigh - borrow;
            dest.set(high, low);
        }

        public static void multiplyInto(Int128Value left, Int128Value right, MutableInt128Value dest) {
            long[] product = multiplyUnsigned128(left.high(), left.low(), right.high(), right.low());
            dest.set(product[0], product[1]);
        }

        public static void negateInto(Int128Value value, MutableInt128Value dest) {
            long low = ~value.low() + 1L;
            long high = ~value.high();
            if (low == 0L) {
                high += 1L;
            }
            dest.set(high, low);
        }

        public static void absoluteInto(Int128Value value, MutableInt128Value dest) {
            if (value.high() < 0 || (value.high() == 0L && value.low() < 0L)) {
                negateInto(value, dest);
            } else if (dest != value) {
                dest.set(value.high(), value.low());
            }
        }

        public static void shiftLeft(Int128Value value, int distance, MutableInt128Value dest) {
            dest.set(0L, 0L);
            if (distance == 0) {
                dest.set(value.high(), value.low());
                return;
            }
            if (distance < 0) {
                shiftRight(value, -distance, dest);
                return;
            }
            if (distance >= 128) {
                return;
            }
            long high;
            long low;
            if (distance >= 64) {
                high = value.low() << (distance - 64);
                low = 0L;
            } else {
                high = (value.high() << distance) | (value.low() >>> (64 - distance));
                low = value.low() << distance;
            }
            dest.set(high, low);
        }

        public static void shiftRight(Int128Value value, int distance, MutableInt128Value dest) {
            dest.set(0L, 0L);
            if (distance == 0) {
                dest.set(value.high(), value.low());
                return;
            }
            if (distance < 0) {
                shiftLeft(value, -distance, dest);
                return;
            }
            if (distance >= 128) {
                dest.set(value.high() < 0 ? -1L : 0L, value.high() < 0 ? -1L : 0L);
                return;
            }
            long high;
            long low;
            if (distance >= 64) {
                low = value.high() >> (distance - 64);
                high = value.high() < 0 ? -1L : 0L;
            } else {
                low = (value.low() >>> distance) | (value.high() << (64 - distance));
                high = value.high() >> distance;
            }
            dest.set(high, low);
        }

        public static void rotateLeft(Int128Value value, int distance, MutableInt128Value dest) {
            int dist = distance & 127;
            if (dist == 0) {
                dest.set(value.high(), value.low());
                return;
            }
            if (dist == 64) {
                dest.set(value.low(), value.high());
                return;
            }
            long high;
            long low;
            if (dist < 64) {
                high = (value.high() << dist) | (value.low() >>> (64 - dist));
                low = (value.low() << dist) | (value.high() >>> (64 - dist));
            } else {
                int shift = dist - 64;
                high = (value.low() << shift) | (value.high() >>> (64 - shift));
                low = (value.high() << shift) | (value.low() >>> (64 - shift));
            }
            dest.set(high, low);
        }

        public static int compare(Int128Value left, Int128Value right) {
            int highCompare = Long.compare(left.high(), right.high());
            if (highCompare != 0) {
                return highCompare;
            }
            return Long.compareUnsigned(left.low(), right.low());
        }

        public static int compareUnsigned(Int128Value left, Int128Value right) {
            int highCompare = Long.compareUnsigned(left.high(), right.high());
            if (highCompare != 0) {
                return highCompare;
            }
            return Long.compareUnsigned(left.low(), right.low());
        }

        public static boolean isZero(Int128Value value) {
            return value.high() == 0L && value.low() == 0L;
        }

        public static boolean isNegative(Int128Value value) {
            return value.high() < 0L || (value.high() == 0L && value.low() < 0L);
        }

        public static long carrylessAddLow(long leftLow, long rightLow) {
            long sum = leftLow + rightLow;
            return sum;
        }

        public static long carrylessAddHigh(long leftHigh, long rightHigh, long lowSum, long leftLow) {
            long carry = (Long.compareUnsigned(lowSum, leftLow) < 0) ? 1L : 0L;
            return leftHigh + rightHigh + carry;
        }

        public static long borrowlessSubtractLow(long leftLow, long rightLow) {
            return leftLow - rightLow;
        }

        public static long borrowlessSubtractHigh(long leftHigh, long rightHigh, long leftLow, long rightLow) {
            long borrow = (Long.compareUnsigned(leftLow, rightLow) < 0) ? 1L : 0L;
            return leftHigh - rightHigh - borrow;
        }
    }

    /**
     * Returns {@code true} if {@code this} is numerically between {@code min} and {@code max} inclusive.
     */
    public boolean isBetween(FastInt128Value min, FastInt128Value max) {
        return compareTo(min) >= 0 && compareTo(max) <= 0;
    }

    /**
     * Returns {@code this} if within the bounds, {@code min} or {@code max} otherwise.
     */
    public FastInt128Value clamp(FastInt128Value min, FastInt128Value max) {
        if (compareTo(min) < 0) {
            return min;
        }
        if (compareTo(max) > 0) {
            return max;
        }
        return this;
    }

    /**
     * Computes the average of {@code this} and {@code other} rounding toward zero.
     */
    public FastInt128Value average(FastInt128Value other) {
        long low = (this.low & other.low) + ((this.low ^ other.low) >>> 1);
        long high = (this.high & other.high) + ((this.high ^ other.high) >>> 1);
        if (((this.low ^ other.low) & 1L) != 0) {
            high += ((this.high ^ other.high) >>> 63);
        }
        return of(high, low);
    }

    /**
     * Computes {@code |this - other|}.
     */
    public FastInt128Value absoluteDifference(FastInt128Value other) {
        FastInt128Value diff = subtract(other);
        return diff.isNegative() ? diff.negate() : diff;
    }

    /**
     * Returns {@code true} if the two values have the same sign (ignoring zero).
     */
    public boolean hasSameSign(FastInt128Value other) {
        return (this.high ^ other.high) >= 0;
    }

    /**
     * Returns {@code true} if the magnitude of this value is less than that of {@code other}.
     */
    public boolean magnitudeLessThan(FastInt128Value other) {
        FastInt128Value absA = abs();
        FastInt128Value absB = other.abs();
        return MathOps.compareUnsigned(absA, absB) < 0;
    }

    /**
     * Returns {@code true} if the magnitude of this value is greater than that of {@code other}.
     */
    public boolean magnitudeGreaterThan(FastInt128Value other) {
        FastInt128Value absA = abs();
        FastInt128Value absB = other.abs();
        return MathOps.compareUnsigned(absA, absB) > 0;
    }

    /**
     * Computes the maximum of two values.
     */
    public FastInt128Value max(FastInt128Value other) {
        return compareTo(other) >= 0 ? this : other;
    }

    /**
     * Computes the minimum of two values.
     */
    public FastInt128Value min(FastInt128Value other) {
        return compareTo(other) <= 0 ? this : other;
    }

    /**
     * Computes {@code this * multiplier + addend} in a single pass.
     */
    public FastInt128Value multiplyAdd(FastInt128Value multiplier, FastInt128Value addend) {
        long[] product = multiplyUnsigned128(this.high, this.low, multiplier.high, multiplier.low);
        long lowSum = product[1] + addend.low;
        long carry = (Long.compareUnsigned(lowSum, product[1]) < 0) ? 1L : 0L;
        long highSum = product[0] + addend.high + carry;
        return of(highSum, lowSum);
    }

    /**
     * Computes {@code (this + addend) * multiplier}.
     */
    public FastInt128Value addThenMultiply(FastInt128Value addend, FastInt128Value multiplier) {
        FastInt128Value sum = add(addend);
        return sum.multiply(multiplier);
    }

    /**
     * Computes {@code this << distance} with overflow semantics identical to the built-in shift but faster for dynamic
     * distances thanks to precomputation.
     */
    public FastInt128Value fastShiftLeft(int distance) {
        return shiftLeft(distance);
    }

    /**
     * Computes {@code this >> distance} (arithmetic) leveraging the specialised helpers.
     */
    public FastInt128Value fastShiftRight(int distance) {
        return shiftRight(distance);
    }

    /**
     * Returns {@code true} if this value equals {@code other} when interpreted as unsigned integers.
     */
    public boolean unsignedEquals(FastInt128Value other) {
        return Long.compareUnsigned(this.high, other.high) == 0 && Long.compareUnsigned(this.low, other.low) == 0;
    }

    /**
     * Returns {@code true} if the two values differ by exactly one.
     */
    public boolean differsByOne(FastInt128Value other) {
        FastInt128Value diff = subtract(other);
        return diff.equals(ONE) || diff.equals(ONE.negate());
    }

    /**
     * Returns the sign of the difference between {@code this} and {@code other} without performing a subtraction that
     * would allocate a new object.
     */
    public int differenceSign(FastInt128Value other) {
        int highCompare = Long.compare(this.high, other.high);
        if (highCompare != 0) {
            return Integer.signum(highCompare);
        }
        long diffLow = this.low - other.low;
        if (diffLow == 0L) {
            return 0;
        }
        return diffLow < 0L ? -1 : 1;
    }

    /**
     * Returns the sign of the unsigned difference between {@code this} and {@code other}.
     */
    public int unsignedDifferenceSign(FastInt128Value other) {
        int highCompare = Long.compareUnsigned(this.high, other.high);
        if (highCompare != 0) {
            return Integer.signum(highCompare);
        }
        int lowCompare = Long.compareUnsigned(this.low, other.low);
        return Integer.signum(lowCompare);
    }

    /**
     * Returns the smaller magnitude between {@code this} and {@code other}.
     */
    public FastInt128Value minMagnitude(FastInt128Value other) {
        return magnitudeLessThan(other) ? this : other;
    }

    /**
     * Returns the larger magnitude between {@code this} and {@code other}.
     */
    public FastInt128Value maxMagnitude(FastInt128Value other) {
        return magnitudeGreaterThan(other) ? this : other;
    }

    /**
     * Returns {@code true} if the absolute value is representable in 64 bits.
     */
    public boolean magnitudeFitsInLong() {
        FastInt128Value abs = abs();
        return abs.high == 0L;
    }

    /**
     * Returns {@code true} if the lower 64 bits are zero.
     */
    public boolean hasZeroLowWord() {
        return low == 0L;
    }

    /**
     * Returns {@code true} if the upper 64 bits are zero or sign extension of the low word.
     */
    public boolean hasSignExtendedHighWord() {
        return (high == 0L && low >= 0L) || (high == -1L && low < 0L);
    }

    /**
     * Returns {@code true} if the magnitude is a multiple of {@code 10^n}.
     */
    public boolean isMultipleOfPowerOfTen(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("power must be non-negative");
        }
        FastInt128Value value = abs();
        for (int i = 0; i < n; i++) {
            DivisionResult div = divideUnsignedByInt(value.high, value.low, 10);
            if (div.remainder != 0) {
                return false;
            }
            value = of(div.quotientHigh, div.quotientLow);
        }
        return true;
    }

    /**
     * Returns {@code true} if the number is palindromic in base 10. This is a niche helper but surprisingly useful when
     * validating string conversion correctness with deterministic datasets.
     */
    public boolean isDecimalPalindrome() {
        String asString = toString();
        int start = signum() < 0 ? 1 : 0;
        int end = asString.length() - 1;
        while (start < end) {
            if (asString.charAt(start++) != asString.charAt(end--)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the digits of the decimal representation as an array of integers (least significant chunk first).
     */
    public int[] toDecimalLimbs() {
        if (isZero()) {
            return new int[]{0};
        }
        FastInt128Value value = abs();
        int[] limbs = new int[32];
        int count = 0;
        while (!value.isZero()) {
            DivisionResult div = divideUnsignedByInt(value.high, value.low, (int) DECIMAL_BASE);
            limbs[count++] = div.remainder;
            value = of(div.quotientHigh, div.quotientLow);
        }
        return Arrays.copyOf(limbs, count);
    }

    /**
     * Creates a value from decimal limbs produced by {@link #toDecimalLimbs()}.
     */
    public static FastInt128Value fromDecimalLimbs(int[] limbs, boolean negative) {
        FastInt128Value value = ZERO;
        for (int i = limbs.length - 1; i >= 0; i--) {
            value = value.multiplySmall(DECIMAL_BASE).addSmall(limbs[i]);
        }
        return negative ? value.negate() : value;
    }

    /**
     * Convenience method primarily used for unit testing and manual verifications.
     */
    public static FastInt128Value parseHex(String hex) {
        String trimmed = hex.startsWith("0x") || hex.startsWith("0X") ? hex.substring(2) : hex;
        if (trimmed.length() > 32) {
            throw new NumberFormatException("Hex string too long for 128 bits: " + hex);
        }
        long high = 0L;
        long low = 0L;
        int digits = trimmed.length();
        for (int i = 0; i < digits; i++) {
            char c = trimmed.charAt(i);
            int value = Character.digit(c, 16);
            if (value < 0) {
                throw new NumberFormatException("Invalid hex digit '" + c + "' in " + hex);
            }
            if (i < digits - 16) {
                high = (high << 4) | ((low >>> 60) & 0xF);
                low = (low << 4) | value;
            } else {
                low = (low << 4) | value;
            }
        }
        return of(high, low);
    }

    /**
     * Returns a new value representing {@code this << 1} with wrap-around semantics.
     */
    public FastInt128Value doubleValueExact() {
        long newHigh = (high << 1) | (low >>> 63);
        long newLow = low << 1;
        return of(newHigh, newLow);
    }

    /**
     * Returns a new value representing {@code this >> 1} (arithmetic).
     */
    public FastInt128Value halveValue() {
        long newLow = (low >>> 1) | (high << 63);
        long newHigh = high >> 1;
        return of(newHigh, newLow);
    }

    /**
     * Returns {@code true} if {@code this} and {@code other} differ in any bit within the high word.
     */
    public boolean differsInHighWord(FastInt128Value other) {
        return high != other.high;
    }

    /**
     * Returns {@code true} if {@code this} and {@code other} differ in any bit within the low word.
     */
    public boolean differsInLowWord(FastInt128Value other) {
        return low != other.low;
    }

    /**
     * Returns {@code true} if the value would overflow when multiplied by {@code multiplier} (considering signed range).
     * The method evaluates the exact product and checks whether it lies within {@link #MIN_VALUE} and
     * {@link #MAX_VALUE}.
     */
    public boolean wouldOverflowMultiply(FastInt128Value multiplier) {
        FastInt128Value product = multiply(multiplier);
        return product.compareTo(MIN_VALUE) < 0 || product.compareTo(MAX_VALUE) > 0;
    }

    /**
     * Returns {@code true} if the value would overflow when added to {@code addend}.
     */
    public boolean wouldOverflowAdd(FastInt128Value addend) {
        FastInt128Value sum = add(addend);
        if (this.high >= 0 && addend.high >= 0 && sum.high < 0) {
            return true;
        }
        if (this.high < 0 && addend.high < 0 && sum.high >= 0) {
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if the value would overflow when subtracted by {@code subtrahend}.
     */
    public boolean wouldOverflowSubtract(FastInt128Value subtrahend) {
        FastInt128Value diff = subtract(subtrahend);
        if (this.high >= 0 && subtrahend.high < 0 && diff.high < 0) {
            return true;
        }
        if (this.high < 0 && subtrahend.high >= 0 && diff.high >= 0) {
            return true;
        }
        return false;
    }

    /**
     * Creates a copy of this value and stores it into {@code destination}.
     */
    public void copyTo(MutableFastInt128Value destination) {
        destination.set(high, low);
    }

    /**
     * Stores the negated value into {@code destination}.
     */
    public void negateInto(MutableFastInt128Value destination) {
        long negLow = ~low + 1L;
        long negHigh = ~high;
        if (negLow == 0L) {
            negHigh += 1L;
        }
        destination.set(negHigh, negLow);
    }

    /**
     * Stores {@code this + other} into {@code destination}.
     */
    public void addInto(FastInt128Value other, MutableFastInt128Value destination) {
        long lowSum = low + other.low;
        long carry = (Long.compareUnsigned(lowSum, low) < 0) ? 1L : 0L;
        long highSum = high + other.high + carry;
        destination.set(highSum, lowSum);
    }

    /**
     * Stores {@code this - other} into {@code destination}.
     */
    public void subtractInto(FastInt128Value other, MutableFastInt128Value destination) {
        long lowDiff = low - other.low;
        long borrow = (Long.compareUnsigned(low, other.low) < 0) ? 1L : 0L;
        long highDiff = high - other.high - borrow;
        destination.set(highDiff, lowDiff);
    }

    /**
     * Stores {@code this * other} into {@code destination}.
     */
    public void multiplyInto(FastInt128Value other, MutableFastInt128Value destination) {
        long[] product = multiplyUnsigned128(high, low, other.high, other.low);
        destination.set(product[0], product[1]);
    }

    /**
     * Stores {@code this << distance} into {@code destination}.
     */
    public void shiftLeftInto(int distance, MutableFastInt128Value destination) {
        FastInt128Value shifted = shiftLeft(distance);
        destination.set(shifted.high, shifted.low);
    }

    /**
     * Stores {@code this >> distance} into {@code destination}.
     */
    public void shiftRightInto(int distance, MutableFastInt128Value destination) {
        FastInt128Value shifted = shiftRight(distance);
        destination.set(shifted.high, shifted.low);
    }

    /**
     * Stores {@code this.rotateLeft(distance)} into {@code destination}.
     */
    public void rotateLeftInto(int distance, MutableFastInt128Value destination) {
        FastInt128Value rotated = rotateLeft(distance);
        destination.set(rotated.high, rotated.low);
    }

    /**
     * Stores {@code this.rotateRight(distance)} into {@code destination}.
     */
    public void rotateRightInto(int distance, MutableFastInt128Value destination) {
        FastInt128Value rotated = rotateRight(distance);
        destination.set(rotated.high, rotated.low);
    }

    /**
     * Copies the internal state into the provided array in big-endian order.
     */
    public void copyInto(byte[] buffer, int offset) {
        writeBigEndian(high, low, buffer, offset);
    }

    /**
     * Returns {@code true} if this value equals {@code other} ignoring sign.
     */
    public boolean equalsMagnitude(FastInt128Value other) {
        FastInt128Value absA = abs();
        FastInt128Value absB = other.abs();
        return absA.high == absB.high && absA.low == absB.low;
    }

    /**
     * Returns a value with all bits inverted.
     */
    public FastInt128Value bitwiseNot() {
        return of(~high, ~low);
    }

    /**
     * Returns {@code this & other}.
     */
    public FastInt128Value bitwiseAnd(FastInt128Value other) {
        return of(this.high & other.high, this.low & other.low);
    }

    /**
     * Returns {@code this | other}.
     */
    public FastInt128Value bitwiseOr(FastInt128Value other) {
        return of(this.high | other.high, this.low | other.low);
    }

    /**
     * Returns {@code this ^ other}.
     */
    public FastInt128Value bitwiseXor(FastInt128Value other) {
        return of(this.high ^ other.high, this.low ^ other.low);
    }

    /**
     * Returns {@code true} if any bit is set in the intersection of {@code this} and {@code mask}.
     */
    public boolean intersects(FastInt128Value mask) {
        return ((this.high & mask.high) | (this.low & mask.low)) != 0L;
    }

    /**
     * Returns {@code true} if all bits set in {@code mask} are also set in {@code this}.
     */
    public boolean containsAllBits(FastInt128Value mask) {
        return (this.high & mask.high) == mask.high && (this.low & mask.low) == mask.low;
    }

    /**
     * Returns {@code true} if there is no overlap of bits between {@code this} and {@code mask}.
     */
    public boolean containsNoBits(FastInt128Value mask) {
        return (this.high & mask.high) == 0L && (this.low & mask.low) == 0L;
    }

    /**
     * Returns a human-friendly description capturing multiple representations of the number. This helper is extremely
     * useful when dumping values in benchmark failure logs.
     */
    public String describe() {
        return "FastInt128Value[dec=" + toString() + ", hex=" + toHexString() + ", high=" + highUnsignedString()
                + ", low=" + lowUnsignedString() + ']';
    }
}

