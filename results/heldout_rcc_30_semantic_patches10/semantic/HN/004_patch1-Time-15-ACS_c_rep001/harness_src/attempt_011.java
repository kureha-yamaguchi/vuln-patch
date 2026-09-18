package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();
        relationOverloadAgreementLongIntLongLongSmallInputs(data);
        relationOverloadAgreementLongIntIntIntSmallInputs(data);
        relationSafeMultiplyByMinusOneAgreesWithSafeNegate(data);
        exercisePatchedBranch(data);
    }

    private static void checkLiftedOracles() {
        assertEqualsLong("[oracle:lifted-0x0]", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("[oracle:lifted-1x1]", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("[oracle:lifted-1x3]", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("[oracle:lifted-3x1]", 3L, FieldUtils.safeMultiply(3L, 1));

        assertEqualsLong("[oracle:lifted-2x3]", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("[oracle:lifted-2x-3]", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("[oracle:lifted--2x3]", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("[oracle:lifted--2x-3]", 6L, FieldUtils.safeMultiply(-2L, -3));

        assertEqualsLong("[oracle:lifted--1xintmin]", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        assertEqualsLong("[oracle:lifted-longmaxx1]", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longminx1]", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longmaxx-1]", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        assertThrowsArithmetic("[oracle:lifted-longminx-1]", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("[oracle:lifted-longminx100]", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("[oracle:lifted-longminxintmax]", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("[oracle:lifted-longmaxxintmin]", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void relationOverloadAgreementLongIntLongLongSmallInputs(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] relation overload_agreement_longInt_longLong_small_inputs violated: input=" + val1 + "," + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void relationOverloadAgreementLongIntIntIntSmallInputs(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] relation overload_agreement_longInt_intInt_small_inputs violated: input=" + a + "," + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void relationSafeMultiplyByMinusOneAgreesWithSafeNegate(FuzzedDataProvider data) {
        int val;
        try {
            if (data.consumeBoolean()) {
                val = data.consumeInt(-1000000, 1000000);
            } else {
                val = Integer.MIN_VALUE;
            }
        } catch (Exception e) {
            return;
        }

        Long mul = null;
        Throwable mulThrown = null;
        try {
            mul = Long.valueOf(FieldUtils.safeMultiply((long) val, -1));
        } catch (Throwable t) {
            mulThrown = t;
        }

        Integer neg = null;
        Throwable negThrown = null;
        try {
            neg = Integer.valueOf(FieldUtils.safeNegate(val));
        } catch (Throwable t) {
            negThrown = t;
        }

        if (mulThrown != null || negThrown != null) {
            if (mulThrown != null && negThrown != null &&
                mulThrown instanceof ArithmeticException && negThrown instanceof ArithmeticException) {
                return;
            }
            return;
        }

        /* Contract guarantee asserted: safeMultiply((long)value, -1) and safeNegate(value)
           are both checked negation in the safe* family; for any correct implementation on
           int inputs where neither rejects, they must yield the same numeric result. A patch
           that removes or bypasses the -1 overflow rejection, or returns a wrong negated
           value instead of throwing, violates this post-condition. */
        if (mul.longValue() != (long) neg.intValue()) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] relation safeMultiply_minusOne_agrees_with_safeNegate violated: input=" + val + " lhs=" + mul + " rhs=" + neg);
        }
    }

    private static void exercisePatchedBranch(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            boolean chooseSentinel = data.consumeBoolean();
            if (chooseSentinel) {
                val1 = Long.MIN_VALUE;
                val2 = -1;
            } else {
                int hi = data.consumeInt();
                int lo = data.consumeInt();
                val1 = (((long) hi) << 32) ^ (((long) lo) & 0xffffffffL);
                int selector = data.consumeInt(0, 4);
                if (selector == 0) {
                    val2 = -1;
                } else if (selector == 1) {
                    val2 = 0;
                } else if (selector == 2) {
                    val2 = 1;
                } else {
                    val2 = data.consumeInt();
                }
            }
        } catch (Exception e) {
            return;
        }

        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException expected) {
            return;
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException e) {
            return;
        }
        throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected ArithmeticException for input=" + val1 + "," + val2);
    }
}