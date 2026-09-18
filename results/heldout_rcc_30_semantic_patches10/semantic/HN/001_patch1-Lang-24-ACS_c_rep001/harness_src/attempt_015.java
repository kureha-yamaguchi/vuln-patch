package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void assertBooleanEquals(String oracleId, String what, boolean expected, boolean actual, String input) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + what
                    + " input=" + String.valueOf(input)
                    + " expected=" + expected
                    + " actual=" + actual);
        }
    }

    private static void checkPair(String suffix, String val, boolean expectedIsNumber, boolean expectedCreateNumber) {
        boolean actualIsNumber = NumberUtils.isNumber(val);
        assertBooleanEquals("lifted-isNumber-" + suffix, "NumberUtils.isNumber", expectedIsNumber, actualIsNumber, val);

        boolean actualCreateNumber = checkCreateNumber(val);
        assertBooleanEquals("lifted-checkCreateNumber-" + suffix, "checkCreateNumber", expectedCreateNumber, actualCreateNumber, val);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkPair("1", "12345", true, true);
        checkPair("2", "1234.5", true, true);
        checkPair("3", ".12345", true, true);
        checkPair("4", "1234E5", true, true);
        checkPair("5", "1234E+5", true, true);
        checkPair("6", "1234E-5", true, true);
        checkPair("7", "123.4E5", true, true);
        checkPair("8", "-1234", true, true);
        checkPair("9", "-1234.5", true, true);
        checkPair("10", "-.12345", true, true);
        checkPair("11", "-1234E5", true, true);
        checkPair("12", "0", true, true);
        checkPair("13", "-0", true, true);
        checkPair("14", "01234", true, true);
        checkPair("15", "-01234", true, true);
        checkPair("16", "0xABC123", true, true);
        checkPair("17", "0x0", true, true);
        checkPair("19", "123.4E21D", true, true);
        checkPair("20", "-221.23F", true, true);
        checkPair("21", "22338L", true, true);

        checkPair("1-Neg", null, false, false);
        checkPair("2-Neg", "", false, false);
        checkPair("3-Neg", "--2.3", false, false);
        checkPair("4-Neg", ".12.3", false, false);
        checkPair("5-Neg", "-123E", false, false);
        checkPair("6-Neg", "-123E+-212", false, false);
        checkPair("7-Neg", "-123E2.12", false, false);
        checkPair("8-Neg", "0xGF", false, false);
        checkPair("9-Neg", "0xFAE-1", false, false);
        checkPair("10-Neg", ".", false, false);
        checkPair("11-Neg", "-0ABC123", false, false);
        checkPair("12-Neg", "123.4E-D", false, false);
        checkPair("13-Neg", "123.4ED", false, false);
        checkPair("14-Neg", "1234E5l", false, false);
        checkPair("15-Neg", "11a", false, false);
        checkPair("16-Neg", "1a", false, false);
        checkPair("17-Neg", "a", false, false);
        checkPair("18-Neg", "11g", false, false);
        checkPair("19-Neg", "11z", false, false);
        checkPair("20-Neg", "11def", false, false);
        checkPair("21-Neg", "11d11", false, false);
        checkPair("22-Neg", "11 11", false, false);
        checkPair("23-Neg", " 1111", false, false);
        checkPair("24-Neg", "1111 ", false, false);

        assertBooleanEquals("lifted-isNumber-LANG-521", "NumberUtils.isNumber", true, NumberUtils.isNumber("2."), "2.");
        assertBooleanEquals("lifted-isNumber-LANG-664", "NumberUtils.isNumber", false, NumberUtils.isNumber("1.1L"), "1.1L");

        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);
        checkCreateNumber(fuzz);

        int n = data.consumeInt(-1000000, 1000000);
        String canonicalInt = Integer.toString(n);
        boolean intIsNumber = NumberUtils.isNumber(canonicalInt);
        boolean intCreateNumber = checkCreateNumber(canonicalInt);
        if (!intIsNumber) {
            throw new RuntimeException("[oracle:canonical-int] metamorphic violation: canonical decimal integer must be recognized input="
                    + canonicalInt + " lhs=" + intIsNumber + " rhs=true");
        }
        if (!intCreateNumber) {
            throw new RuntimeException("[oracle:canonical-int-create] metamorphic violation: createNumber must accept canonical decimal integer input="
                    + canonicalInt + " lhs=" + intCreateNumber + " rhs=true");
        }

        int m = data.consumeInt(0, 1000000);
        String digits = Integer.toString(m);
        String longQualified = digits + "L";
        String decimalLongQualified = digits + ".0L";

        NumberUtils.isNumber(longQualified);
        NumberUtils.isNumber(decimalLongQualified);
        checkCreateNumber(longQualified);
        checkCreateNumber(decimalLongQualified);

        /* Contract from NumberUtils.isNumber code shown above:
           "not allowing L with an exponent or decimal point".
           Therefore for any non-negative canonical digit string, adding 'L' preserves numeric status,
           while adding ".0L" must make it non-numeric. A patch that merely bypasses the buggy branch
           or deletes the decimal-point guard would violate this observable post-condition without throwing. */
        boolean longOk = NumberUtils.isNumber(longQualified);
        if (!longOk) {
            throw new RuntimeException("[oracle:long-suffix-accepted] metamorphic violation: integer with L suffix should be numeric input="
                    + longQualified + " lhs=" + longOk + " rhs=true");
        }
        boolean decimalLongRejected = NumberUtils.isNumber(decimalLongQualified);
        if (decimalLongRejected) {
            throw new RuntimeException("[oracle:decimal-long-rejected] metamorphic violation: decimal with L suffix must be rejected input="
                    + decimalLongQualified + " lhs=" + decimalLongRejected + " rhs=false");
        }

        String exponentLongQualified = digits + "E1L";
        boolean exponentLongRejected = NumberUtils.isNumber(exponentLongQualified);
        if (exponentLongRejected) {
            throw new RuntimeException("[oracle:exponent-long-rejected] metamorphic violation: exponent form with L suffix must be rejected input="
                    + exponentLongQualified + " lhs=" + exponentLongRejected + " rhs=false");
        }
    }
}