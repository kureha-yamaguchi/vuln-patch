package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test.
        try {
            String anchored = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchored)) {
                throw new RuntimeException(
                    "[oracle:anchor-eq] metamorphic violation: exact trigger should return original string input=0123456789 lower=15 upper=20 lhs="
                        + String.valueOf(anchored) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInAbbreviate(t)) {
                throw t;
            }
        }

        // EXPLORE: varied REAL inputs with the same root-cause property:
        // lower is at or beyond str.length(), and upper is -1 or beyond str.length().
        // We build no-space strings by construction so StringUtils.indexOf(str, " ", lower) is -1,
        // making the documented/clamped substring path observable.
        String base = data.consumeAsciiString(64);
        if (base == null) {
            return;
        }
        String noSpace = stripSpaces(base);
        if (noSpace.length() == 0) {
            noSpace = "A";
        }

        int len = noSpace.length();
        int lower = len + data.consumeInt(0, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(16);

        try {
            String result = WordUtils.abbreviate(noSpace, lower, upper, append);

            // Post-condition from the method contract visible in the provided code/comments:
            // if upper == -1 or upper > str.length(), upper is set to str.length();
            // if no space is found, the method appends str.substring(0, upper), and only appends
            // appendToEnd when upper != str.length().
            // For a no-space string with lower >= str.length() and upper == -1 or > str.length(),
            // a correct implementation must therefore return the original string unchanged.
            // A throw-deleting or branch-skipping patch that avoids the crash by altering behavior
            // would violate this observable result.
            if (!noSpace.equals(result)) {
                throw new RuntimeException(
                    "[oracle:nospace-identity] metamorphic violation: no-space string with lower>=len and upper out of range must return original string input="
                        + quote(noSpace) + " lower=" + lower + " upper=" + upper + " append="
                        + quote(append) + " lhs=" + quote(result) + " rhs=" + quote(noSpace));
            }

            // Equivalent-input relation on the same documented guarantee:
            // for these constructed inputs, upper == -1 and upper > len are both specified to clamp
            // to str.length(), so they must agree.
            try {
                String resultUnlimited = WordUtils.abbreviate(noSpace, lower, -1, append);
                if (!result.equals(resultUnlimited)) {
                    throw new RuntimeException(
                        "[oracle:upper-clamp-agree] metamorphic violation: upper=-1 and upper>len should agree after clamping input="
                            + quote(noSpace) + " lower=" + lower + " upper=" + upper + " append="
                            + quote(append) + " lhs=" + quote(result) + " rhs=" + quote(resultUnlimited));
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCauseInAbbreviate(t) && isValidByConstruction(noSpace, lower, -1)) {
                    throw t;
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInAbbreviate(t) && isValidByConstruction(noSpace, lower, upper)) {
                throw t;
            }
        }
    }

    private static boolean isValidByConstruction(String s, int lower, int upper) {
        if (s == null || s.length() == 0) {
            return false;
        }
        if (s.indexOf(' ') != -1) {
            return false;
        }
        if (lower < s.length()) {
            return false;
        }
        return upper == -1 || upper > s.length();
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseInAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String stripSpaces(String s) {
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}