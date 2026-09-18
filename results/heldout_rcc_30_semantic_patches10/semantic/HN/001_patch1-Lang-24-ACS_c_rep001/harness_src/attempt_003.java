package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] POSITIVE_VALS = new String[] {
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
        "22338L"
    };

    private static final String[] NEGATIVE_VALS = new String[] {
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
        "1111 "
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        for (int i = 0; i < POSITIVE_VALS.length; i++) {
            String val = POSITIVE_VALS[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-positive] semantic mismatch: NumberUtils.isNumber(\"" + escape(val) + "\") expected=true actual=false index=" + i);
            }

            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (!actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-positive] semantic mismatch: checkCreateNumber(\"" + escape(val) + "\") expected=true actual=false index=" + i);
            }
        }

        for (int i = 0; i < NEGATIVE_VALS.length; i++) {
            String val = NEGATIVE_VALS[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-negative] semantic mismatch: NumberUtils.isNumber(" + printable(val) + ") expected=false actual=true index=" + i);
            }

            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-negative] semantic mismatch: checkCreateNumber(" + printable(val) + ") expected=false actual=true index=" + i);
            }
        }

        if (!NumberUtils.isNumber("2.")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang521] semantic mismatch: NumberUtils.isNumber(\"2.\") expected=true actual=false");
        }

        if (NumberUtils.isNumber("1.1L")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang664] semantic mismatch: NumberUtils.isNumber(\"1.1L\") expected=false actual=true");
        }

        int n = data.consumeInt(-1_000_000, 1_000_000);
        String canonicalInt = Integer.toString(n);
        if (!NumberUtils.isNumber(canonicalInt)) {
            throw new RuntimeException(
                "[oracle:meta-canonical-int] metamorphic violation: canonical decimal integer strings must be accepted input=" + canonicalInt + " lhs=" + NumberUtils.isNumber(canonicalInt) + " rhs=true");
        }
        if (!checkCreateNumber(canonicalInt)) {
            throw new RuntimeException(
                "[oracle:meta-canonical-int-create] metamorphic violation: createNumber must parse canonical decimal integer strings input=" + canonicalInt + " lhs=" + checkCreateNumber(canonicalInt) + " rhs=true");
        }

        int whole = data.consumeInt(0, 1_000_000);
        int frac = data.consumeInt(0, 1_000_000);
        String suffix = data.consumeBoolean() ? "L" : "l";
        String decimalLongLike = whole + "." + frac + suffix;

        boolean observed = NumberUtils.isNumber(decimalLongLike);
        // Contract cited from the patched method's own comment: "not allowing L with an exponent or decimal point".
        // A throw-deleting or guard-only patch that leaves decimal-point+L inputs accepted would violate this observable result.
        if (observed) {
            throw new RuntimeException(
                "[oracle:meta-decimal-long] metamorphic violation: decimal-point numbers with L/l suffix must be rejected input=" + decimalLongLike + " lhs=true rhs=false");
        }

        try {
            NumberUtils.createNumber(decimalLongLike);
        } catch (RuntimeException ignored) {
        }

        String arbitrary = data.consumeString(64);
        NumberUtils.isNumber(arbitrary);
        try {
            NumberUtils.createNumber(arbitrary);
        } catch (RuntimeException ignored) {
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

    private static String printable(String s) {
        return s == null ? "null" : "\"" + escape(s) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r").replace("\"", "\\\"");
    }
}