import com.symguac.int128.impl.highperf.FastInt128Value;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FastInt128ValueTest {

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

            BigInteger bi = toBigInteger(hi, lo);
            assertEquals(bi.toString(), x.toString());
        }
    }

    private static BigInteger toBigInteger(long high, long low) {
        byte[] bytes = new byte[16];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (high >>> (8 * (7 - i)));
        }
        for (int i = 0; i < 8; i++) {
            bytes[8 + i] = (byte) (low >>> (8 * (7 - i)));
        }
        return new BigInteger(bytes);
    }
}
