import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class Int128PropertyTest {
    private static Int128 rnd(Random r) {
        return Int128.of(r.nextLong(), r.nextLong());
    }

    @Test
    public void identityAndBoundsRandom() {
        Random r = new Random(123);
        for (int i = 0; i < 20_000; i++) {
            Int128 a = rnd(r);
            Int128 d;
            do {
                d = rnd(r);
            } while (d.isZero());

            Int128[] dr = a.divRem(d);
            Int128 q = dr[0];
            Int128 rem = dr[1];

            assertEquals(a, q.mul(d).add(rem));
            assertTrue(rem.abs().compareUnsigned(d.abs()) < 0);
            assertTrue(rem.isZero() || rem.signum() == a.signum());
        }
    }

    @Test
    public void adversarialDiv128by128() {
        Int128 a = Int128.parseHex("0xFFFF000000000000FFFF000000000000");
        Int128 d = Int128.parseHex("0x0000FFFF00000000FFFFFFFF00000001");
        Int128[] dr = a.divRem(d);
        assertEquals(a, dr[0].mul(d).add(dr[1]));
        assertTrue(dr[1].abs().compareUnsigned(d.abs()) < 0);
    }

    @Test
    public void pow10Helpers() {
        for (int k = 0; k <= 38; k++) {
            BigInteger expected = BigInteger.valueOf(-37).multiply(BigInteger.TEN.pow(k));
            if (expected.abs().bitLength() < 128) {
                Int128 x = Int128.fromString(expected.toString());
                Int128[] dr = x.divRemPow10(k);
                assertEquals(Int128.valueOf(-37), dr[0], "k=" + k);
                assertTrue(dr[1].isZero(), "k=" + k);
            }
        }

        assertEquals(Int128.valueOf(2), Int128.valueOf(25).divRoundHalfEvenPow10(1));
        assertEquals(Int128.valueOf(2), Int128.valueOf(15).divRoundHalfEvenPow10(1));
        assertEquals(Int128.valueOf(-2), Int128.valueOf(-25).divRoundHalfEvenPow10(1));
        assertEquals(Int128.valueOf(-2), Int128.valueOf(-15).divRoundHalfEvenPow10(1));
    }
}
