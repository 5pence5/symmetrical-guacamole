public class DebugDivisionTest {
    public static void main(String[] args) {
        // Test case 1: The boundary divisor that fails
        Int128 a1 = Int128.parseHex("0x80000000000000000000000000000000"); // MIN_VALUE
        Int128 d1 = Int128.parseHex("0xFFFFFFFFFFFFFFFF0000000000000001"); // -1 * 2^64 + 1

        System.out.println("Test 1: Boundary Divisor");
        System.out.println("a = " + a1.toDebugHex() + " (signed: " + a1.signum() + ")");
        System.out.println("d = " + d1.toDebugHex() + " (signed: " + d1.signum() + ")");
        System.out.println("a (abs) = " + a1.abs().toDebugHex());
        System.out.println("d (abs) = " + d1.abs().toDebugHex());

        try {
            Int128[] qr = a1.divRem(d1);
            Int128 q = qr[0];
            Int128 r = qr[1];
            Int128 recomposed = q.mul(d1).add(r);

            System.out.println("q = " + q.toDebugHex());
            System.out.println("r = " + r.toDebugHex());
            System.out.println("q*d + r = " + recomposed.toDebugHex());
            System.out.println("Match: " + recomposed.equals(a1));
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            e.printStackTrace();
        }

        System.out.println("\nTest 2: Power of 10^19");
        Int128 a2 = Int128.parseHex("0xCD1D47FB62105531D0B77685288D0110");
        Int128 d2 = Int128.tenPow(19);

        System.out.println("a = " + a2.toDebugHex());
        System.out.println("d = 10^19 = " + d2.toDebugHex());

        try {
            Int128[] qr = a2.divRem(d2);
            Int128 q = qr[0];
            Int128 r = qr[1];
            Int128 recomposed = q.mul(d2).add(r);

            System.out.println("q = " + q.toDebugHex());
            System.out.println("r = " + r.toDebugHex());
            System.out.println("q*d + r = " + recomposed.toDebugHex());
            System.out.println("Match: " + recomposed.equals(a2));

            // Also test divRemPow10
            Int128[] qr2 = a2.divRemPow10(19);
            Int128 recomposed2 = qr2[0].mul(d2).add(qr2[1]);
            System.out.println("\ndivRemPow10 result:");
            System.out.println("q = " + qr2[0].toDebugHex());
            System.out.println("r = " + qr2[1].toDebugHex());
            System.out.println("q*d + r = " + recomposed2.toDebugHex());
            System.out.println("Match: " + recomposed2.equals(a2));
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            e.printStackTrace();
        }
    }
}
