package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Contract asserted below comes directly from the method's own comments:
        // - "if the lower value is greater than the length of the string, set to the length of the string"
        // - "if the upper value is -1 ... or is greater than the length of the string, set to the length of the string"
        // For any non-null, non-empty string built that way, a correct implementation must return the original
        // string unchanged (substring(0, str.length()) with no append). A patch that merely suppresses the crash
        // or skips the bookkeeping would violate this observable post-condition.

        // ANCHOR: exact regression-test trigger first.
        try {
            String out = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(out)) {
                throw new RuntimeException(
                    "[oracle:anchor] metamorphic violation: documented clamp-to-length contract input=0123456789,15,20,null lhs="
                        + out + " rhs=0123456789");
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }

        // EXPLORE: varied real inputs satisfying the same root-cause property:
        // non-null, non-empty string; lower beyond length; upper == -1 or beyond length.
        String str = data.consumeAsciiString(64);
        if (str.length() == 0) {
            str = "A";
        }

        // Occasionally add spaces and surrounding content; lower remains beyond length, so correct behavior is unchanged.
        if (data.consumeBoolean()) {
            String extra = data.consumeAsciiString(16);
            if (extra.length() == 0) {
                extra = "B";
            }
            str = data.consumeBoolean() ? (str + " " + extra) : (extra + " " + str);
        }

        int len = str.length();
        int lower = len + data.consumeInt(1, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String out = WordUtils.abbreviate(str, lower, upper, appendToEnd);
            if (!str.equals(out)) {
                throw new RuntimeException(
                    "[oracle:clamp] metamorphic violation: documented clamp-to-length contract input="
                        + printable(str) + "," + lower + "," + upper + "," + printable(appendToEnd)
                        + " lhs=" + printable(out) + " rhs=" + printable(str));
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t == null) {
            return;
        }

        if (isOracleViolation(t)) {
            throw (RuntimeException) t;
        }

        if (isCleanRejection(t)) {
            return;
        }

        if (validByConstruction && t instanceof StringIndexOutOfBoundsException && passesThroughAbbreviate(t)) {
            throw (StringIndexOutOfBoundsException) t;
        }

        // Outside this patch's scope or malformed-input rejection: swallow to avoid false positives.
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException;
    }

    private static boolean passesThroughAbbreviate(Throwable t) {
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

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}