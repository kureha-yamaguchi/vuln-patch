package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    /*__vpCause*/ private static Throwable __vpCause = null;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOraclePairs();
        checkLiftedOracleRejections();

        // Metamorphic check from equivalent overloads on valid-by-construction small inputs.
        relationOverloadAgreementLongIntLongLongSmallInputs(data);
        relationOverloadAgreementLongIntIntIntSmallInputs(data);

        // Additional trusted metamorphic relation:
        // For non-overflowing small inputs, safe multiplication distributes over safe addition:
        // safeMultiply(x, y + z) == safeAdd(safeMultiply(x, y), safeMultiply(x, z)).
        // This uses only real library calls, keeps magnitudes small by construction,
        // and would catch a patch that avoids throwing by silently returning a wrong product.
        relationDistributiveSmallInputs(data);

        // Drive the patched branch with fuzzed input as well.
        // Trusted oracle by construction:
        // if val2 is 1 or 0, the documented implementation returns val1 or 0 respectively;
        // if val2 is -1 and val1 != Long.MIN_VALUE, the result must be -val1;
        // if val2 is -1 and val1 == Long.MIN_VALUE, overflow must throw.
        long val1 = data.consumeBoolean() ? Long.MIN_VALUE : (long) data.consumeInt();
        int selector = data.consumeInt(0, 3);
        int val2;
        if (selector == 0) {
            val2 = -1;
        } else if (selector == 1) {
            val2 = 0;
        } else if (selector == 2) {
            val2 = 1;
        } else {
            val2 = data.consumeInt(-1000000, 1000000);
        }

        if (val2 == -1) {
            boolean threw = false;
            long actual = 0L;
            try {
                actual = FieldUtils.safeMultiply(val1, val2);
            } catch (ArithmeticException e) { __vpCause = e;
                threw = true;
            }
            if (val1 == Long.MIN_VALUE) {
                if (!threw) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:fuzzed-minus-one-overflow] semantic mismatch: safeMultiply(Long.MIN_VALUE, -1) must throw ArithmeticException but returned " + actual, __vpCause);
                }
            } else {
                if (threw) {
                    return;
                }
                long expected = -val1;
                if (actual != expected) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:fuzzed-minus-one-overflow] semantic mismatch: safeMultiply(" + val1 + ", -1) expected " + expected + " but got " + actual);
                }
            }
        } else if (val2 == 0) {
            try {
                long actual = FieldUtils.safeMultiply(val1, 0);
                if (actual != 0L) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:fuzzed-zero] semantic mismatch: safeMultiply(" + val1 + ", 0) expected 0 but got " + actual);
                }
            } catch (ArithmeticException e) {
                return;
            }
        } else if (val2 == 1) {
            try {
                long actual = FieldUtils.safeMultiply(val1, 1);
                if (actual != val1) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:fuzzed-one] semantic mismatch: safeMultiply(" + val1 + ", 1) expected " + val1 + " but got " + actual);
                }
            } catch (ArithmeticException e) {
                return;
            }
        } else {
            try {
                FieldUtils.safeMultiply(val1, val2);
            } catch (ArithmeticException e) {
                return;
            }
        }
    }

    private static void checkLiftedOraclePairs() {
        assertEqualsLong("[oracle:lifted-0x0]", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("[oracle:lifted-1x1]", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("[oracle:lifted-1x3]", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("[oracle:lifted-3x1]", 3L, FieldUtils.safeMultiply(3L, 1));
        assertEqualsLong("[oracle:lifted-2x3]", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("[oracle:lifted-2x-3]", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("[oracle:lifted--2x3]", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("[oracle:lifted--2x-3]", 6L, FieldUtils.safeMultiply(-2L, -3));
        assertEqualsLong("[oracle:lifted--1xIntegerMin]", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));
        assertEqualsLong("[oracle:lifted-longMaxx1]", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longMinx1]", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longMaxx-1]", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));
    }

    private static void checkLiftedOracleRejections() {
        assertArithmeticException("[oracle:lifted-longMinx-1-throws]", Long.MIN_VALUE, -1);
        assertArithmeticException("[oracle:lifted-longMinx100-throws]", Long.MIN_VALUE, 100);
        assertArithmeticException("[oracle:lifted-longMinxIntMax-throws]", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertArithmeticException("[oracle:lifted-longMaxxIntMin-throws]", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void relationOverloadAgreementLongIntLongLongSmallInputs(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (RuntimeException e) {
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
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] relation overload_agreement_longInt_longLong_small_inputs violated: lhs=" + r1 + " rhs=" + r2 +
                " val1=" + val1 + " val2=" + val2);
        }
    }

    private static void relationOverloadAgreementLongIntIntIntSmallInputs(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (RuntimeException e) {
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
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] relation overload_agreement_longInt_intInt_small_inputs violated: lhs=" + r1 + " rhs=" + r2 +
                " a=" + a + " b=" + b);
        }
    }

    private static void relationDistributiveSmallInputs(FuzzedDataProvider data) {
        long x;
        int y;
        int z;
        try {
            x = (long) data.consumeInt(-10000, 10000);
            y = data.consumeInt(-10000, 10000);
            z = data.consumeInt(-10000, 10000);
        } catch (RuntimeException e) {
            return;
        }

        long lhs;
        long rhs;
        try {
            int yz = FieldUtils.safeAdd(y, z);
            lhs = FieldUtils.safeMultiply(x, yz);
            rhs = FieldUtils.safeAdd(FieldUtils.safeMultiply(x, y), FieldUtils.safeMultiply(x, z));
        } catch (Exception e) {
            return;
        }

        if (lhs != rhs) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:distributive-small] metamorphic violation: safeMultiply(x, safeAdd(y, z)) == safeAdd(safeMultiply(x, y), safeMultiply(x, z)) input=x:" +
                x + ",y:" + y + ",z:" + z + " lhs=" + lhs + " rhs=" + rhs);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected " + expected + " but got " + actual);
        }
    }

    private static void assertArithmeticException(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected ArithmeticException but got return value " + actual);
        } catch (ArithmeticException expected) {
        }
    }
}