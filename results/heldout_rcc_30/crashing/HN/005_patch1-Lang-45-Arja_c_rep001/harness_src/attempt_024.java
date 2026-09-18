package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        String fuzz = data.consumeAsciiString(64);
        if (fuzz == null) {
            return;
        }

        String noSpaces = fuzz.replace(" ", "");
        if (noSpaces.length() == 0) {
            noSpaces = "A";
        }

        int len = noSpaces.length();
        int lower = len + data.consumeInt(0, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(0, 32);
        }
        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        callAndCheckValid(noSpaces, lower, upper, appendToEnd);

        String mixed = data.consumeString(64);
        if (mixed == null) {
            return;
        }
        if (mixed.length() == 0) {
            mixed = "B";
        }
        int mlen = mixed.length();
        int lower2 = mlen + data.consumeInt(0, 32);
        int upper2 = data.consumeBoolean() ? -1 : mlen + data.consumeInt(0, 32);
        try {
            WordUtils.abbreviate(mixed, lower2, upper2, appendToEnd);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                return;
            }
        }
    }

    private static void anchor() {
        try {
            String out = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(out)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input should abbreviate to original string input=0123456789 lower=15 upper=20 lhs=" + String.valueOf(out) + " rhs=0123456789");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void callAndCheckValid(String str, int lower, int upper, String appendToEnd) {
        try {
            String out = WordUtils.abbreviate(str, lower, upper, appendToEnd);

            /*
             * Contract used for this oracle:
             * - shown code/comments say: if upper == -1 or upper > str.length(), upper is set to str.length()
             * - then if no space is found from lower, result appends str.substring(0, upper)
             * - appendToEnd is appended only if upper != str.length()
             * Therefore, for a non-empty string with no spaces and lower >= str.length(),
             * any correct implementation must return the original string exactly when upper is -1
             * or greater than the string length. A patch that merely suppresses the crash but
             * loses this normalization would violate this observable result.
             */
            if (!str.equals(out)) {
                throw new RuntimeException("[oracle:abbrev-no-space] metamorphic violation: no-space string with lower>=len and upper out of range must return original string input=" + str + " lower=" + lower + " upper=" + upper + " append=" + String.valueOf(appendToEnd) + " lhs=" + String.valueOf(out) + " rhs=" + str);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("invalid") || lower.contains("validation")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
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
}