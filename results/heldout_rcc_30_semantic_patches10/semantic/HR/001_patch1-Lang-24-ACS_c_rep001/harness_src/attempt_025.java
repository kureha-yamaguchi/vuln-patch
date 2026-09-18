package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        verifyLiftedTestSeeds();
        verifyPatchedBoundaryGeneralization(data);
        String extra = data.consumeRemainingAsString();
        try {
            NumberUtils.isNumber(extra);
        } catch (Throwable t) {
        }
    }

    private static void verifyLiftedTestSeeds() {
        assertSeed("lifted-pos-1", "12345", true, true);
        assertSeed("lifted-pos-2", "1234.5", true, true);
        assertSeed("lifted-pos-3", ".12345", true, true);
        assertSeed("lifted-pos-4", "1234E5", true, true);
        assertSeed("lifted-pos-5", "1234E+5", true, true);
        assertSeed("lifted-pos-6", "1234E-5", true, true);
        assertSeed("lifted-pos-7", "123.4E5", true, true);
        assertSeed("lifted-pos-8", "-1234", true, true);
        assertSeed("lifted-pos-9", "-1234.5", true, true);
        assertSeed("lifted-pos-10", "-.12345", true, true);
        assertSeed("lifted-pos-11", "-1234E5", true, true);
        assertSeed("lifted-pos-12", "0", true, true);
        assertSeed("lifted-pos-13", "-0", true, true);
        assertSeed("lifted-pos-14", "01234", true, true);
        assertSeed("lifted-pos-15", "-01234", true, true);
        assertSeed("lifted-pos-16", "0xABC123", true, true);
        assertSeed("lifted-pos-17", "0x0", true, true);
        assertSeed("lifted-pos-19", "123.4E21D", true, true);
        assertSeed("lifted-pos-20", "-221.23F", true, true);
        assertSeed("lifted-pos-21", "22338L", true, true);

        assertSeed("lifted-neg-1", null, false, false);
        assertSeed("lifted-neg-2", "", false, false);
        assertSeed("lifted-neg-3", "--2.3", false, false);
        assertSeed("lifted-neg-4", ".12.3", false, false);
        assertSeed("lifted-neg-5", "-123E", false, false);
        assertSeed("lifted-neg-6", "-123E+-212", false, false);
        assertSeed("lifted-neg-7", "-123E2.12", false, false);
        assertSeed("lifted-neg-8", "0xGF", false, false);
        assertSeed("lifted-neg-9", "0xFAE-1", false, false);
        assertSeed("lifted-neg-10", ".", false, false);
        assertSeed("lifted-neg-11", "-0ABC123", false, false);
        assertSeed("lifted-neg-12", "123.4E-D", false, false);
        assertSeed("lifted-neg-13", "123.4ED", false, false);
        assertSeed("lifted-neg-14", "1234E5l", false, false);
        assertSeed("lifted-neg-15", "11a", false, false);
        assertSeed("lifted-neg-16", "1a", false, false);
        assertSeed("lifted-neg-17", "a", false, false);
        assertSeed("lifted-neg-18", "11g", false, false);
        assertSeed("lifted-neg-19", "11z", false, false);
        assertSeed("lifted-neg-20", "11def", false, false);
        assertSeed("lifted-neg-21", "11d11", false, false);
        assertSeed("lifted-neg-22", "11 11", false, false);
        assertSeed("lifted-neg-23", " 1111", false, false);
        assertSeed("lifted-neg-24", "1111 ", false, false);

        assertSeed("lifted-lang-521", "2.", true, true);
        assertSeed("lifted-lang-664", "1.1L", false, false);
    }

    private static void verifyPatchedBoundaryGeneralization(FuzzedDataProvider data) {
        int whole = data.consumeInt(1, 1_000_000);
        int frac = data.consumeInt(1, 1_000_000);
        boolean negative = data.consumeBoolean();
        boolean lowerSuffix = data.consumeBoolean();

        String sign = negative ? "-" : "";
        String suffix = lowerSuffix ? "l" : "L";
        String integral = sign + whole + suffix;
        String decimal = sign + whole + "." + frac + suffix;
        String trailingDot = sign + whole + "." + suffix;

        boolean emptyIntegral = org.apache.commons.lang3.StringUtils.isEmpty(integral);
        boolean emptyDecimal = org.apache.commons.lang3.StringUtils.isEmpty(decimal);
        boolean emptyTrailing = org.apache.commons.lang3.StringUtils.isEmpty(trailingDot);

        if (emptyIntegral || emptyDecimal || emptyTrailing) {
            throw new FuzzerSecurityIssueLow("[oracle:boundary-nonempty-construction] semantic mismatch: constructed literals must be non-empty integral=" + integral + " decimal=" + decimal + " trailingDot=" + trailingDot);
        }

        boolean integralIsNumber;
        boolean decimalIsNumber;
        boolean trailingIsNumber;
        try {
            integralIsNumber = NumberUtils.isNumber(integral);
            decimalIsNumber = NumberUtils.isNumber(decimal);
            trailingIsNumber = NumberUtils.isNumber(trailingDot);
        } catch (Throwable t) {
            return;
        }

        boolean integralCreate = checkCreateNumber(integral);
        boolean decimalCreate = checkCreateNumber(decimal);
        boolean trailingCreate = checkCreateNumber(trailingDot);

        if (!integralIsNumber || !integralCreate) {
            throw new FuzzerSecurityIssueLow("[oracle:boundary-integral-long-constructed] metamorphic violation: canonical integral long literal should be accepted integral=" + integral + " isNumber=" + integralIsNumber + " createNumberAccepts=" + integralCreate);
        }

        /* Contract basis:
         * - The lifted test hard-codes "22338L" as valid.
         * - The lifted test hard-codes "1.1L" as invalid.
         * - The implementation comment at the patched line says "not allowing L with an exponent or decimal point".
         * Therefore for any correctly constructed non-empty decimal literal with an L/l suffix, inserting a decimal point before the suffix flips acceptance from the integral form to rejection.
         * A band-aid that special-cases only "1.1L" or deletes the decimal-point guard would violate this relation.
         */
        if (decimalIsNumber || decimalCreate) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-point-before-long-suffix] metamorphic violation: adding a decimal point before L/l must invalidate the literal integral=" + integral + " decimal=" + decimal + " integralIsNumber=" + integralIsNumber + " decimalIsNumber=" + decimalIsNumber + " integralCreateAccepts=" + integralCreate + " decimalCreateAccepts=" + decimalCreate);
        }

        /* Same documented/seeded guarantee as above: any decimal point before L/l invalidates the number.
         * Using the trailing-dot shape checks the boundary immediately adjacent to the suffix, not just the exact seed "1.1L".
         */
        if (trailingIsNumber || trailingCreate) {
            throw new FuzzerSecurityIssueLow("[oracle:trailing-decimal-point-before-long-suffix] metamorphic violation: trailing decimal point before L/l must invalidate the literal integral=" + integral + " trailingDot=" + trailingDot + " integralIsNumber=" + integralIsNumber + " trailingIsNumber=" + trailingIsNumber + " integralCreateAccepts=" + integralCreate + " trailingCreateAccepts=" + trailingCreate);
        }
    }

    private static void assertSeed(String oracleId, String val, boolean expectedIsNumber, boolean expectedCreateNumber) {
        boolean actualIsNumber = NumberUtils.isNumber(val);
        if (actualIsNumber != expectedIsNumber) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "-isnumber] semantic mismatch: input=" + String.valueOf(val) + " expected=" + expectedIsNumber + " actual=" + actualIsNumber);
        }

        boolean actualCreateNumber = checkCreateNumber(val);
        if (actualCreateNumber != expectedCreateNumber) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "-createnumber] semantic mismatch: input=" + String.valueOf(val) + " expected=" + expectedCreateNumber + " actual=" + actualCreateNumber);
        }
    }

    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            if (obj == null) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}