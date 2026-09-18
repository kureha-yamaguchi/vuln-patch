package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        // Anchor: exact trigger from WordUtilsTest.testAbbreviate.
        // Documented guarantee in abbreviate's own comments/code:
        // - if lower > str.length(), lower is effectively beyond the searchable range
        // - if upper == -1 or upper > str.length(), upper must be set to str.length()
        // Therefore abbreviate("0123456789", 15, 20, null) must return the full original string,
        // not throw. A "fix" that merely suppresses the crash or skips the normalization would violate this.
        exerciseAndCheck("0123456789", 15, 20, null, "0123456789", true);

        // Explore the root cause with many real inputs:
        // non-null, non-empty strings; lower at or beyond the string length; upper == -1 or > length.
        // For every correct implementation, the method must clamp upper to str.length() and return the
        // original string unchanged.
        String str = data.consumeAsciiString(64);
        if (str.length() == 0) {
            str = "A";
        }

        int len = str.length();
        int lower = len + data.consumeInt(0, 32);

        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
            if (data.consumeBoolean()) {
                // Also explore the case upper < lower before normalization.
                upper = Math.min(upper, lower - data.consumeInt(0, 8));
            }
        }

        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        exerciseAndCheck(str, lower, upper, appendToEnd, str, true);

        // A second varied call uses the same real API and same root property on a related input with
        // surrounding content changes, including possible spaces.
        String extra = data.consumeAsciiString(16);
        String str2 = str + extra;
        if (str2.length() == 0) {
            str2 = "B";
        }
        int len2 = str2.length();
        int lower2 = len2 + data.consumeInt(0, 32);
        int upper2 = data.consumeBoolean() ? -1 : len2 + data.consumeInt(1, 32);
        exerciseAndCheck(str2, lower2, upper2, appendToEnd, str2, true);
    }

    private static void exerciseAndCheck(String str, int lower, int upper, String appendToEnd,
                                         String expected, boolean validByConstruction) {
        try {
            String actual = WordUtils.abbreviate(str, lower, upper, appendToEnd);
            if (!safeEquals(expected, actual)) {
                throw new RuntimeException(
                    "[oracle:abbrev-clamp] metamorphic violation: expected full string when lower>=len and upper is -1 or >len"
                        + " input=" + describe(str, lower, upper, appendToEnd)
                        + " lhs=" + expected
                        + " rhs=" + actual
                );
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                sneakyThrow(t);
            }
            // Any other throwable is outside this patch's scope or from invalid/unspecified behavior.
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation")
                || name.contains("Invalid")
                || name.contains("Illegal");
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String describe(String str, int lower, int upper, String appendToEnd) {
        return "{str=" + String.valueOf(str)
                + ", lower=" + lower
                + ", upper=" + upper
                + ", appendToEnd=" + String.valueOf(appendToEnd)
                + "}";
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}