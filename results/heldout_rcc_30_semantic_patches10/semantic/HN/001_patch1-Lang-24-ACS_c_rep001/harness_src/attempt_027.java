package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void requireTrue(String oracleId, String what, boolean actual) {
        if (!actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected true for " + what + " actual=false");
        }
    }

    private static void requireFalse(String oracleId, String what, boolean actual) {
        if (actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected false for " + what + " actual=true");
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };

        String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            requireTrue("lifted-isNumber-pos-" + (i + 1), "NumberUtils.isNumber(\"" + val + "\")", NumberUtils.isNumber(val));
            requireTrue("lifted-createNumber-pos-" + (i + 1), "checkCreateNumber(\"" + val + "\")", checkCreateNumber(val));
        }

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            String rendered = val == null ? "null" : "\"" + val + "\"";
            requireFalse("lifted-isNumber-neg-" + (i + 1), "NumberUtils.isNumber(" + rendered + ")", NumberUtils.isNumber(val));
            requireFalse("lifted-createNumber-neg-" + (i + 1), "checkCreateNumber(" + rendered + ")", checkCreateNumber(val));
        }

        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);

        String fuzzAscii = data.consumeAsciiString(64);
        NumberUtils.isNumber(fuzzAscii);

        int n = data.consumeInt(-1_000_000, 1_000_000);
        String canonicalInt = Integer.toString(n);
        String upperLong = canonicalInt + "L";
        String lowerLong = canonicalInt + "l";

        boolean upperIsNumber;
        boolean lowerIsNumber;
        try {
            upperIsNumber = NumberUtils.isNumber(upperLong);
            lowerIsNumber = NumberUtils.isNumber(lowerLong);
        } catch (Throwable t) {
            return;
        }

        /* Contract/invariant used: the shown implementation explicitly accepts both 'l' and 'L'
           in the same branch (`chars[i] == 'l' || chars[i] == 'L'`), so case of the long suffix
           must not change the result. A throw-deleting or guard-only patch in that branch can make
           one suffix behave differently or allow/disallow the wrong form silently. */
        if (upperIsNumber != lowerIsNumber) {
            throw new RuntimeException(
                "[oracle:metamorphic-suffix-case] metamorphic violation: isNumber must treat l/L equivalently input=" +
                canonicalInt + " lhs=" + upperLong + ":" + upperIsNumber + " rhs=" + lowerLong + ":" + lowerIsNumber);
        }

        /* Documented by the in-code comment in the shown method: 'not allowing L with an exponent or decimal point'.
           Therefore a plain canonical integer with an L suffix is allowed, while adding a decimal point before L is not. */
        if (!upperIsNumber) {
            throw new RuntimeException(
                "[oracle:metamorphic-canonical-long] metamorphic violation: canonical integer plus L must be recognized input=" +
                upperLong + " lhs=" + upperIsNumber + " rhs=true");
        }

        String decimalLong = canonicalInt + ".0L";
        boolean decimalLongIsNumber;
        try {
            decimalLongIsNumber = NumberUtils.isNumber(decimalLong);
        } catch (Throwable t) {
            return;
        }
        if (decimalLongIsNumber) {
            throw new RuntimeException(
                "[oracle:metamorphic-decimal-long] metamorphic violation: decimal form with L must be rejected input=" +
                decimalLong + " lhs=" + decimalLongIsNumber + " rhs=false");
        }

        try {
            Object parsedUpper = NumberUtils.createNumber(upperLong);
            Object parsedLower = NumberUtils.createNumber(lowerLong);
            if (parsedUpper == null || parsedLower == null) {
                throw new RuntimeException(
                    "[oracle:metamorphic-createNumber-long] metamorphic violation: createNumber must parse canonical long suffix forms input=" +
                    canonicalInt + " lhs=" + parsedUpper + " rhs=" + parsedLower);
            }
        } catch (Throwable t) {
            return;
        }
    }
}