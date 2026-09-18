package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        String violation = null;

        // Oracle from the input itself: for these exact boundary cases, the mathematical gcd is 2^31.
        // A correct int-returning implementation rejects them; if it returns normally, the returned int,
        // widened to long, must still represent that known answer to be semantically correct.
        // Buggy code returns Integer.MIN_VALUE instead.
        int gcdCase = data.consumeInt(0, 2);
        int gcdA;
        int gcdB;
        if (gcdCase == 0) {
            gcdA = Integer.MIN_VALUE;
            gcdB = 0;
        } else if (gcdCase == 1) {
            gcdA = 0;
            gcdB = Integer.MIN_VALUE;
        } else {
            gcdA = Integer.MIN_VALUE;
            gcdB = Integer.MIN_VALUE;
        }
        boolean gcdReturned = false;
        int gcdResult = 0;
        try {
            gcdResult = MathUtils.gcd(gcdA, gcdB);
            gcdReturned = true;
        } catch (Throwable t) {
            gcdReturned = false;
        }
        if (gcdReturned) {
            long expected = 1L << 31;
            long actual = (long) gcdResult;
            if (actual != expected) {
                violation = "[oracle:gcd-min-overflow-exact-long] semantic mismatch: gcd(" + gcdA + "," + gcdB
                        + ") returned int " + gcdResult + " (asLong=" + actual + ") but the mathematical gcd is "
                        + expected + " for this constructed boundary input";
            }
        }

        // Oracle from the input itself: if n = +/- 2^k, then lcm(Integer.MIN_VALUE, n) is exactly 2^31.
        // A correct implementation throws because 2^31 is not representable as a nonnegative int.
        // If it returns normally, the numeric value it reports must still equal that known mathematical lcm.
        int k = data.consumeInt(0, 30);
        int n = 1 << k;
        if (data.consumeBoolean()) {
            n = -n;
        }
        boolean swap = data.consumeBoolean();
        int lcmA = swap ? n : Integer.MIN_VALUE;
        int lcmB = swap ? Integer.MIN_VALUE : n;

        boolean lcmReturned = false;
        int lcmResult = 0;
        try {
            lcmResult = MathUtils.lcm(lcmA, lcmB);
            lcmReturned = true;
        } catch (Throwable t) {
            lcmReturned = false;
        }
        if (lcmReturned) {
            long expected = 1L << 31;
            long actual = (long) lcmResult;
            if (actual != expected) {
                String msg = "[oracle:lcm-min-power2-exact-long] semantic mismatch: lcm(" + lcmA + "," + lcmB
                        + ") returned int " + lcmResult + " (asLong=" + actual + ") but the mathematical lcm is "
                        + expected + " for Integer.MIN_VALUE with power-of-two input " + n;
                if (violation == null) {
                    violation = msg;
                } else {
                    violation = violation + " ; " + msg;
                }
            }
        }

        if (violation != null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(violation);
        }
    }
}