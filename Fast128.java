import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Objects;
/**
* Int128 — high-performance, exact, signed 128-bit two's-complement integer.
*
* Key properties:
*  - Immutable & thread-safe.
*  - Core arithmetic (add/sub/mul/div/rem/shifts/bitwise) without BigInteger.
*  - String conversion (toString/fromString) may use BigInteger ONLY there.
*  - Division: fast path for 128/64; constant-iteration 128-step fallback.
*
* Representation: (hi, lo) 64-bit limbs, value = (hi << 64) + (lo unsigned).
*
* Notes for financial usage:
*  - Prefer scaling/rounding via the provided helpers that leverage fast 128/64 division
*    for powers of 10 up to 10^19, common in money code (scale 0..19).
*  - All operations are exact in 128-bit two's complement with wrap semantics where applicable.
*/
public final class Int128 implements Comparable<Int128>, Serializable {
   // ------------------------------------------------------------
   // region: fields & constants
   // ------------------------------------------------------------
   private static final long serialVersionUID = 1L;
   private final long hi;
   private final long lo;
   private static final long MASK_64     = 0xFFFF_FFFF_FFFF_FFFFL;
   private static final long SIGN_BIT_64 = 0x8000_0000_0000_0000L;
   // Precomputed powers of ten Int128[0..38].
   private static final Int128[] TEN_POW;     // up to 10^38
   // Also keep native 64-bit powers for fast 128/64 division when exp<=19.
   private static final long[] TEN_POW_64;    // up to 10^19
   /** ZERO constant. */
   public static final Int128 ZERO = new Int128(0L, 0L);
   /** ONE constant. */
   public static final Int128 ONE  = new Int128(0L, 1L);
   /** TEN (decimal base). */
   public static final Int128 DECIMAL_BASE = Int128.valueOf(10);
   /** Signed min & max (two's complement). */
   public static final Int128 MIN_VALUE = new Int128(Long.MIN_VALUE, 0L);
   public static final Int128 MAX_VALUE = new Int128(Long.MAX_VALUE, MASK_64);
   // BigInteger bounds for robust parsing/printing only.
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
           if (i < TEN_POW_64.length - 1) acc = Math.multiplyExact(acc, 10L);
       }
       BIG_MIN = toBigInteger(MIN_VALUE);
       BIG_MAX = toBigInteger(MAX_VALUE);
   }
   // ------------------------------------------------------------
   // region: construction
   // ------------------------------------------------------------
   private Int128(long hi, long lo) {
       this.hi = hi;
       this.lo = lo;
   }
   /** Build from hi/lo limbs (no normalisation). */
   public static Int128 of(long hi, long lo) { return new Int128(hi, lo); }
   /** From signed long (sign-extended). */
   public static Int128 valueOf(long v) { return new Int128(v < 0 ? -1L : 0L, v); }
   /** From signed int (sign-extended). */
   public static Int128 valueOf(int v)  { return new Int128(v < 0 ? -1L : 0L, (long) v); }
   /** From unsigned long (upper limb zeroed). */
   public static Int128 fromUnsignedLong(long unsigned) { return new Int128(0L, unsigned); }
   // ------------------------------------------------------------
   // region: comparisons, predicates
   // ------------------------------------------------------------
   /** Signed comparison. */
   @Override
   public int compareTo(Int128 o) {
       if (this.hi != o.hi) return (this.hi < o.hi) ? -1 : 1;
       int cu = Long.compareUnsigned(this.lo, o.lo);
       return (cu < 0) ? -1 : (cu > 0 ? 1 : 0);
   }
   /** Unsigned comparison (128-bit). */
   public int compareUnsigned(Int128 o) {
       int ch = Long.compareUnsigned(this.hi, o.hi);
       if (ch != 0) return ch;
       return Long.compareUnsigned(this.lo, o.lo);
   }
   /** True if zero. */
   public boolean isZero()     { return hi == 0L && lo == 0L; }
   /** True if negative. */
   public boolean isNegative() { return hi < 0L; }
   /** True if positive (>0). */
   public boolean isPositive() { return hi > 0L || (hi == 0L && lo != 0L); }
   /** Signum: -1, 0, +1. */
   public int signum() {
       if (hi == 0L && lo == 0L) return 0;
       return (hi < 0L) ? -1 : +1;
   }
   /** Power of two (strictly positive). */
   public boolean isPowerOfTwo() {
       if (isNegative() || isZero()) return false;
       return (Long.bitCount(hi) + Long.bitCount(lo)) == 1;
   }
   // ------------------------------------------------------------
   // region: equals, hashCode, strings
   // ------------------------------------------------------------
   @Override
   public boolean equals(Object obj) {
       if (this == obj) return true;
       if (!(obj instanceof Int128)) return false;
       Int128 o = (Int128) obj;
       return this.hi == o.hi && this.lo == o.lo;
   }
   @Override
   public int hashCode() {
       long h = (hi * 0x9E3779B97F4A7C15L) ^ (lo + 0xC2B2AE3D27D4EB4FL);
       return (int) (h ^ (h >>> 32));
   }
   /** Decimal toString via BigInteger for conversion only (not used in arithmetic). */
   @Override
   public String toString() { return toBigInteger(this).toString(10); }
   /** Radix toString via BigInteger (conversion only). */
   public String toString(int radix) { return toBigInteger(this).toString(radix); }
   /** Hex (lower-case), minimal digits, no prefix. */
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
   /** Parse decimal string. */
   public static Int128 fromString(String s) { return fromString(s, 10); }
   /** Parse string with radix 2..36 (checks range). */
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
   /** Parse hex with optional sign and optional "0x"/"0X" prefix (e.g., "-0xFF"). */
   public static Int128 parseHex(String s) {
       if (s == null) throw new NullPointerException("s");
       String t = s.trim();
       if (t.isEmpty()) throw new NumberFormatException("Empty hex string");
       boolean neg = false;
       if (t.charAt(0) == '+' || t.charAt(0) == '-') {
           neg = (t.charAt(0) == '-');
           t = t.substring(1).trim();
       }
       if (t.startsWith("0x") || t.startsWith("0X")) t = t.substring(2);
       if (t.isEmpty()) throw new NumberFormatException("Empty hex digits");
       // Max 32 hex digits (unsigned 128).
       if (t.length() > 32) throw new NumberFormatException("Hex literal exceeds 128 bits: " + s);
       // Split into hi/lo parts
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
   // ------------------------------------------------------------
   // region: basic arithmetic
   // ------------------------------------------------------------
   public Int128 add(Int128 b) {
       long loSum = this.lo + b.lo;
       long carry = Long.compareUnsigned(loSum, this.lo) < 0 ? 1L : 0L;
       long hiSum = this.hi + b.hi + carry;
       return new Int128(hiSum, loSum);
   }
   public Int128 sub(Int128 b) {
       long loDiff = this.lo - b.lo;
       long borrow = Long.compareUnsigned(this.lo, b.lo) < 0 ? 1L : 0L;
       long hiDiff = this.hi - b.hi - borrow;
       return new Int128(hiDiff, loDiff);
   }
   public Int128 negate() {
       long loN = ~this.lo + 1L;
       long carry = (loN == 0L) ? 1L : 0L;
       long hiN = ~this.hi + carry;
       return new Int128(hiN, loN);
   }
   public Int128 abs() { return isNegative() ? negate() : this; }
   public Int128 inc() { return add(ONE); }
   public Int128 dec() { return sub(ONE); }
   // ------------------------------------------------------------
   // region: multiplication (mod 2^128)
   // ------------------------------------------------------------
   /**
    * 128x128 -> low 128 bits (wrap semantics).
    * Uses 64x64->128 and cross-terms; upper 128 bits are discarded by design.
    */
   public Int128 mul(Int128 b) {
       final long aLo = this.lo, aHi = this.hi;
       final long bLo = b.lo,   bHi = b.hi;
       long[] p0 = mul64to128(aLo, bLo);  // aLo*bLo
       long lo0 = p0[1];
       long hi0 = p0[0];
       long cross1 = aLo * bHi; // low limb of (aLo*bHi) << 64
       long cross2 = aHi * bLo; // low limb of (aHi*bLo) << 64
       long lo = lo0;
       long hi = hi0 + cross1 + cross2;
       return new Int128(hi, lo);
   }
   /** Multiply by signed long (wrap semantics). */
   public Int128 mul(long k) {
       if (k == 0)  return ZERO;
       if (k == 1)  return this;
       if (k == -1) return this.negate();
       long[] p0 = mul64to128(this.lo, k);
       long lo0 = p0[1];
       long hi0 = p0[0];
       long cross = this.hi * k; // contributes to high limb (<<64)
       long lo = lo0;
       long hi = hi0 + cross;
       return new Int128(hi, lo);
   }
   /** Scale by 10^exp, 0..38 (wrap on overflow). */
   public Int128 scaleUpPow10(int exp) {
       if (exp < 0 || exp >= TEN_POW.length) throw new IllegalArgumentException("exp out of [0..38]");
       return this.mul(TEN_POW[exp]);
   }
   // ------------------------------------------------------------
   // region: division & remainder
   // ------------------------------------------------------------
   /** Signed division (wrap semantics; throws on /0). */
   public Int128 div(Int128 d) { return divRem(d)[0]; }
   /** Signed remainder (throws on /0). */
   public Int128 rem(Int128 d) { return divRem(d)[1]; }
   /**
    * Signed div/rem: uses unsigned arithmetic internally.
    * Fast path for divisor that fits in 64 bits, else uses 128-step fallback.
    */
   public Int128[] divRem(Int128 divisor) {
       if (divisor.isZero()) throw new ArithmeticException("divide by zero");
       boolean negA = this.isNegative();
       boolean negB = divisor.isNegative();
       boolean negQ = negA ^ negB; // quotient sign
       boolean negR = negA;        // remainder sign follows dividend
       long aHi = this.hi, aLo = this.lo;
       long bHi = divisor.hi, bLo = divisor.lo;
       // Take unsigned "absolute values" (two's complement magnitude). For MIN_VALUE, negation is itself.
       if (negA) { long[] t = negate128(aHi, aLo); aHi = t[0]; aLo = t[1]; }
       if (negB) { long[] t = negate128(bHi, bLo); bHi = t[0]; bLo = t[1]; }
       long qHi, qLo, rHi, rLo;
       if (bHi == 0L) {
           // ---- Fast path: 128 / 64 ----
           long v = bLo; // divisor (unsigned 64)
           if (v == 0L) throw new ArithmeticException("divide by zero"); // defensive
           // qHigh = aHi / v; r = aHi % v
           long qHigh = Long.divideUnsigned(aHi, v);
           long r = Long.remainderUnsigned(aHi, v);
           // Now divide (r<<64 | aLo) by v to get qLow (64 bits) and remainder r.
           long[] ql_r = udivrem_96by64_bitloop(r, aLo, v); // 64 steps max
           long qLow = ql_r[0];
           long rem  = ql_r[1];
           qHi = qHigh;
           qLo = qLow;
           rHi = 0L;
           rLo = rem;
       } else {
           // ---- General path: 128-step restoring division (unsigned) ----
           long[] qr = udivrem128_bitloop(aHi, aLo, bHi, bLo);
           qHi = qr[0]; qLo = qr[1]; rHi = qr[2]; rLo = qr[3];
       }
       // Apply signs
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
       long[] qr = (d.hi == 0L)
               ? udivrem_128by64(this.hi, this.lo, d.lo)
               : udivrem128_bitloop(this.hi, this.lo, d.hi, d.lo);
       return new Int128(qr[0], qr[1]);
   }
   /** Unsigned remainder only. */
   public Int128 remainderUnsigned(Int128 d) {
       if (d.isZero()) throw new ArithmeticException("divide by zero");
       long[] qr = (d.hi == 0L)
               ? udivrem_128by64(this.hi, this.lo, d.lo)
               : udivrem128_bitloop(this.hi, this.lo, d.hi, d.lo);
       return new Int128(qr[2], qr[3]);
   }
   // ------------------------------------------------------------
   // region: shifts
   // ------------------------------------------------------------
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
   /** Arithmetic right shift. */
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
   /** Logical right shift. */
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
   // ------------------------------------------------------------
   // region: bitwise
   // ------------------------------------------------------------
   public Int128 and(Int128 b) { return new Int128(this.hi & b.hi, this.lo & b.lo); }
   public Int128 or (Int128 b) { return new Int128(this.hi | b.hi, this.lo | b.lo); }
   public Int128 xor(Int128 b) { return new Int128(this.hi ^ b.hi, this.lo ^ b.lo); }
   public Int128 not()         { return new Int128(~this.hi, ~this.lo); }
   public boolean testBit(int bit) {
       if (bit < 0 || bit > 127) throw new IllegalArgumentException("bit out of [0..127]");
       if (bit < 64) return ((this.lo >>> bit) & 1L) != 0L;
       int b = bit - 64;
       return ((this.hi >>> b) & 1L) != 0L;
   }
   public Int128 setBit(int bit) {
       if (bit < 0 || bit > 127) throw new IllegalArgumentException("bit out of [0..127]");
       if (bit < 64) return new Int128(this.hi, this.lo | (1L << bit));
       int b = bit - 64;
       return new Int128(this.hi | (1L << b), this.lo);
   }
   public Int128 clearBit(int bit) {
       if (bit < 0 || bit > 127) throw new IllegalArgumentException("bit out of [0..127]");
       if (bit < 64) return new Int128(this.hi, this.lo & ~(1L << bit));
       int b = bit - 64;
       return new Int128(this.hi & ~(1L << b), this.lo);
   }
   /**
    * Two's-complement bit length: 128 minus the count of leading sign bits.
    * Returns 0 for 0 and -1, 127 for MIN_VALUE, etc.
    */
   public int bitLength() {
       if (hi == 0L && lo == 0L) return 0;
       if (hi >= 0L) {
           if (hi != 0L) return 64 + (64 - Long.numberOfLeadingZeros(hi));
           return 64 - Long.numberOfLeadingZeros(lo);
       } else {
           // count leading ones across 128 bits
           int leadOnesHi = Long.numberOfLeadingZeros(~hi);
           if (leadOnesHi < 64) {
               return 128 - leadOnesHi;
           } else {
               int leadOnesLo = Long.numberOfLeadingZeros(~lo);
               return 64 - leadOnesLo;
           }
       }
   }
   // ------------------------------------------------------------
   // region: min/max, clamp
   // ------------------------------------------------------------
   public static Int128 min(Int128 a, Int128 b) { return (a.compareTo(b) <= 0) ? a : b; }
   public static Int128 max(Int128 a, Int128 b) { return (a.compareTo(b) >= 0) ? a : b; }
   public static Int128 clamp(Int128 x, Int128 lo, Int128 hi) {
       if (lo.compareTo(hi) > 0) throw new IllegalArgumentException("lo > hi");
       return max(lo, min(x, hi));
   }
   // ------------------------------------------------------------
   // region: financial helpers
   // ------------------------------------------------------------
   /** Divide by 10^exp, 0..38, returning [quotient, remainder]. Fast path for exp<=19. */
   public Int128[] divRemPow10(int exp) {
       if (exp < 0 || exp >= TEN_POW.length) throw new IllegalArgumentException("exp out of [0..38]");
       if (exp <= 19) {
           long d = TEN_POW_64[exp];
           long[] qr = udivrem_128by64(this.hi, this.lo, d);
           return new Int128[] { new Int128(qr[0], qr[1]), new Int128(qr[2], qr[3]) };
       } else {
           Int128 d = TEN_POW[exp];
           return this.divRem(d);
       }
   }
   /** Banker’s rounding (half-even) for /10^exp. */
   public Int128 divRoundHalfEvenPow10(int exp) {
       Int128[] dr = divRemPow10(exp);
       Int128 q = dr[0], r = dr[1].abs();
       Int128 d = (exp <= 19) ? fromUnsignedLong(TEN_POW_64[exp]) : TEN_POW[exp];
       Int128 twiceR = r.shiftLeft(1);
       int cmp = twiceR.compareTo(d);
       if (cmp < 0) return q;
       if (cmp > 0) return q.add(ONE.withSignOf(this));
       // exactly half
       boolean even = (q.lo & 1L) == 0L;
       return even ? q : q.add(ONE.withSignOf(this));
   }
   public Int128 floorDivPow10(int exp) {
       Int128[] dr = divRemPow10(exp);
       if (this.isNegative() && !dr[1].isZero()) return dr[0].sub(ONE);
       return dr[0];
   }
   public Int128 ceilDivPow10(int exp) {
       Int128[] dr = divRemPow10(exp);
       if (this.isPositive() && !dr[1].isZero()) return dr[0].add(ONE);
       return dr[0];
   }
   private Int128 withSignOf(Int128 other) {
       if (other.isZero()) return ZERO;
       return other.isNegative() ? this.negate() : this;
   }
   // ------------------------------------------------------------
   // region: conversions
   // ------------------------------------------------------------
   public long toLong() { return lo; }
   /** Exact narrowing to long; throws if out of range. */
   public long toLongExact() {
       // fits in 64 bits if hi==0 (non-negative) or hi==-1 && top bit set (negative).
       if (hi == 0L) return lo;
       if (hi == -1L && (lo & SIGN_BIT_64) != 0) return lo;
       throw new ArithmeticException("Out of long range: " + this);
   }
   public int toIntExact() {
       long v = toLongExact();
       if ((int) v == v) return (int) v;
       throw new ArithmeticException("Out of int range: " + this);
   }
   public byte[] toBytesBE() {
       byte[] a = new byte[16];
       writeLongBE(a, 0, hi);
       writeLongBE(a, 8, lo);
       return a;
   }
   public static Int128 fromBytesBE(byte[] a16) {
       if (a16 == null || a16.length != 16) throw new IllegalArgumentException("Need exactly 16 bytes");
       long hi = readLongBE(a16, 0);
       long lo = readLongBE(a16, 8);
       return new Int128(hi, lo);
   }
   public void putTo(ByteBuffer bb) { bb.putLong(hi).putLong(lo); }
   public static Int128 getFrom(ByteBuffer bb) { return new Int128(bb.getLong(), bb.getLong()); }
   // ------------------------------------------------------------
   // region: utilities (BigInteger bridge for string I/O only)
   // ------------------------------------------------------------
   private static BigInteger toBigInteger(Int128 x) {
       byte[] a = new byte[16];
       writeLongBE(a, 0, x.hi);
       writeLongBE(a, 8, x.lo);
       return new BigInteger(a);
   }
   private static Int128 fromBigInteger(BigInteger bi) {
       byte[] raw = bi.toByteArray();
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
   // ------------------------------------------------------------
   // region: internal 64x64->128 multiply & helpers
   // ------------------------------------------------------------
   /** Unsigned 64x64 -> 128 product, returned as [hi, lo]. */
   private static long[] mul64to128(long x, long y) {
       long x0 = x & 0xFFFF_FFFFL;
       long x1 = x >>> 32;
       long y0 = y & 0xFFFF_FFFFL;
       long y1 = y >>> 32;
       long p0 = x0 * y0;            // 64-bit
       long p1 = x0 * y1;            // 64-bit
       long p2 = x1 * y0;            // 64-bit
       long p3 = x1 * y1;            // 64-bit
       long mid = (p1 & 0xFFFF_FFFFL) + (p2 & 0xFFFF_FFFFL) + (p0 >>> 32);
       long hi  = p3 + (p1 >>> 32) + (p2 >>> 32) + (mid >>> 32);
       long lo  = (p0 & 0xFFFF_FFFFL) | (mid << 32);
       return new long[] { hi, lo };
   }
   private static long[] negate128(long hi, long lo) {
       long loN = ~lo + 1L;
       long carry = (loN == 0L) ? 1L : 0L;
       long hiN = ~hi + carry;
       return new long[] { hiN, loN };
   }
   private static int cmpu128(long aHi, long aLo, long bHi, long bLo) {
       int ch = Long.compareUnsigned(aHi, bHi);
       if (ch != 0) return ch;
       return Long.compareUnsigned(aLo, bLo);
   }
   // ------------------------------------------------------------
   // region: division helpers (fast 128/64; generic 128-step)
   // ------------------------------------------------------------
   /**
    * Fast unsigned 128 / 64 division using a tight 64-iteration bit loop for the low limb.
    * Input: (aHi:aLo) / v ; returns [qHi, qLo, rHi=0, rLo].
    */
   private static long[] udivrem_128by64(long aHi, long aLo, long v) {
       if (v == 0L) throw new ArithmeticException("divide by zero");
       long qHi = Long.divideUnsigned(aHi, v);
       long r   = Long.remainderUnsigned(aHi, v);
       // Now divide (r<<64 | aLo) by v, producing 64-bit qLo and remainder r
       long[] ql_r = udivrem_96by64_bitloop(r, aLo, v);
       long qLo = ql_r[0];
       long rem = ql_r[1];
       return new long[] { qHi, qLo, 0L, rem };
   }
   /**
    * Divide 96-bit numerator (r:64 | aLo:64) by 64-bit v.
    * Returns [qLo(64), rem(64)]. Implementation: 64-step restoring divider on the low limb only.
    */
   private static long[] udivrem_96by64_bitloop(long r, long aLo, long v) {
       long q = 0L;
       for (int i = 63; i >= 0; i--) {
           // r = (r << 1) | bit_i(aLo)
           long bit = (aLo >>> i) & 1L;
           r = (r << 1) | bit;
           // if r >= v: r -= v; set q_i
           if (Long.compareUnsigned(r, v) >= 0) {
               r -= v;
               q |= (1L << i);
           }
       }
       return new long[] { q, r };
   }
   /**
    * Generic unsigned division: (aHi:aLo) / (bHi:bLo) -> [qHi, qLo, rHi, rLo]
    * 128 iterations, constant control flow. Kept as robust fallback when divisor >= 2^64.
    */
   private static long[] udivrem128_bitloop(long aHi, long aLo, long bHi, long bLo) {
       if (bHi == 0L && bLo == 0L) throw new ArithmeticException("divide by zero");
       long qHi = 0L, qLo = 0L;
       long rHi = 0L, rLo = 0L;
       for (int i = 127; i >= 0; i--) {
           // r <<= 1; bring bit i of numerator
           long bit = (i >= 64) ? ((aHi >>> (i - 64)) & 1L) : ((aLo >>> i) & 1L);
           // r = (r << 1) | bit
           long newLo = (rLo << 1) | bit;
           long newHi = (rHi << 1) | ((rLo >>> 63) & 1L);
           rHi = newHi; rLo = newLo;
           // if r >= b: r -= b; set q_i
           if (cmpu128(rHi, rLo, bHi, bLo) >= 0) {
               long lo = rLo - bLo;
               long borrow = Long.compareUnsigned(rLo, bLo) < 0 ? 1L : 0L;
               long hi = rHi - bHi - borrow;
               rHi = hi; rLo = lo;
               if (i >= 64) qHi |= (1L << (i - 64));
               else         qLo |= (1L << i);
           }
       }
       return new long[] { qHi, qLo, rHi, rLo };
   }
   // ------------------------------------------------------------
   // region: convenience & extras
   // ------------------------------------------------------------
   public Int128 times10()   { return this.mul(10); }
   public Int128 times100()  { return this.mul(100); }
   public Int128 times1000() { return this.mul(1000); }
   public Int128 add(long k) { return this.add(Int128.valueOf(k)); }
   public Int128 sub(long k) { return this.sub(Int128.valueOf(k)); }
   /** Debug helper. */
   public String toDebugHex() { return String.format("0x%016X_%016X", hi, lo); }
   /** Fits in signed 64-bit exactly. */
   public boolean fitsInLong() { return hi == 0L || (hi == -1L && (lo & SIGN_BIT_64) != 0); }
   /** Fits in unsigned 64-bit. */
   public boolean fitsInUnsignedLong() { return hi == 0L; }
   /** TEN^k for 0<=k<=38. */
   public static Int128 tenPow(int k) {
       if (k < 0 || k >= TEN_POW.length) throw new IllegalArgumentException("k out of [0..38]");
       return TEN_POW[k];
   }
   // ------------------------------------------------------------
   // region: quick self-checks (very light-touch)
   // ------------------------------------------------------------
   public static void quickSelfCheck() {
       if (!MIN_VALUE.inc().dec().equals(MIN_VALUE)) throw new AssertionError("MIN inc/dec");
       if (!ZERO.add(ONE).equals(ONE))               throw new AssertionError("0+1=1");
       if (!ONE.sub(ONE).equals(ZERO))               throw new AssertionError("1-1=0");
       if (!tenPow(1).div(DECIMAL_BASE).equals(ONE)) throw new AssertionError("10/10=1");
       String s = MAX_VALUE.toString();
       if (!Int128.fromString(s).equals(MAX_VALUE))  throw new AssertionError("roundtrip MAX");
       // Division corner: MIN_VALUE / -1 -> MIN_VALUE (wrap)
       Int128 q = MIN_VALUE.div(Int128.valueOf(-1));
       if (!q.equals(MIN_VALUE))                     throw new AssertionError("MIN/-1 wrap");
       // parseHex corner
       if (!parseHex("-0x1").equals(Int128.valueOf(-1))) throw new AssertionError("parseHex -0x1");
   }
   // ------------------------------------------------------------
   // end
   // ------------------------------------------------------------
}
