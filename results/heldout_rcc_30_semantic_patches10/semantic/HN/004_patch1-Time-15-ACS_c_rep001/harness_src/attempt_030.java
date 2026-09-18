package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-00] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-01] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-02] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-03] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-04] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-05] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-06] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-07] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-08] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-09] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        // The overloads safeMultiply(long,int) and safeMultiply(long,long) are documented identically
        // apart from parameter type; on equivalent small, non-overflow inputs they must return the same long.
        long val1 = (long) data.consumeInt(-1000000, 1000000);
        int val2 = data.consumeInt(-1000000, 1000000);
        long r1;
        long r2;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }
        if (r1 != r2) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-overload-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent valid inputs val1=" + val1 + " val2=" + val2 + " lhs=" + r1 + " rhs=" + r2);
        }

        // When the first argument fits in int and the product fits in int, safeMultiply(long,int) and
        // safeMultiply(int,int) perform the same documented multiplication, with the int result widened to long.
        int a = data.consumeInt(-46340, 46340);
        int b = data.consumeInt(-46340, 46340);
        long rr1;
        int rr2;
        try {
            rr1 = FieldUtils.safeMultiply((long) a, b);
            rr2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }
        if (rr1 != (long) rr2) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-overload-longint-intint] metamorphic violation: safeMultiply(long,int) must agree with widened safeMultiply(int,int) on equivalent valid inputs a=" + a + " b=" + b + " lhs=" + rr1 + " rhs=" + rr2);
        }

        // Multiplication distributes over addition for all exact arithmetic. Using small magnitudes keeps all calls
        // valid by construction, so a wrong-value patch in safeMultiply or a throw-deleting patch is observable here.
        int x = data.consumeInt(-1000, 1000);
        int y = data.consumeInt(-1000, 1000);
        int z = data.consumeInt(-1000, 1000);
        long lhs;
        long rhs;
        try {
            lhs = FieldUtils.safeMultiply((long) x, y + z);
            rhs = FieldUtils.safeAdd(FieldUtils.safeMultiply((long) x, y), FieldUtils.safeMultiply((long) x, z));
        } catch (Exception e) {
            return;
        }
        if (lhs != rhs) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-distributive] metamorphic violation: safeMultiply must distribute over addition on valid small inputs x=" + x + " y=" + y + " z=" + z + " lhs=" + lhs + " rhs=" + rhs);
        }

        // The documented special cases show multiplying by 1 is identity and multiplying by 0 returns 0.
        // This post-condition remains observable even if a patch simply makes an error path unreachable.
        int idInput = data.consumeInt(-1000000, 1000000);
        long idValue = (long) idInput;
        long timesOne;
        long timesZero;
        try {
            timesOne = FieldUtils.safeMultiply(idValue, 1);
            timesZero = FieldUtils.safeMultiply(idValue, 0);
        } catch (Exception e) {
            return;
        }
        if (timesOne != idValue) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-identity-one] metamorphic violation: safeMultiply(v,1) must equal v for valid input v=" + idValue + " actual=" + timesOne);
        }
        if (timesZero != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-identity-zero] metamorphic violation: safeMultiply(v,0) must equal 0 for valid input v=" + idValue + " actual=" + timesZero);
        }
    }
}