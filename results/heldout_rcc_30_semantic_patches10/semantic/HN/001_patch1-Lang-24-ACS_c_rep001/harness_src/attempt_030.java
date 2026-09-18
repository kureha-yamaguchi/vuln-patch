package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
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
            boolean actual = NumberUtils.isNumber(val);
            if (!actual) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleIdForPositiveIsNumber(i) + " semantic mismatch: NumberUtils.isNumber(\"" + val + "\") expected=true actual=false");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (!actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleIdForPositiveCreateNumber(i) + " semantic mismatch: checkCreateNumber(\"" + val + "\") expected=true actual=false");
            }
        }

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actual = NumberUtils.isNumber(val);
            if (actual) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleIdForNegativeIsNumber(i) + " semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=false actual=true");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleIdForNegativeCreateNumber(i) + " semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=false actual=true");
            }
        }

        // Contract visible in NumberUtils.isNumber: "not allowing L with an exponent or decimal point".
        // Therefore any canonically constructed decimal-with-long-qualifier string must be rejected.
        int intPart = data.consumeInt(0, 1_000_000);
        int fracPart = data.consumeInt(0, 1_000_000);
        String decimalLongUpper = Integer.toString(intPart) + "." + Integer.toString(fracPart) + "L";
        String decimalLongLower = Integer.toString(intPart) + "." + Integer.toString(fracPart) + "l";
        if (NumberUtils.isNumber(decimalLongUpper)) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper] metamorphic violation: decimal-point number with 'L' qualifier must be rejected input="
                    + quote(decimalLongUpper) + " actual=true");
        }
        if (NumberUtils.isNumber(decimalLongLower)) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower] metamorphic violation: decimal-point number with 'l' qualifier must be rejected input="
                    + quote(decimalLongLower) + " actual=true");
        }

        // Same documented guarantee as above, exercised with an exponent as well.
        int mantissa = data.consumeInt(0, 1_000_000);
        int exponent = data.consumeInt(0, 1_000_000);
        String expLong = Integer.toString(mantissa) + "E" + Integer.toString(exponent) + "L";
        if (NumberUtils.isNumber(expLong)) {
            throw new RuntimeException(
                "[oracle:exponent-long-suffix] metamorphic violation: exponent number with 'L' qualifier must be rejected input="
                    + quote(expLong) + " actual=true");
        }

        String arbitrary = data.consumeRemainingAsString();
        NumberUtils.isNumber(arbitrary);
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

    private static String oracleIdForPositiveIsNumber(int i) {
        switch (i) {
            case 0: return "[oracle:lifted-isNumber-1]";
            case 1: return "[oracle:lifted-isNumber-2]";
            case 2: return "[oracle:lifted-isNumber-3]";
            case 3: return "[oracle:lifted-isNumber-4]";
            case 4: return "[oracle:lifted-isNumber-5]";
            case 5: return "[oracle:lifted-isNumber-6]";
            case 6: return "[oracle:lifted-isNumber-7]";
            case 7: return "[oracle:lifted-isNumber-8]";
            case 8: return "[oracle:lifted-isNumber-9]";
            case 9: return "[oracle:lifted-isNumber-10]";
            case 10: return "[oracle:lifted-isNumber-11]";
            case 11: return "[oracle:lifted-isNumber-12]";
            case 12: return "[oracle:lifted-isNumber-13]";
            case 13: return "[oracle:lifted-isNumber-14]";
            case 14: return "[oracle:lifted-isNumber-15]";
            case 15: return "[oracle:lifted-isNumber-16]";
            case 16: return "[oracle:lifted-isNumber-17]";
            case 17: return "[oracle:lifted-isNumber-19]";
            case 18: return "[oracle:lifted-isNumber-20]";
            case 19: return "[oracle:lifted-isNumber-21]";
            case 20: return "[oracle:lifted-isNumber-LANG-521]";
            default: return "[oracle:lifted-isNumber-pos]";
        }
    }

    private static String oracleIdForPositiveCreateNumber(int i) {
        switch (i) {
            case 0: return "[oracle:lifted-createNumber-1]";
            case 1: return "[oracle:lifted-createNumber-2]";
            case 2: return "[oracle:lifted-createNumber-3]";
            case 3: return "[oracle:lifted-createNumber-4]";
            case 4: return "[oracle:lifted-createNumber-5]";
            case 5: return "[oracle:lifted-createNumber-6]";
            case 6: return "[oracle:lifted-createNumber-7]";
            case 7: return "[oracle:lifted-createNumber-8]";
            case 8: return "[oracle:lifted-createNumber-9]";
            case 9: return "[oracle:lifted-createNumber-10]";
            case 10: return "[oracle:lifted-createNumber-11]";
            case 11: return "[oracle:lifted-createNumber-12]";
            case 12: return "[oracle:lifted-createNumber-13]";
            case 13: return "[oracle:lifted-createNumber-14]";
            case 14: return "[oracle:lifted-createNumber-15]";
            case 15: return "[oracle:lifted-createNumber-16]";
            case 16: return "[oracle:lifted-createNumber-17]";
            case 17: return "[oracle:lifted-createNumber-19]";
            case 18: return "[oracle:lifted-createNumber-20]";
            case 19: return "[oracle:lifted-createNumber-21]";
            case 20: return "[oracle:lifted-createNumber-LANG-521]";
            default: return "[oracle:lifted-createNumber-pos]";
        }
    }

    private static String oracleIdForNegativeIsNumber(int i) {
        switch (i) {
            case 0: return "[oracle:lifted-isNumber-1-Neg]";
            case 1: return "[oracle:lifted-isNumber-2-Neg]";
            case 2: return "[oracle:lifted-isNumber-3-Neg]";
            case 3: return "[oracle:lifted-isNumber-4-Neg]";
            case 4: return "[oracle:lifted-isNumber-5-Neg]";
            case 5: return "[oracle:lifted-isNumber-6-Neg]";
            case 6: return "[oracle:lifted-isNumber-7-Neg]";
            case 7: return "[oracle:lifted-isNumber-8-Neg]";
            case 8: return "[oracle:lifted-isNumber-9-Neg]";
            case 9: return "[oracle:lifted-isNumber-10-Neg]";
            case 10: return "[oracle:lifted-isNumber-11-Neg]";
            case 11: return "[oracle:lifted-isNumber-12-Neg]";
            case 12: return "[oracle:lifted-isNumber-13-Neg]";
            case 13: return "[oracle:lifted-isNumber-14-Neg]";
            case 14: return "[oracle:lifted-isNumber-15-Neg]";
            case 15: return "[oracle:lifted-isNumber-16-Neg]";
            case 16: return "[oracle:lifted-isNumber-17-Neg]";
            case 17: return "[oracle:lifted-isNumber-18-Neg]";
            case 18: return "[oracle:lifted-isNumber-19-Neg]";
            case 19: return "[oracle:lifted-isNumber-20-Neg]";
            case 20: return "[oracle:lifted-isNumber-21-Neg]";
            case 21: return "[oracle:lifted-isNumber-22-Neg]";
            case 22: return "[oracle:lifted-isNumber-23-Neg]";
            case 23: return "[oracle:lifted-isNumber-24-Neg]";
            case 24: return "[oracle:lifted-isNumber-LANG-664]";
            default: return "[oracle:lifted-isNumber-neg]";
        }
    }

    private static String oracleIdForNegativeCreateNumber(int i) {
        switch (i) {
            case 0: return "[oracle:lifted-createNumber-1-Neg]";
            case 1: return "[oracle:lifted-createNumber-2-Neg]";
            case 2: return "[oracle:lifted-createNumber-3-Neg]";
            case 3: return "[oracle:lifted-createNumber-4-Neg]";
            case 4: return "[oracle:lifted-createNumber-5-Neg]";
            case 5: return "[oracle:lifted-createNumber-6-Neg]";
            case 6: return "[oracle:lifted-createNumber-7-Neg]";
            case 7: return "[oracle:lifted-createNumber-8-Neg]";
            case 8: return "[oracle:lifted-createNumber-9-Neg]";
            case 9: return "[oracle:lifted-createNumber-10-Neg]";
            case 10: return "[oracle:lifted-createNumber-11-Neg]";
            case 11: return "[oracle:lifted-createNumber-12-Neg]";
            case 12: return "[oracle:lifted-createNumber-13-Neg]";
            case 13: return "[oracle:lifted-createNumber-14-Neg]";
            case 14: return "[oracle:lifted-createNumber-15-Neg]";
            case 15: return "[oracle:lifted-createNumber-16-Neg]";
            case 16: return "[oracle:lifted-createNumber-17-Neg]";
            case 17: return "[oracle:lifted-createNumber-18-Neg]";
            case 18: return "[oracle:lifted-createNumber-19-Neg]";
            case 19: return "[oracle:lifted-createNumber-20-Neg]";
            case 20: return "[oracle:lifted-createNumber-21-Neg]";
            case 21: return "[oracle:lifted-createNumber-22-Neg]";
            case 22: return "[oracle:lifted-createNumber-23-Neg]";
            case 23: return "[oracle:lifted-createNumber-24-Neg]";
            case 24: return "[oracle:lifted-createNumber-LANG-664]";
            default: return "[oracle:lifted-createNumber-neg]";
        }
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}