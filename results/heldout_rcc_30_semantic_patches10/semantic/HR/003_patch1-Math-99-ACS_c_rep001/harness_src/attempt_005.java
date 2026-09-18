package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mode = data.consumeInt(0, 5);
        int a = data.consumeInt();
        int b = data.consumeInt();

        if (mode == 0) {
            a = Integer.MIN_VALUE;
            b = 0;
        } else if (mode == 1) {
            a = 0;
            b = Integer.MIN_VALUE;
        } else if (mode == 2) {
            a = Integer.MIN_VALUE;
            b = Integer.MIN_VALUE;
        } else if (mode == 3) {
            a = 1 << 30;
            b = -Integer.MIN_VALUE;
        } else if (mode == 4) {
            a = Integer.MIN_VALUE;
            b = 1;
        }

        checkGcdAgainstBigInteger(a, b);
        checkLcmAgainstBigInteger(a, b);
    }

    private static void checkGcdAgainstBigInteger(int a, int b) {
        BigInteger expected = BigInteger.valueOf(a).gcd(BigInteger.valueOf(b));
        boolean shouldThrow = expected.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0;

        try {
            int actual = MathUtils.gcd(a, b);

            if (shouldThrow) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:gcd-biginteger-consistency] semantic mismatch: gcd should throw for input a="
                                + a + " b=" + b + " because exactGcd=" + expected + " exceeds int range, but returned "
                                + actual);
            }

            int expectedInt = expected.intValue();

            if (actual != expectedInt) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:gcd-biginteger-consistency] semantic mismatch: input a="
                                + a + " b=" + b + " expected=" + expectedInt + " actual=" + actual);
            }

            /* Contract/invariant justification:
             * The shown implementation returns "-u * (1 << k)", and the lifted unit test
             * always expects a non-negative gcd. A throw-deleting patch on the MIN_VALUE/0
             * path returns Integer.MIN_VALUE, violating this observable post-condition even
             * if top-level exception checks are masked. */
            if (actual < 0) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:gcd-sign-postcondition] consistency violation: gcd must be non-negative for input a="
                                + a + " b=" + b + " but returned " + actual);
            }

            /* Independent consistency check from the exact mathematical quantity:
             * for every correct implementation, the returned gcd divides both inputs
             * whenever it returns normally. */
            if ((a % actual) != 0 || (b % actual) != 0) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:gcd-divides-both] consistency violation: returned gcd does not divide both operands for input a="
                                + a + " b=" + b + " gcd=" + actual);
            }
        } catch (ArithmeticException ex) {
            if (!shouldThrow) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:gcd-biginteger-consistency] semantic mismatch: unexpected ArithmeticException for input a="
                                + a + " b=" + b + " exactGcd=" + expected, ex);
            }
        }
    }

    private static void checkLcmAgainstBigInteger(int a, int b) {
        BigInteger absA = BigInteger.valueOf(a).abs();
        BigInteger absB = BigInteger.valueOf(b).abs();
        BigInteger expected;
        if (a == 0 || b == 0) {
            expected = BigInteger.ZERO;
        } else {
            expected = absA.divide(absA.gcd(absB)).multiply(absB);
        }
        boolean shouldThrow = expected.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0;

        try {
            int actual = MathUtils.lcm(a, b);

            if (shouldThrow) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lcm-biginteger-consistency] semantic mismatch: lcm should throw for input a="
                                + a + " b=" + b + " because exactLcm=" + expected + " exceeds int range, but returned "
                                + actual);
            }

            int expectedInt = expected.intValue();
            if (actual != expectedInt) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lcm-biginteger-consistency] semantic mismatch: input a="
                                + a + " b=" + b + " expected=" + expectedInt + " actual=" + actual);
            }

            /* Contract justification:
             * MathUtils.lcm is documented as "the least common multiple of the absolute
             * value of two numbers", so every normal return must be non-negative. */
            if (actual < 0) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lcm-nonnegative-postcondition] consistency violation: lcm must be non-negative for input a="
                                + a + " b=" + b + " but returned " + actual);
            }

            /* Cross-check using the exact gcd already exposed by the same API family:
             * for non-zero operands and non-overflowing results, lcm(a,b) * gcd(a,b)
             * must equal |a*b| exactly. This is an independent observable quantity that
             * catches band-aid patches that suppress throws but leave a related helper wrong. */
            if (a != 0 && b != 0) {
                try {
                    int g = MathUtils.gcd(a, b);
                    long lhs = (long) actual * (long) g;
                    long rhs = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)).abs().longValue();
                    if (lhs != rhs) {
                        throw new FuzzerSecurityIssueLow(
                                "[oracle:lcm-gcd-product-consistency-big] consistency violation: input a="
                                        + a + " b=" + b + " lcm=" + actual + " gcd=" + g + " lhs=" + lhs + " rhs=" + rhs);
                    }
                } catch (ArithmeticException ignored) {
                    return;
                }
            }
        } catch (ArithmeticException ex) {
            if (!shouldThrow) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lcm-biginteger-consistency] semantic mismatch: unexpected ArithmeticException for input a="
                                + a + " b=" + b + " exactLcm=" + expected, ex);
            }
        }
    }
}