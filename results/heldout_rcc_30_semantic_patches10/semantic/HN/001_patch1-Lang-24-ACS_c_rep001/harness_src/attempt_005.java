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

    private static void requireTrue(String oracleId, String description, boolean actual) {
        if (!actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + description + " expected=true actual=false");
        }
    }

    private static void requireFalse(String oracleId, String description, boolean actual) {
        if (actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + description + " expected=false actual=true");
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
            requireTrue("lifted-isNumber-pos-" + i, "NumberUtils.isNumber(\"" + val + "\")", NumberUtils.isNumber(val));
            requireTrue("lifted-createNumber-pos-" + i, "checkCreateNumber(\"" + val + "\")", checkCreateNumber(val));
        }

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            String shown = String.valueOf(val);
            requireFalse("lifted-isNumber-neg-" + i, "NumberUtils.isNumber(" + shown + ")", NumberUtils.isNumber(val));
            requireFalse("lifted-createNumber-neg-" + i, "checkCreateNumber(" + shown + ")", checkCreateNumber(val));
        }

        String fuzz = data.consumeString(32);
        NumberUtils.isNumber(fuzz);

        String digits1 = data.consumeAsciiString(12);
        String digits2 = data.consumeAsciiString(12);
        if (digits1.length() == 0) {
            digits1 = "1";
        }
        if (digits2.length() == 0) {
            digits2 = "0";
        }
        digits1 = digitsOnly(digits1);
        digits2 = digitsOnly(digits2);
        if (digits1.length() == 0) {
            digits1 = "1";
        }
        if (digits2.length() == 0) {
            digits2 = "0";
        }

        String longLiteral = digits1 + "L";
        try {
            boolean actualLongLiteral = NumberUtils.isNumber(longLiteral);
            boolean actualCreateLongLiteral = checkCreateNumber(longLiteral);
            if (!actualLongLiteral) {
                throw new RuntimeException(
                    "[oracle:constructed-long-literal] metamorphic violation: constructed canonical long literal must be accepted input="
                        + longLiteral + " lhs=" + actualLongLiteral + " rhs=true");
            }
            if (!actualCreateLongLiteral) {
                throw new RuntimeException(
                    "[oracle:constructed-long-create] metamorphic violation: createNumber must accept the same constructed canonical long literal input="
                        + longLiteral + " lhs=" + actualCreateLongLiteral + " rhs=true");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        String decimalLongLiteral = digits1 + "." + digits2 + "L";
        try {
            /*
             * Contract justification: the shown NumberUtils.isNumber implementation documents
             * at the patched branch "not allowing L with an exponent or decimal point".
             * Therefore any constructed non-degenerate decimal literal ending in L must be rejected.
             * A patch that merely deletes or bypasses the patched check would incorrectly return true.
             */
            boolean actualDecimalLong = NumberUtils.isNumber(decimalLongLiteral);
            if (actualDecimalLong) {
                throw new RuntimeException(
                    "[oracle:decimal-long-forbidden] metamorphic violation: decimal literal with trailing L must be rejected input="
                        + decimalLongLiteral + " lhs=" + actualDecimalLong + " rhs=false");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static String digitsOnly(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}