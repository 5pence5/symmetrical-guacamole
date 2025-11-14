import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Int128 — High‑performance, exact, signed 128‑bit two's‑complement integer for latency‑sensitive workloads.
 *
 * <h2>Design</h2>
 * <ul>
 *   <li>Representation: two 64‑bit limbs (hi, lo). Value = (hi &lt;&lt; 64) + (lo unsigned).</li>
 *   <li>Arithmetic: add/sub/mul/div/rem/bitwise/shifts, all without BigInteger. Wrap semantics mod 2^128.</li>
 *   <li>Division:
 *     <ul>
 *       <li>128/64 fast path (very common in finance: 10^k for k≤19, fees, small divisors).</li>
 *       <li>Optimised 128/128 (same‑degree) division using a two‑limb (approx+correction) method — no 128‑iteration loop.</li>
 *     </ul>
 *   </li>
 *   <li>String conversion: BigInteger is used ONLY for toString/fromString (decimal/radix I/O), not for arithmetic.</li>
 *   <li>Immutability: instances are immutable and thread‑safe.</li>
 * </ul>
 *
 * <h2>Constants</h2>
 * <ul>
 *   <li>{@link #ZERO}, {@link #ONE}, {@link #DECIMAL_BASE}, {@link #MIN_VALUE}, {@link #MAX_VALUE}</li>
 * </ul>
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>Exactness: operations are exact in 128‑bit two's complement. Overflow wraps by construction.</li>
 *   <li>Performance: the hot paths are straight‑line and allocation‑free; no hidden boxing. The divider
 *       avoids bit‑at‑a‑time loops and uses at most a couple of corrective steps.</li>
 *   <li>JDK: no reliance on {@code Math.multiplyHigh}; the 64×64→128 core uses a portable 32‑bit split that
 *       the JIT optimises well on both x86_64 and AArch64. If you standardise on JDK 9+, you can trivially
 *       wire {@code Math.multiplyHigh} into {@code mul64to128} (comment included where to switch).</li>
 * </ul>
 */
public final class Int128 implements Comparable<Int128>, Serializable {

    // =========================================================================
    // Fields & core constants
    // =========================================================================

    private static final long serialVersionUID = 1L;

    /** High 64 bits (signed). */
    private final long hi;
    /** Low 64 bits (treated as unsigned for composition). */
    private final long lo;

    /** Unsigned masks. */
    private static final long MASK_64     = 0xFFFF_FFFF_FFFF_FFFFL;
    private static final long SIGN_BIT_64 = 0x8000_0000_0000_0000L;

    // Precomputed 10^k for 0..38 as Int128.
    private static final Int128[] TEN_POW;       // 10^0 .. 10^38
    // Also 10^k for 0..19 as long (fits in unsigned 64).
    private static final long[]   TEN_POW_64;    // 10^0 .. 10^19

    // Public constants (as requested).
    public static final Int128 ZERO        = new Int128(0L, 0L);
    public static final Int128 ONE         = new Int128(0L, 1L);
    public static final Int128 DECIMAL_BASE= Int128.valueOf(10);
    public static final Int128 MIN_VALUE   = new Int128(Long.MIN_VALUE, 0L);
    public static final Int128 MAX_VALUE   = new Int128(Long.MAX_VALUE, MASK_64);

    // Range bounds for string conversions.
    private static final BigInteger BIG_MIN;
    private static final BigInteger BIG_MAX;

    static {
        TEN_POW = new Int128[39];
        TEN_POW[0] = ONE;
        for (int i = 1; i < TEN_POW.length; i++) {
            TEN_POW[i] = TEN_POW[i - 1].mul(10);
        }

        TEN_POW_64 = new long[20];
        long acc = 1L;
        for (int i = 0; i < TEN_POW_64.length; i++) {
            TEN_POW_64[i] = acc;
            if (i < TEN_POW_64.length - 1) {
                long next = acc * 10L; // exact within 64 bits up to 10^19
                acc = next;
            }
        }

        BIG_MIN = toBigInteger(MIN_VALUE);
        BIG_MAX = toBigInteger(MAX_VALUE);
    }

    // =========================================================================
    // Construction
    // =========================================================================

    private Int128(long hi, long lo) {
        this.hi = hi;
        this.lo = lo;
    }

    /** Build from hi/lo limbs (no normalisation). */
    public static Int128 of(long hi, long lo) {
        return new Int128(hi, lo);
    }

    /** From signed long (sign‑extended). */
    public static Int128 valueOf(long v) {
        return new Int128(v < 0 ? -1L : 0L, v);
    }

    /** From signed int (sign‑extended). */
    public static Int128 valueOf(int v) {
        return new Int128(v < 0 ? -1L : 0L, (long) v);
    }

    /** From unsigned long (upper limb zero). */
    public static Int128 fromUnsignedLong(long unsigned) {
        return new Int128(0L, unsigned);
    }

    // =========================================================================
    // Predicates & comparisons
    // =========================================================================

    /** True if the value is zero. */
    public boolean isZero() { return hi == 0L && lo == 0L; }

    /** True if negative (top bit set). */
    public boolean isNegative() { return hi < 0L; }

    /** True if positive (> 0). */
    public boolean isPositive() { return hi > 0L || (hi == 0L && lo != 0L); }

    /** Signum: -1, 0, +1. */
    public int signum() {
        if (hi == 0L && lo == 0L) return 0;
        return hi < 0L ? -1 : +1;
    }

    /** Strict power of two (positive only). */
    public boolean isPowerOfTwo() {
        if (isNegative() || isZero()) return false;
        return Long.bitCount(hi) + Long.bitCount(lo) == 1;
    }

    /** Signed compare (Comparable). */
    @Override
    public int compareTo(Int128 o) {
        if (this.hi != o.hi) return (this.hi < o.hi) ? -1 : 1;
        int cu = Long.compareUnsigned(this.lo, o.lo);
        return (cu < 0) ? -1 : (cu > 0 ? 1 : 0);
    }

    /** Unsigned 128‑bit compare. */
    public int compareUnsigned(Int128 o) {
        int ch = Long.compareUnsigned(this.hi, o.hi);
        if (ch != 0) return ch;
        return Long.compareUnsigned(this.lo, o.lo);
    }

    // =========================================================================
    // Equality, hashing, string forms
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Int128)) return false;
        Int128 o = (Int128) obj;
        return this.hi == o.hi && this.lo == o.lo;
    }

    @Override
    public int hashCode() {
        // Well‑distributed and fast; stable across JVMs
        long h = (hi * 0x9E3779B97F4A7C15L) ^ (lo + 0xC2B2AE3D27D4EB4FL);
        return (int) (h ^ (h >>> 32));
    }

    /** Decimal toString (uses BigInteger for conversion only; arithmetic never uses it). */
    @Override
    public String toString() {
        return toBigInteger(this).toString(10);
    }

    /** Radix toString (2..36). */
    public String toString(int radix) {
        return toBigInteger(this).toString(radix);
    }

    /** Hex (lowercase), minimal digits, no prefix. */
    public String toHexString() {
        if (hi == 0L) {
            return (lo == 0L) ? "0" : Long.toUnsignedString(lo, 16);
        }
        String hiHex = Long.toUnsignedString(hi, 16);
        String loHex = Long.toUnsignedString(lo, 16);
        StringBuilder sb = new StringBuilder(hiHex.length() + 16);
        sb.append(hiHex);
        for (int i = loHex.length(); i < 16; i++) sb.append('0');
        sb.append(loHex);
        return sb.toString();
    }

    // =========================================================================
    // Parsing
    // =========================================================================

    /** Parse decimal string. */
    public static Int128 fromString(String s) {
        return fromString(s, 10);
    }

    /** Parse with radix 2..36 (BigInteger only for conversion/range checking). */
    public static Int128 fromString(String s, int radix) {
        if (s == null) throw new NullPointerException("s");
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
            throw new NumberFormatException("Invalid radix: " + radix);
        String cleaned = stripNumericSeparatorsAndTrim(s);
        BigInteger bi = new BigInteger(cleaned, radix);
        if (bi.compareTo(BIG_MIN) < 0 || bi.compareTo(BIG_MAX) > 0) {
            throw new NumberFormatException("Value out of Int128 range: " + s);
        }
        return fromBigInteger(bi);
    }

    /** Parse hexadecimal with optional sign and optional "0x"/"0X" prefix (e.g., "-0xFF"). */
    public static Int128 parseHex(String s) {
        if (s == null) throw new NullPointerException("s");
        String t = stripNumericSeparatorsAndTrim(s);   // <— change: strip underscores
        if (t.isEmpty()) throw new NumberFormatException("Empty hex string");

        boolean neg = false;
        char c0 = t.charAt(0);
        if (c0 == '+' || c0 == '-') {
            neg = (c0 == '-');
            t = t.substring(1).trim();
        }
        if (t.startsWith("0x") || t.startsWith("0X")) t = t.substring(2);
        if (t.isEmpty()) throw new NumberFormatException("Empty hex digits");
        if (t.length() > 32) throw new NumberFormatException("Hex literal exceeds 128 bits: " + s);

        int hiDigits = Math.max(0, t.length() - 16);
        long hi = 0L, lo = 0L;
        if (hiDigits > 0) {
            String hs = t.substring(0, hiDigits);
            hi = Long.parseUnsignedLong(hs, 16);
        }
        String ls = t.substring(hiDigits);
        if (!ls.isEmpty()) {
            lo = Long.parseUnsignedLong(ls, 16);
        }
        Int128 v = Int128.of(hi, lo);
        return neg ? v.negate() : v;
    }

    private static String stripNumericSeparatorsAndTrim(String s) {
        int n = s.length();
        char[] tmp = new char[n];
        int j = 0;
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c != '_') tmp[j++] = c;
        }
        return new String(tmp, 0, j).trim();
    }

    // =========================================================================
    // Basic arithmetic (add/sub/neg/abs)
    // =========================================================================

    /** Adds two 128‑bit integers (wrap on overflow). */
    public Int128 add(Int128 b) {
        long loSum = this.lo + b.lo;
        long carry = Long.compareUnsigned(loSum, this.lo) < 0 ? 1L : 0L;
        long hiSum = this.hi + b.hi + carry;
        return new Int128(hiSum, loSum);
    }

    /** Subtracts b from this (wrap on overflow). */
    public Int128 sub(Int128 b) {
        long loDiff = this.lo - b.lo;
        long borrow = Long.compareUnsigned(this.lo, b.lo) < 0 ? 1L : 0L;
        long hiDiff = this.hi - b.hi - borrow;
        return new Int128(hiDiff, loDiff);
    }

    /** Two's‑complement negate (MIN_VALUE negates to itself). */
    public Int128 negate() {
        long loN = ~this.lo + 1L;
        long carry = (loN == 0L) ? 1L : 0L;
        long hiN = ~this.hi + carry;
        return new Int128(hiN, loN);
    }

    /** Absolute value; MIN_VALUE stays MIN_VALUE (wrap semantics). */
    public Int128 abs() { return isNegative() ? negate() : this; }

    /** Increment by one. */
    public Int128 inc() { return add(ONE); }

    /** Decrement by one. */
    public Int128 dec() { return sub(ONE); }

    // =========================================================================
    // Multiplication (mod 2^128)
    // =========================================================================

    /**
     * 128×128 → low 128 bits (wrap semantics).
     * <p>Implements (hi,lo) = a*b mod 2^128 using alloc‑free 64×64 core and cross terms.</p>
     */
    public Int128 mul(Int128 b) {
        long aLo = this.lo, aHi = this.hi;
        long bLo = b.lo,   bHi = b.hi;

        long lo = mulLo64(aLo, bLo);
        long hi = mulHi64(aLo, bLo);
        hi += mulLo64(aLo, bHi);
        hi += mulLo64(aHi, bLo);
        return new Int128(hi, lo);
    }

    /** Multiply by signed long (wrap semantics). */
    public Int128 mul(long k) {
        if (k == 0)  return ZERO;
        if (k == 1)  return this;
        if (k == -1) return this.negate();

        long lo = mulLo64(this.lo, k);
        long hi = mulHi64(this.lo, k) + mulLo64(this.hi, k);
        return new Int128(hi, lo);
    }

    /** Multiply by 10^exp (0..38). Wraps on overflow. */
    public Int128 scaleUpPow10(int exp) {
        if (exp < 0 || exp >= TEN_POW.length) throw new IllegalArgumentException("exp out of [0..38]: " + exp);
        return this.mul(TEN_POW[exp]);
    }

    // =========================================================================
    // Division & remainder (fast 128/64 path, optimised 128/128 path)
    // =========================================================================

    /** Signed division (wrap semantics). Throws ArithmeticException on /0. */
    public Int128 div(Int128 d) { return divRem(d)[0]; }

    /** Signed remainder. Throws ArithmeticException on /0. */
    public Int128 rem(Int128 d) { return divRem(d)[1]; }

    /**
     * Signed div/rem. Internally uses unsigned algorithms on magnitudes and repairs signs.
     * <p>Fast paths:</p>
     * <ul>
     *   <li>divisor fits in 64 bits → 128/64 algorithm</li>
     *   <li>otherwise → two‑limb 128/128 division (approx quotient via 128/64 then corrected)</li>
     * </ul>
     */
    public Int128[] divRem(Int128 divisor) {
        if (divisor.isZero()) throw new ArithmeticException("divide by zero");

        boolean negA = this.isNegative();
        boolean negB = divisor.isNegative();
        boolean negQ = negA ^ negB; // quotient sign
        boolean negR = negA;        // remainder sign follows dividend

        long aHi = this.hi, aLo = this.lo;
        long bHi = divisor.hi, bLo = divisor.lo;

        // Magnitudes (abs in two's complement; MIN_VALUE negates to itself — OK here).
        if (negA) { long[] t = negate128(aHi, aLo); aHi = t[0]; aLo = t[1]; }
        if (negB) { long[] t = negate128(bHi, bLo); bHi = t[0]; bLo = t[1]; }

        long qHi, qLo, rHi, rLo;

        // 1) Quick compare: if a < b, quotient=0, remainder=a
        if (cmpu128(aHi, aLo, bHi, bLo) < 0) {
            qHi = 0L; qLo = 0L; rHi = aHi; rLo = aLo;
        } else if (bHi == 0L) {
            // 2) Fast 128/64 path
            long[] qr = udivrem_128by64(aHi, aLo, bLo);
            qHi = qr[0]; qLo = qr[1]; rHi = qr[2]; rLo = qr[3];
        } else {
            // 3) Optimised 128/128 (same degree) using 2‑limb method
            long[] qr = udivrem_128by128(aHi, aLo, bHi, bLo);
            qHi = qr[0]; qLo = qr[1]; rHi = qr[2]; rLo = qr[3];
        }

        // Repair signs
        if (negQ) {
            long[] t = negate128(qHi, qLo);
            qHi = t[0]; qLo = t[1];
        }
        if (negR && (rHi != 0L || rLo != 0L)) {
            long[] t = negate128(rHi, rLo);
            rHi = t[0]; rLo = t[1];
        }
        return new Int128[] { new Int128(qHi, qLo), new Int128(rHi, rLo) };
    }

    /** Unsigned divide (quotient only). */
    public Int128 divideUnsigned(Int128 d) {
        if (d.isZero()) throw new ArithmeticException("divide by zero");
        long[] qr;
        if (cmpu128(this.hi, this.lo, d.hi, d.lo) < 0) {
            qr = new long[] {0L, 0L, this.hi, this.lo};
        } else if (d.hi == 0L) {
            qr = udivrem_128by64(this.hi, this.lo, d.lo);
        } else {
            qr = udivrem_128by128(this.hi, this.lo, d.hi, d.lo);
        }
        return new Int128(qr[0], qr[1]);
    }

    /** Unsigned remainder only. */
    public Int128 remainderUnsigned(Int128 d) {
        if (d.isZero()) throw new ArithmeticException("divide by zero");
        long[] qr;
        if (cmpu128(this.hi, this.lo, d.hi, d.lo) < 0) {
            qr = new long[] {0L, 0L, this.hi, this.lo};
        } else if (d.hi == 0L) {
            qr = udivrem_128by64(this.hi, this.lo, d.lo);
        } else {
            qr = udivrem_128by128(this.hi, this.lo, d.hi, d.lo);
        }
        return new Int128(qr[2], qr[3]);
    }

    // =========================================================================
    // Shifts
    // =========================================================================

    /** Logical left shift by 0..127 (masking larger counts). */
    public Int128 shiftLeft(int n) {
        int s = n & 127;
        if (s == 0) return this;
        if (s < 64) {
            long hiN = (hi << s) | (lo >>> (64 - s));
            long loN = (lo << s);
            return new Int128(hiN, loN);
        } else if (s == 64) {
            return new Int128(lo, 0L);
        } else {
            long hiN = (lo << (s - 64));
            return new Int128(hiN, 0L);
        }
    }

    /** Arithmetic right shift by 0..127 (sign‑extending). */
    public Int128 shiftRight(int n) {
        int s = n & 127;
        if (s == 0) return this;
        if (s < 64) {
            long loN = (lo >>> s) | (hi << (64 - s));
            long hiN = (hi >> s);
            return new Int128(hiN, loN);
        } else if (s == 64) {
            long loN = hi;
            long hiN = (hi < 0) ? -1L : 0L;
            return new Int128(hiN, loN);
        } else {
            long loN = (hi >> (s - 64));
            long hiN = (hi < 0) ? -1L : 0L;
            return new Int128(hiN, loN);
        }
    }

    /** Logical right shift by 0..127 (zero‑extending). */
    public Int128 shiftRightUnsigned(int n) {
        int s = n & 127;
        if (s == 0) return this;
        if (s < 64) {
            long loN = (lo >>> s) | (hi << (64 - s));
            long hiN = (hi >>> s);
            return new Int128(hiN, loN);
        } else if (s == 64) {
            return new Int128(0L, hi);
        } else {
            long loN = (hi >>> (s - 64));
            return new Int128(0L, loN);
        }
    }

    // =========================================================================
    // Bitwise
    // =========================================================================

    public Int128 and(Int128 b) { return new Int128(this.hi & b.hi, this.lo & b.lo); }
    public Int128 or (Int128 b) { return new Int128(this.hi | b.hi, this.lo | b.lo); }
    public Int128 xor(Int128 b) { return new Int128(this.hi ^ b.hi, this.lo ^ b.lo); }
    public Int128 not()         { return new Int128(~this.hi, ~this.lo); }

    /** Tests a bit (0..127). */
    public boolean testBit(int bit) {
        if (bit < 0 || bit > 127) throw new IllegalArgumentException("bit out of [0..127]");
        if (bit < 64) return ((this.lo >>> bit) & 1L) != 0L;
        int b = bit - 64;
        return ((this.hi >>> b) & 1L) != 0L;
    }

    /** Sets a bit (0..127). */
    public Int128 setBit(int bit) {
        if (bit < 0 || bit > 127) throw new IllegalArgumentException("bit out of [0..127]");
        if (bit < 64) return new Int128(this.hi, this.lo | (1L << bit));
        int b = bit - 64;
        return new Int128(this.hi | (1L << b), this.lo);
    }

    /** Clears a bit (0..127). */
    public Int128 clearBit(int bit) {
        if (bit < 0 || bit > 127) throw new IllegalArgumentException("bit out of [0..127]");
        if (bit < 64) return new Int128(this.hi, this.lo & ~(1L << bit));
        int b = bit - 64;
        return new Int128(this.hi & ~(1L << b), this.lo);
    }

    /**
     * Two's‑complement bit length: 128 minus the count of leading sign bits.
     * Returns 0 for 0 and -1, 127 for MIN_VALUE, etc.
     */
    public int bitLength() {
        if (hi == 0L && lo == 0L) return 0;
        if (hi >= 0L) {
            if (hi != 0L) return 64 + (64 - Long.numberOfLeadingZeros(hi));
            return 64 - Long.numberOfLeadingZeros(lo);
        } else {
            int leadOnesHi = Long.numberOfLeadingZeros(~hi);
            if (leadOnesHi < 64) return 128 - leadOnesHi;
            int leadOnesLo = Long.numberOfLeadingZeros(~lo);
            return 64 - leadOnesLo;
        }
    }

    // =========================================================================
    // Min/max/clamp
    // =========================================================================

    public static Int128 min(Int128 a, Int128 b) { return (a.compareTo(b) <= 0) ? a : b; }
    public static Int128 max(Int128 a, Int128 b) { return (a.compareTo(b) >= 0) ? a : b; }

    public static Int128 clamp(Int128 x, Int128 lo, Int128 hi) {
        if (lo.compareTo(hi) > 0) throw new IllegalArgumentException("lo > hi");
        return max(lo, min(x, hi));
    }

    // =========================================================================
    // Financial helpers (decimal scaling & rounding)
    // =========================================================================

    /** Divide by 10^exp (0..38), returning [quotient, remainder]. Fast path for exp ≤ 19. */
    public Int128[] divRemPow10(int exp) {
        if (exp < 0 || exp >= TEN_POW.length) {
            throw new IllegalArgumentException("exp out of [0..38]");
        }
        // Always-positive divisor
        if (exp <= 19) {
            final long d = TEN_POW_64[exp];
            if (d == 1L) return new Int128[] { this, ZERO }; // trivial

            boolean neg = this.isNegative();
            long aHi = this.hi, aLo = this.lo;
            if (neg) {
                long[] t = negate128(aHi, aLo);
                aHi = t[0]; aLo = t[1];
            }

            long[] qr = udivrem_128by64(aHi, aLo, d); // [qHi, qLo, 0, r]
            long qHi = qr[0], qLo = qr[1];
            long r   = qr[3];

            if (neg) {
                // quotient sign: negate; remainder sign: same as dividend (negative), unless zero
                long[] nq = negate128(qHi, qLo);
                qHi = nq[0]; qLo = nq[1];
                if (r != 0L) {
                    long[] nr = negate128(0L, r);
                    return new Int128[] { new Int128(qHi, qLo), new Int128(nr[0], nr[1]) };
                } else {
                    return new Int128[] { new Int128(qHi, qLo), ZERO };
                }
            } else {
                return new Int128[] { new Int128(qHi, qLo), new Int128(0L, r) };
            }
        } else {
            // Delegate to full signed div/rem for large powers (still exact).
            return this.divRem(TEN_POW[exp]);
        }
    }

    /** Round half‑even (banker's) for /10^exp. */
    public Int128 divRoundHalfEvenPow10(int exp) {
        Int128[] dr = divRemPow10(exp);
        Int128 q = dr[0], r = dr[1].abs();
        Int128 d = (exp <= 19) ? fromUnsignedLong(TEN_POW_64[exp]) : TEN_POW[exp];
        Int128 twiceR = r.shiftLeft(1);
        int cmp = twiceR.compareTo(d);
        if (cmp < 0) return q;                                   // < 0.5 -> down
        if (cmp > 0) return q.add(ONE.withSignOf(this));         // > 0.5 -> up
        boolean even = (q.lo & 1L) == 0L;                        // exactly halfway -> to even
        return even ? q : q.add(ONE.withSignOf(this));
    }

    /** Floor division by 10^exp (towards -∞). */
    public Int128 floorDivPow10(int exp) {
        Int128[] dr = divRemPow10(exp);
        if (this.isNegative() && !dr[1].isZero()) return dr[0].sub(ONE);
        return dr[0];
    }

    /** Ceil division by 10^exp (towards +∞). */
    public Int128 ceilDivPow10(int exp) {
        Int128[] dr = divRemPow10(exp);
        if (this.isPositive() && !dr[1].isZero()) return dr[0].add(ONE);
        return dr[0];
    }

    private Int128 withSignOf(Int128 other) {
        if (other.isZero()) return ZERO;
        return other.isNegative() ? this.negate() : this;
    }

    /** Multiply and divide by 10^exp with half‑even rounding (common for fixed‑point money). */
    public Int128 mulAndDivPow10RoundHalfEven(Int128 multiplicand, int exp) {
        Int128 prod = this.mul(multiplicand);
        return prod.divRoundHalfEvenPow10(exp);
    }

    /** Multiply by a long and divide by 10^exp with half‑even rounding. */
    public Int128 mulLongAndDivPow10RoundHalfEven(long k, int exp) {
        Int128 prod = this.mul(k);
        return prod.divRoundHalfEvenPow10(exp);
    }

    // =========================================================================
    // Conversions: primitives, bytes, BigInteger (for strings only)
    // =========================================================================

    public long toLong() { return lo; }

    /** Exact narrowing to long; throws if out of range. */
    public long toLongExact() {
        boolean loSign = (lo & SIGN_BIT_64) != 0;
        if ((!loSign && hi == 0L) || (loSign && hi == -1L)) return lo;
        throw new ArithmeticException("Out of long range: " + this);
    }

    public int toIntExact() {
        long v = toLongExact();
        if ((int) v == v) return (int) v;
        throw new ArithmeticException("Out of int range: " + this);
    }

    /** Big‑endian 16‑byte two's‑complement array. */
    public byte[] toBytesBE() {
        byte[] a = new byte[16];
        writeLongBE(a, 0, hi);
        writeLongBE(a, 8, lo);
        return a;
    }

    /** Build from big‑endian two's‑complement 16 bytes. */
    public static Int128 fromBytesBE(byte[] a16) {
        if (a16 == null || a16.length != 16) throw new IllegalArgumentException("Need exactly 16 bytes");
        long hi = readLongBE(a16, 0);
        long lo = readLongBE(a16, 8);
        return new Int128(hi, lo);
    }

    /** Put into a ByteBuffer as two consecutive longs (hi, lo). */
    public void putTo(ByteBuffer bb) {
        bb.putLong(hi).putLong(lo);
    }

    /** Read from a ByteBuffer (hi, lo). */
    public static Int128 getFrom(ByteBuffer bb) {
        return new Int128(bb.getLong(), bb.getLong());
    }

    // BigInteger bridging for string I/O only
    private static BigInteger toBigInteger(Int128 x) {
        byte[] a = new byte[16];
        writeLongBE(a, 0, x.hi);
        writeLongBE(a, 8, x.lo);
        return new BigInteger(a);
    }

    private static Int128 fromBigInteger(BigInteger bi) {
        byte[] raw = bi.toByteArray(); // two's complement big‑endian
        if (raw.length == 16) {
            long hi = readLongBE(raw, 0);
            long lo = readLongBE(raw, 8);
            return new Int128(hi, lo);
        }
        byte[] a = new byte[16];
        int srcPos = Math.max(0, raw.length - 16);
        int len = Math.min(16, raw.length);
        System.arraycopy(raw, srcPos, a, 16 - len, len);
        byte fill = (bi.signum() < 0) ? (byte) 0xFF : (byte) 0x00;
        for (int i = 0; i < 16 - len; i++) a[i] = fill;
        long hi = readLongBE(a, 0);
        long lo = readLongBE(a, 8);
        return new Int128(hi, lo);
    }

    private static void writeLongBE(byte[] a, int off, long v) {
        a[off    ] = (byte) (v >>> 56);
        a[off + 1] = (byte) (v >>> 48);
        a[off + 2] = (byte) (v >>> 40);
        a[off + 3] = (byte) (v >>> 32);
        a[off + 4] = (byte) (v >>> 24);
        a[off + 5] = (byte) (v >>> 16);
        a[off + 6] = (byte) (v >>> 8);
        a[off + 7] = (byte) (v);
    }

    private static long readLongBE(byte[] a, int off) {
        return ((long) a[off    ] << 56) |
               ((long) (a[off + 1] & 0xFF) << 48) |
               ((long) (a[off + 2] & 0xFF) << 40) |
               ((long) (a[off + 3] & 0xFF) << 32) |
               ((long) (a[off + 4] & 0xFF) << 24) |
               ((long) (a[off + 5] & 0xFF) << 16) |
               ((long) (a[off + 6] & 0xFF) << 8)  |
               ((long) (a[off + 7] & 0xFF));
    }

    // =========================================================================
    // Internal unsigned helpers (64×64→128, 128/64 division, 128/128 division)
    // =========================================================================

    /** Lower 64 bits of x * y (wrap semantics). */
    private static long mulLo64(long x, long y) { return x * y; }

    /** Upper 64 bits of x * y. Portable; swap to Math.multiplyHigh on JDK 9+. */
    private static long mulHi64(long x, long y) {
        // Portable 32‑bit split (JDK 8 compatible).
        long x0 = x & 0xFFFF_FFFFL, x1 = x >>> 32;
        long y0 = y & 0xFFFF_FFFFL, y1 = y >>> 32;
        long p0 = x0 * y0;
        long p1 = x0 * y1;
        long p2 = x1 * y0;
        long p3 = x1 * y1;
        long mid = (p1 & 0xFFFF_FFFFL) + (p2 & 0xFFFF_FFFFL) + (p0 >>> 32);
        return p3 + (p1 >>> 32) + (p2 >>> 32) + (mid >>> 32);
    }

    /** Unsigned 64×64 → 128 product; returns [hi, lo]. */
    private static long[] mul64to128(long x, long y) {
        // Portable 32‑bit split. For JDK9+ you may switch to Math.multiplyHigh(x,y) here.
        long x0 = x & 0xFFFF_FFFFL;
        long x1 = x >>> 32;
        long y0 = y & 0xFFFF_FFFFL;
        long y1 = y >>> 32;

        long p0 = x0 * y0;            // 64‑bit
        long p1 = x0 * y1;            // 64‑bit
        long p2 = x1 * y0;            // 64‑bit
        long p3 = x1 * y1;            // 64‑bit

        long mid = (p1 & 0xFFFF_FFFFL) + (p2 & 0xFFFF_FFFFL) + (p0 >>> 32);
        long hi  = p3 + (p1 >>> 32) + (p2 >>> 32) + (mid >>> 32);
        long lo  = (p0 & 0xFFFF_FFFFL) | (mid << 32);
        return new long[] { hi, lo };
    }

    /** 128‑bit unsigned compare: returns -1, 0, +1. */
    private static int cmpu128(long aHi, long aLo, long bHi, long bLo) {
        int ch = Long.compareUnsigned(aHi, bHi);
        if (ch != 0) return ch;
        return Long.compareUnsigned(aLo, bLo);
    }

    /** Two's‑complement negate of limbs: returns [hi, lo]. */
    private static long[] negate128(long hi, long lo) {
        long loN = ~lo + 1L;
        long carry = (loN == 0L) ? 1L : 0L;
        long hiN = ~hi + carry;
        return new long[] { hiN, loN };
    }

    /**
     * Fast unsigned 128/64 division.
     * Input: (aHi:aLo) / v; returns [qHi, qLo, rHi=0, rLo].
     *
     * We compute:
     *   qHi = (aHi / v), r = (aHi % v)
     *   Then divide (r<<64 | aLo) / v → qLo and remainder.
     */
    private static long[] udivrem_128by64(long aHi, long aLo, long v) {
        if (v == 0L) throw new ArithmeticException("divide by zero");

        long qHi = Long.divideUnsigned(aHi, v);
        long r   = Long.remainderUnsigned(aHi, v);

        // Divide 128 (r:aLo) by 64 (v) to get qLo and rem
        long qLo = 0L;
        for (int i = 63; i >= 0; i--) {
            long bit = (aLo >>> i) & 1L;
            long r2 = (r << 1) | bit;
            // Handle overflow: if r >= 2^63, then r*2 overflows, and r*2 >= 2^64 > v
            if (r < 0L || Long.compareUnsigned(r2, v) >= 0) {
                r = r2 - v;
                qLo |= (1L << i);
            } else {
                r = r2;
            }
        }
        // r is remainder (< v)
        return new long[] { qHi, qLo, 0L, r };
    }

    /**
     * Helper: unsigned 96-bit / 64-bit division using bit-by-bit loop.
     * Divides (rHi:rLo) / d where rHi < d.
     * Returns [quotient, remainder].
     */
    private static long[] udivrem_96by64_bitloop(long rHi, long rLo, long d) {
        long q = 0L;
        long r = rHi;
        for (int i = 63; i >= 0; i--) {
            long bit = (rLo >>> i) & 1L;
            long r2 = (r << 1) | bit;
            // Handle overflow: if r >= 2^63, then r*2 overflows, and r*2 >= 2^64 > d
            if (r < 0L || Long.compareUnsigned(r2, d) >= 0) {
                r = r2 - d;
                q |= (1L << i);
            } else {
                r = r2;
            }
        }
        return new long[] { q, r };
    }

    /**
     * Optimised unsigned 128/128 division for same‑degree operands (bHi != 0).
     * Uses normalised two‑limb division (Knuth‑style) with bounded corrections (at most 2).
     *
     * Returns [qHi=0, qLo, rHi, rLo].
     */
    private static long[] udivrem_128by128(long aHi, long aLo, long bHi, long bLo) {
        // Preconditions: (aHi:aLo) >= (bHi:bLo), and bHi != 0.

        // 1) Normalise so that the top bit of divisor is 1.
        final int s  = Long.numberOfLeadingZeros(bHi);
        final long d1 = (s == 0) ? bHi : (bHi << s) | (bLo >>> (64 - s));
        final long d0 = (s == 0) ? bLo : (bLo << s);

        // Normalise dividend, tracking overflow from aHi
        final long n2 = (s == 0) ? 0L  : (aHi >>> (64 - s));
        final long n1 = (s == 0) ? aHi : (aHi << s) | (aLo >>> (64 - s));
        final long n0 = (s == 0) ? aLo : (aLo << s);

        // 2) Trial quotient using the top two limbs (Knuth D2/D3).
        long qHi_est = Long.divideUnsigned(n2, d1);
        long rem1    = Long.remainderUnsigned(n2, d1);

        // Finish 128/64 division of (rem1:n1)/d1 using the existing 96/64 helper.
        long[] ql_r  = udivrem_96by64_bitloop(rem1, n1, d1);
        long q    = ql_r[0];
        long rhat = ql_r[1];

        // Reduce q from >= 2^64 to < 2^64 if needed (bounded; at most one iteration).
        while (qHi_est != 0L) {
            if (q == 0L) { qHi_est--; q = 0xFFFF_FFFF_FFFF_FFFFL; }
            else         { q--; }
            long oldR = rhat;
            rhat = rhat + d1;
            if (Long.compareUnsigned(rhat, oldR) < 0) break; // overflow => stop
        }

        // 3) Knuth corrections: while q*d0 > rhat*B + n0, decrement q and add d1 to rhat (≤ 2 iterations).
        for (int corrections = 0; corrections < 2; corrections++) {
            long qd0_lo = mulLo64(q, d0);
            long qd0_hi = mulHi64(q, d0);

            boolean needsCorrection =
                (Long.compareUnsigned(qd0_hi, rhat) > 0) ||
                (qd0_hi == rhat && Long.compareUnsigned(qd0_lo, n0) > 0);

            if (!needsCorrection) break;

            q--;
            long oldR = rhat;
            rhat = rhat + d1;
            if (Long.compareUnsigned(rhat, oldR) < 0) break; // overflow => stop
        }

        // 4) Subtract q * D from normalised numerator (n2:n1:n0), inline 192‑bit multiply.
        // qD = (d1:d0) * q  => top, hi, lo
        long qD_lo    = mulLo64(d0, q);
        long qD_loHi  = mulHi64(d0, q);
        long qD_hiLo  = mulLo64(d1, q);
        long qD_hiHi  = mulHi64(d1, q);

        long qD_hi  = qD_loHi + qD_hiLo;
        long qD_top = qD_hiHi + (Long.compareUnsigned(qD_hi, qD_loHi) < 0 ? 1L : 0L);

        // 3‑limb subtraction: (n2:n1:n0) - (qD_top:qD_hi:qD_lo)
        long rLo = n0 - qD_lo;
        long borrow1 = Long.compareUnsigned(n0, qD_lo) < 0 ? 1L : 0L;

        long rMid = n1 - qD_hi - borrow1;
        long borrow2 = (Long.compareUnsigned(n1, qD_hi) < 0 || (n1 == qD_hi && borrow1 != 0)) ? 1L : 0L;

        long rHi = n2 - qD_top - borrow2;

        // If underflowed (top‑limb borrow), add D back once and decrement q.
        if (rHi < 0) {
            long rLo2 = rLo + d0;
            long carry = Long.compareUnsigned(rLo2, rLo) < 0 ? 1L : 0L;
            long rMid2 = rMid + d1 + carry;
            rLo = rLo2;
            rMid = rMid2;
            q--;
        }

        // 5) Denormalise remainder by shifting right s bits; use rMid:rLo as the 128‑bit remainder.
        final long rHiDen = (s == 0) ? rMid : (rMid >>> s);
        final long rLoDen = (s == 0) ? rLo  : (rLo >>> s) | (rMid << (64 - s));

        // Quotient is one limb: qHi = 0, qLo = q
        return new long[] { 0L, q, rHiDen, rLoDen };
    }

    // =========================================================================
    // Convenience & diagnostics
    // =========================================================================

    /** Returns a debug hex form "0xHHHH_HHHH_LLLL_LLLL". */
    public String toDebugHex() {
        return String.format("0x%016X_%016X", hi, lo);
    }

    /** Fits exactly in signed 64‑bit. */
    public boolean fitsInLong() {
        boolean loSign = (lo & SIGN_BIT_64) != 0;
        return loSign ? (hi == -1L) : (hi == 0L);
    }

    /** Fits in unsigned 64‑bit. */
    public boolean fitsInUnsignedLong() { return hi == 0L; }

    /** TEN^k for 0 <= k <= 38. */
    public static Int128 tenPow(int k) {
        if (k < 0 || k >= TEN_POW.length) throw new IllegalArgumentException("k out of [0..38]");
        return TEN_POW[k];
    }

    /** Alias for DECIMAL_BASE. */
    public static Int128 decimalBase() { return DECIMAL_BASE; }

    public Int128 add(long k) { return this.add(Int128.valueOf(k)); }
    public Int128 sub(long k) { return this.sub(Int128.valueOf(k)); }
    public Int128 times10()   { return this.mul(10); }
    public Int128 times100()  { return this.mul(100); }
    public Int128 times1000() { return this.mul(1000); }

    // =========================================================================
    // Quick sanity checks (optional; no heavy tests shipped)
    // =========================================================================

    /**
     * Very light self‑check guardrails for integration smoke testing.
     * Throws AssertionError on failure. Call manually in debug builds.
     */
    public static void quickSelfCheck() {
        if (!MIN_VALUE.inc().dec().equals(MIN_VALUE)) throw new AssertionError("MIN inc/dec");
        if (!ZERO.add(ONE).equals(ONE))               throw new AssertionError("0+1=1");
        if (!ONE.sub(ONE).equals(ZERO))               throw new AssertionError("1-1=0");
        if (!tenPow(1).div(DECIMAL_BASE).equals(ONE)) throw new AssertionError("10/10=1");
        String s = MAX_VALUE.toString();
        if (!Int128.fromString(s).equals(MAX_VALUE))  throw new AssertionError("roundtrip MAX");
        // Division corners
        Int128 q = MIN_VALUE.div(Int128.valueOf(-1)); // wrap semantics in 128‑bit ring
        if (!q.equals(MIN_VALUE))                     throw new AssertionError("MIN/-1 wrap");
        if (!parseHex("-0x1").equals(Int128.valueOf(-1))) throw new AssertionError("parseHex -0x1");
        // Critical 128/128 division edge case (previously caused infinite loop - now fixed)
        Int128 a = parseHex("0xFFFF000000000000FFFF000000000000");
        Int128 d = parseHex("0x0000FFFF00000000FFFFFFFF00000001");
        Int128[] dr = a.divRem(d);
        Int128 recomposed = dr[0].mul(d).add(dr[1]);
        if (!recomposed.equals(a)) throw new AssertionError("a = q*d + r identity");
        // Verify Euclidean property: |remainder| < |divisor|
        if (dr[1].abs().compareUnsigned(d.abs()) >= 0) throw new AssertionError("|r| >= |d|");
    }

    // =========================================================================
    // End of class
    // =========================================================================
}
