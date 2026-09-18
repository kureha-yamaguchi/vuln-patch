package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test / ground truth.
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
        }

        // EXPLORE:
        // Build valid-by-construction inputs for the documented/visible contract in abbreviate:
        // when upper == -1 or upper > str.length(), upper is treated as str.length().
        // For strings with no spaces, abbreviate(str, lower, hugeUpper, append) must therefore
        // agree with abbreviate(str, lower, -1, append): both normalize upper to length and
        // return the full string unchanged.
        String s = data.consumeAsciiString(64);
        if (s.length() == 0) {
            s = "A";
        }
        s = s.replace(' ', 'X');

        int len = s.length();
        int lower = len + data.consumeInt(0, 32);
        int upper = len + 1 + data.consumeInt(0, 32);

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(16);

        String lhs;
        try {
            lhs = WordUtils.abbreviate(s, lower, upper, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
            return;
        }

        String rhs;
        try {
            rhs = WordUtils.abbreviate(s, lower, -1, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
            return;
        }

        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:abbr-upper-normalization] metamorphic violation: abbreviate(str, lower, upper>len, append) " +
                "must agree with abbreviate(str, lower, -1, append) on no-space strings " +
                "input={str=" + quote(s) + ", lower=" + lower + ", upper=" + upper + ", append=" + quote(append) + "} " +
                "lhs=" + quote(lhs) + " rhs=" + quote(rhs)
            );
        }

        if (!safeEquals(lhs, s)) {
            throw new RuntimeException(
                "[oracle:abbr-full-string] metamorphic violation: with no spaces and upper normalized to len, result must be the original string " +
                "input={str=" + quote(s) + ", lower=" + lower + ", upper=" + upper + ", append=" + quote(append) + "} " +
                "lhs=" + quote(lhs) + " expected=" + quote(s)
            );
        }
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
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}