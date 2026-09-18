package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression input from WordUtilsTest.testAbbreviate.
        // This is valid by construction according to the test's documented expected result:
        // abbreviate("0123456789", 15, 20, null) must return "0123456789".
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (Throwable t) {
            if (isOracleViolation(t)) {
                throw (RuntimeException) t;
            }
            if (isValidationThrowable(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t) && isValidByConstruction("0123456789", 15, 20)) {
                throw propagate(t);
            }
            return;
        }

        String s = buildNonEmptyString(data);
        int len = s.length();

        // EXPLORE: generate many real inputs satisfying the root-cause property:
        // upper is -1 or exceeds str.length(), especially with lower at or beyond str.length().
        int lower;
        if (data.consumeBoolean()) {
            lower = len + data.consumeInt(0, 32);
        } else {
            lower = data.consumeInt(0, len + 32);
        }

        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String appendToEnd = data.consumeBoolean() ? null : data.consumeString(8);

        try {
            WordUtils.abbreviate(s, lower, upper, appendToEnd);
        } catch (Throwable t) {
            if (isOracleViolation(t)) {
                throw (RuntimeException) t;
            }
            if (isValidationThrowable(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t) && isValidByConstruction(s, lower, upper)) {
                throw propagate(t);
            }
            return;
        }

        // Metamorphic oracle:
        // Contract visible in the method body/comments: "if the upper value is -1
        // (i.e. no limit) or is greater than the length of the string, set to the
        // length of the string". Therefore, for the same (str, lower, appendToEnd),
        // replacing upper=-1 with any upper>str.length() must produce the same result.
        // A patch that merely deletes/guards the crashing path but returns a wrong
        // substring would violate this observable equivalence without throwing.
        int lowerForOracle = len + data.consumeInt(0, 32);
        int beyondLenUpper = len + data.consumeInt(1, 32);
        String oracleAppend = data.consumeBoolean() ? null : data.consumeString(8);

        String lhs;
        String rhs;
        try {
            lhs = WordUtils.abbreviate(s, lowerForOracle, -1, oracleAppend);
        } catch (Throwable t) {
            if (isValidationThrowable(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t) && isValidByConstruction(s, lowerForOracle, -1)) {
                throw propagate(t);
            }
            return;
        }
        try {
            rhs = WordUtils.abbreviate(s, lowerForOracle, beyondLenUpper, oracleAppend);
        } catch (Throwable t) {
            if (isValidationThrowable(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t) && isValidByConstruction(s, lowerForOracle, beyondLenUpper)) {
                throw propagate(t);
            }
            return;
        }

        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:upper-clamp] metamorphic violation: abbreviate(str, lower, -1, append) must equal abbreviate(str, lower, upper>len, append)"
                    + " input=" + printable(s)
                    + " lower=" + lowerForOracle
                    + " upper=" + beyondLenUpper
                    + " append=" + printable(oracleAppend)
                    + " lhs=" + printable(lhs)
                    + " rhs=" + printable(rhs));
        }
    }

    private static String buildNonEmptyString(FuzzedDataProvider data) {
        String a = data.consumeString(24);
        String b = data.consumeAsciiString(24);
        String s;
        if (data.consumeBoolean()) {
            s = a + " " + b;
        } else if (data.consumeBoolean()) {
            s = a + b;
        } else {
            s = b + " " + a;
        }
        if (s == null || s.length() == 0) {
            s = "A B";
        }
        return s;
    }

    private static boolean isRootCauseFromAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                    && "abbreviate".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidationThrowable(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isValidByConstruction(String s, int lower, int upper) {
        if (s == null || s.length() == 0) {
            return false;
        }
        if (lower < 0) {
            return false;
        }
        return upper == -1 || upper > s.length();
    }

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String printable(String s) {
        return String.valueOf(s);
    }
}