package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        try {
            actualLong = FieldUtils.safeMultiply(0L, 0);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(0L, 0)", t);
        }
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 1);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(1L, 1)", t);
        }
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 3);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(1L, 3)", t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(3L, 1);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(3L, 1)", t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, 3);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(2L, 3)", t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, -3);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(2L, -3)", t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, 3);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-2L, 3)", t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, -3);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-2L, -3)", t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)", t);
        }
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MAX_VALUE, 1)", t);
        }
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MIN_VALUE, 1)", t);
        }
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MAX_VALUE, -1)", t);
        }
        if (actualLong != -Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, -1)", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, 100)", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)", t);
        }

        {
            long val1 = data.consumeLong();
            int val2 = data.consumeInt();

            try {
                long expected = FieldUtils.safeMultiply(val1, (long) val2);
                long actual = FieldUtils.safeMultiply(val1, val2);
                if (actual != expected) {
                    throw new FuzzerSecurityIssueLow("[oracle:long-int-vs-long-long] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs val1=" + val1 + " val2=" + val2 + " lhs=" + actual + " rhs=" + expected);
                }
            } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            }
        }

        {
            int a = data.consumeInt();
            int b = data.consumeInt();

            try {
                int intResult = FieldUtils.safeMultiply(a, b);
                long longResult = FieldUtils.safeMultiply((long) a, b);
                if (longResult != (long) intResult) {
                    throw new FuzzerSecurityIssueLow("[oracle:int-int-vs-long-int] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int result is valid a=" + a + " b=" + b + " lhs=" + longResult + " rhs=" + ((long) intResult));
                }
            } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            }
        }

        {
            int n = data.consumeInt(-1_000_000, 1_000_000);

            try {
                long viaMultiply = FieldUtils.safeMultiply((long) n, -1);
                int viaNegate = FieldUtils.safeNegate(n);
                if (viaMultiply != (long) viaNegate) {
                    throw new FuzzerSecurityIssueLow("[oracle:minus-one-vs-negate] metamorphic violation: multiplying by -1 must match safeNegate on int-range inputs n=" + n + " lhs=" + viaMultiply + " rhs=" + ((long) viaNegate));
                }
            } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            }
        }

        {
            long x = data.consumeInt(-1_000_000, 1_000_000);
            int scalar = data.consumeInt(-1000, 1000);

            try {
                long lhs = FieldUtils.safeMultiply(x, scalar);
                long rhs = FieldUtils.safeAdd(
                        FieldUtils.safeMultiply(x, scalar / 2),
                        FieldUtils.safeMultiply(x, scalar - (scalar / 2)));
                if (lhs != rhs) {
                    throw new FuzzerSecurityIssueLow("[oracle:composition-split] metamorphic violation: multiplication must distribute over splitting the scalar because safeMultiply and safeAdd document exact arithmetic with overflow checks x=" + x + " scalar=" + scalar + " lhs=" + lhs + " rhs=" + rhs);
                }
            } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            }
        }
    }
}