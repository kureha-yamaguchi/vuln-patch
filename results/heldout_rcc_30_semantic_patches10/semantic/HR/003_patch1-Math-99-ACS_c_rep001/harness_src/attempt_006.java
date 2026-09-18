package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = 30;
        int b = 50;
        int c = 77;

        if (MathUtils.lcm(0, b) != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm-0b] semantic mismatch: MathUtils.lcm(0, 50) expected=0 actual=" + MathUtils.lcm(0, b));
        }
        if (MathUtils.lcm(a, 0) != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm-a0] semantic mismatch: MathUtils.lcm(30, 0) expected=0 actual=" + MathUtils.lcm(a, 0));
        }
        if (MathUtils.lcm(1, b) != b) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm-1b] semantic mismatch: MathUtils.lcm(1, 50) expected=50 actual=" + MathUtils.lcm(1, b));
        }
        if (MathUtils.lcm(a, 1) != a) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm-a1] semantic mismatch: MathUtils.lcm(30, 1) expected=30 actual=" + MathUtils.lcm(a, 1));
        }
        if (MathUtils.lcm(a, b) != 150) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm-ab] semantic mismatch: MathUtils.lcm(30, 50) expected=150 actual=" + MathUtils.lcm(a, b));
        }
        if (MathUtils.lcm(-a, b) != 150) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm--ab] semantic mismatch: MathUtils.lcm(-30, 50) expected=150 actual=" + MathUtils.lcm(-a, b));
        }
        if (MathUtils.lcm(a, -b) != 150) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm-a-b] semantic mismatch: MathUtils.lcm(30, -50) expected=150 actual=" + MathUtils.lcm(a, -b));
        }
        if (MathUtils.lcm(-a, -b) != 150) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm--a-b] semantic mismatch: MathUtils.lcm(-30, -50) expected=150 actual=" + MathUtils.lcm(-a, -b));
        }
        if (MathUtils.lcm(a, c) != 2310) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm-ac] semantic mismatch: MathUtils.lcm(30, 77) expected=2310 actual=" + MathUtils.lcm(a, c));
        }
        if (MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5) != ((1 << 20) * 15)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm-pow-scale] semantic mismatch: MathUtils.lcm((1<<20)*3, (1<<20)*5) expected=" + ((1 << 20) * 15) + " actual=" + MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5));
        }
        if (MathUtils.lcm(0, 0) != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm-00] semantic mismatch: MathUtils.lcm(0, 0) expected=0 actual=" + MathUtils.lcm(0, 0));
        }

        boolean violated = false;
        String why = "";
        try {
            MathUtils.lcm(Integer.MIN_VALUE, 1);
            violated = true;
            why = "completed normally for MathUtils.lcm(Integer.MIN_VALUE, 1)";
        } catch (ArithmeticException expected) {
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            violated = true;
            why = "wrong exception for MathUtils.lcm(Integer.MIN_VALUE, 1): " + t.getClass().getName();
        }
        if (violated) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm-min-1-throws] semantic mismatch: " + why);
        }

        violated = false;
        why = "";
        try {
            MathUtils.lcm(Integer.MIN_VALUE, 1 << 20);
            violated = true;
            why = "completed normally for MathUtils.lcm(Integer.MIN_VALUE, 1<<20)";
        } catch (ArithmeticException expected) {
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            violated = true;
            why = "wrong exception for MathUtils.lcm(Integer.MIN_VALUE, 1<<20): " + t.getClass().getName();
        }
        if (violated) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm-min-pow-throws] semantic mismatch: " + why);
        }

        violated = false;
        why = "";
        try {
            MathUtils.lcm(Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
            violated = true;
            why = "completed normally for MathUtils.lcm(Integer.MAX_VALUE, Integer.MAX_VALUE - 1)";
        } catch (ArithmeticException expected) {
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            violated = true;
            why = "wrong exception for MathUtils.lcm(Integer.MAX_VALUE, Integer.MAX_VALUE - 1): " + t.getClass().getName();
        }
        if (violated) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-lcm-max-maxminus1-throws] semantic mismatch: " + why);
        }

        int k = data.consumeInt(0, 30);
        int base = 1 << k;
        int delta = data.consumeInt(-1, 1);
        int n = base + delta;
        if (n == 0) {
            n = 1;
        }
        if (data.consumeBoolean()) {
            n = -n;
        }

        violated = false;
        why = "";
        try {
            MathUtils.lcm(Integer.MIN_VALUE, n);
            violated = true;
            why = "completed normally for MathUtils.lcm(Integer.MIN_VALUE, " + n + ")";
        } catch (ArithmeticException expected) {
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (violated) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-minvalue-all-nonzero-throw-near-boundary] metamorphic violation: for any nonzero n, lcm(Integer.MIN_VALUE, n) is a positive multiple of 2^31 and therefore cannot fit in int; expected ArithmeticException but " + why);
        }

        int safeDelta = data.consumeInt(1, 1024);
        int nearMin = Integer.MIN_VALUE + safeDelta;
        int expectedAbs = Math.abs(nearMin);

        int actualLeft;
        int actualRight;
        try {
            actualLeft = MathUtils.gcd(0, nearMin);
            actualRight = MathUtils.gcd(nearMin, 0);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (actualLeft != expectedAbs) {
            throw new FuzzerSecurityIssueLow("[oracle:gcd-neighbor-of-min-zero-exact] semantic mismatch: MathUtils.gcd(0, " + nearMin + ") expected=" + expectedAbs + " actual=" + actualLeft);
        }
        if (actualRight != expectedAbs) {
            throw new FuzzerSecurityIssueLow("[oracle:gcd-neighbor-of-min-zero-symmetric] metamorphic violation: gcd(x,0) must equal gcd(0,x)=abs(x) for x!=Integer.MIN_VALUE; x=" + nearMin + " lhs=" + actualRight + " rhs=" + expectedAbs);
        }

        int x = data.consumeInt(-1_000_000, 1_000_000);
        int y = data.consumeInt(-1_000_000, 1_000_000);
        try {
            int m1 = MathUtils.mulAndCheck(x, y);
            int m2 = MathUtils.mulAndCheck(y, x);
            if (m1 != m2) {
                throw new FuzzerSecurityIssueLow("[oracle:mul-commutative-safe-range] consistency violation: mulAndCheck must be commutative for exact int products input=(" + x + "," + y + ") lhs=" + m1 + " rhs=" + m2);
            }
        } catch (ArithmeticException expected) {
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }
}