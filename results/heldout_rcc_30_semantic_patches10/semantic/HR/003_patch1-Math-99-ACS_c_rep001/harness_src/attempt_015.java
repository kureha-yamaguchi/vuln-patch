package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedLcmAssertions();
        runNewExactLongOverflowConsistencyOracle(data);
        callTouchedFunctions(data);
    }

    private static void runLiftedLcmAssertions() {
        checkEquals("lifted-lcm-0b", 0, MathUtils.lcm(0, 50));
        checkEquals("lifted-lcm-a0", 0, MathUtils.lcm(30, 0));
        checkEquals("lifted-lcm-1b", 50, MathUtils.lcm(1, 50));
        checkEquals("lifted-lcm-a1", 30, MathUtils.lcm(30, 1));
        checkEquals("lifted-lcm-ab", 150, MathUtils.lcm(30, 50));
        checkEquals("lifted-lcm-negab", 150, MathUtils.lcm(-30, 50));
        checkEquals("lifted-lcm-anegb", 150, MathUtils.lcm(30, -50));
        checkEquals("lifted-lcm-neganegb", 150, MathUtils.lcm(-30, -50));
        checkEquals("lifted-lcm-ac", 2310, MathUtils.lcm(30, 77));
        checkEquals("lifted-lcm-powscale", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5));
        checkEquals("lifted-lcm-00", 0, MathUtils.lcm(0, 0));

        expectArithmeticFromLcm("lifted-lcm-min-one-throws", Integer.MIN_VALUE, 1);
        expectArithmeticFromLcm("lifted-lcm-min-pow-throws", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticFromLcm("lifted-lcm-max-maxminus-throws", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void runNewExactLongOverflowConsistencyOracle(FuzzedDataProvider data) {
        int k = data.consumeInt(0, 30);
        int n = 1 << k;
        if (data.consumeBoolean()) {
            n = -n;
        }

        checkLcmAgainstIndependentExactLong("exact-long-overflow-left", Integer.MIN_VALUE, n);
        checkLcmAgainstIndependentExactLong("exact-long-overflow-right", n, Integer.MIN_VALUE);
    }

    private static void callTouchedFunctions(FuzzedDataProvider data) {
        int a = bounded(data.consumeInt());
        int b = bounded(data.consumeInt());

        try {
            MathUtils.gcd(a, b);
        } catch (ArithmeticException ignored) {
        }

        try {
            MathUtils.lcm(a, b);
        } catch (ArithmeticException ignored) {
        }
    }

    private static int bounded(int x) {
        if (x == Integer.MIN_VALUE) {
            return 0;
        }
        int v = x % 1000000;
        return v;
    }

    private static void checkLcmAgainstIndependentExactLong(String id, int a, int b) {
        boolean completed = false;
        int actual = 0;
        Throwable unexpected = null;
        try {
            actual = MathUtils.lcm(a, b);
            completed = true;
        } catch (ArithmeticException expected) {
            return;
        } catch (Throwable t) {
            unexpected = t;
        }

        if (unexpected != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] consistency violation: unexpected throwable type=" + unexpected.getClass().getName(),
                unexpected);
        }

        long exact = exactLcmLong(a, b);
        /* Contract used for this oracle:
           lcm(a,b) computes the least common multiple as a nonnegative int, and the failing test
           pins that when the mathematical result is 2^31 it must be rejected rather than returned.
           An independently recomputed exact long lcm is therefore a sound second source:
           if exact > Integer.MAX_VALUE, any normal int return is wrong even if a band-aid removed the throw. */
        if (completed && exact > Integer.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] consistency violation: exactLongLcm=" + exact
                    + " exceeds Integer.MAX_VALUE but MathUtils.lcm returned actual=" + actual
                    + " for a=" + a + " b=" + b);
        }
        if (completed && actual < 0) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] consistency violation: lcm must be nonnegative but actual=" + actual
                    + " for a=" + a + " b=" + b + " exactLongLcm=" + exact);
        }
        if (completed && exact <= Integer.MAX_VALUE && actual != (int) exact) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] consistency violation: actual=" + actual
                    + " exactLongLcm=" + exact + " for a=" + a + " b=" + b);
        }
    }

    private static long exactLcmLong(int a, int b) {
        if (a == 0 || b == 0) {
            return 0L;
        }
        int g = MathUtils.gcd(a, b);
        long product = ((long) (a / g)) * ((long) b);
        return product < 0 ? -product : product;
    }

    private static void checkEquals(String id, int expected, int actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmeticFromLcm(String id, int a, int b) {
        boolean violated = false;
        try {
            MathUtils.lcm(a, b);
            violated = true;
        } catch (ArithmeticException expected) {
            return;
        }
        if (violated) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException for MathUtils.lcm("
                    + a + "," + b + ")");
        }
    }
}