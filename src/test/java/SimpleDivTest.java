public class SimpleDivTest {
    public static void main(String[] args) {
        // Simple positive test
        System.out.println("Test 1: Simple positive division");
        Int128 a = Int128.valueOf(100);
        Int128 d = Int128.valueOf(10);
        testDiv(a, d);

        // Test with 10^19 and a positive dividend that fits in long
        System.out.println("\nTest 2: Small positive / 10^19");
        a = Int128.valueOf(123456789L);
        d = Int128.tenPow(19);
        testDiv(a, d);

        // Test with a large positive number / 10^19
        System.out.println("\nTest 3: Large positive (2^100) / 10^19");
        a = Int128.ONE.shiftLeft(100);  // 2^100
        d = Int128.tenPow(19);
        testDiv(a, d);

        // Test specific failing case but smaller
        System.out.println("\nTest 4: Specific case investigation");
        // Use a simpler negative dividend
        a = Int128.valueOf(-1000000000000000000L); // -10^18
        d = Int128.valueOf(100);
        testDiv(a, d);

        System.out.println("\nTest 5: Check 10^19 value");
        d = Int128.tenPow(19);
        System.out.println("10^19 = " + d.toDebugHex());
        System.out.println("10^19 hi = " + String.format("0x%016X", d.toLong() >>> 64));
        System.out.println("10^19 lo = " + String.format("0x%016X", d.toLong()));
        System.out.println("10^19 as long = " + d.toLong());
        System.out.println("10^19 is negative (as long)? " + (d.toLong() < 0));
    }

    private static void testDiv(Int128 a, Int128 d) {
        System.out.println("  a = " + a.toDebugHex() + " (signum=" + a.signum() + ")");
        System.out.println("  d = " + d.toDebugHex() + " (signum=" + d.signum() + ")");

        try {
            Int128[] qr = a.divRem(d);
            Int128 q = qr[0];
            Int128 r = qr[1];
            Int128 recomposed = q.mul(d).add(r);

            System.out.println("  q = " + q.toDebugHex());
            System.out.println("  r = " + r.toDebugHex());
            System.out.println("  q*d+r = " + recomposed.toDebugHex());
            System.out.println("  OK? " + recomposed.equals(a));

            if (!recomposed.equals(a)) {
                System.out.println("  ERROR: Identity failed!");
                System.out.println("  Expected: " + a);
                System.out.println("  Got:      " + recomposed);
            }
        } catch (Exception e) {
            System.out.println("  EXCEPTION: " + e);
            e.printStackTrace();
        }
    }
}
