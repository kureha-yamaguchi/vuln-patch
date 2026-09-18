package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        String raw = data.consumeString(64);
        String append = data.consumeBoolean() ? null : data.consumeString(8);

        String noSpace = normalizeNoSpace(raw);
        if (noSpace.length() == 0) {
            noSpace = "A";
        }

        int len = noSpace.length();
        int lower = len + data.consumeInt(0, 32);
        int upperChoice = data.consumeInt(0, 4);
        int upper;
        if (upperChoice == 0) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(0, 32);
        }

        try {
            String result = WordUtils.abbreviate(noSpace, lower, upper, append);

            /*
             * Contract asserted:
             * For abbreviate, values beyond the string length are normalized to the length
             * ("if the lower value is greater than the length of the string, set to the length
             * of the string" and "if the upper value is -1 or is greater than the length of the
             * string, set to the length of the string"). For a string with no spaces, indexOf(..., lower)
             * must therefore act as "no space found", causing substring(0, str.length()) and, because
             * no abbreviation occurred, appendToEnd must not be appended.
             *
             * A throw-deleting or branch-skipping patch that avoids the crash but returns a truncated
             * string or appends appendToEnd would violate this observable result.
             */
            if (lower >= len && (upper == -1 || upper >= len) && !noSpace.equals(result)) {
                throw new RuntimeException(
                    "[oracle:abbrev-full] metamorphic violation: expected full string when bounds exceed length"
                        + " input=" + printable(noSpace)
                        + " lower=" + lower
                        + " upper=" + upper
                        + " append=" + printable(append)
                        + " lhs=" + printable(result)
                        + " rhs=" + printable(noSpace));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
            return;
        }

        String arbitrary = data.consumeRemainingAsString();
        if (arbitrary == null) {
            return;
        }
        int arbLen = arbitrary.length();
        int arbLower = arbLen + 1 + Math.abs(data.consumeInt() % 16);
        int arbUpper = data.consumeBoolean() ? -1 : arbLen + 1 + Math.abs(data.consumeInt() % 16);
        try {
            WordUtils.abbreviate(arbitrary, arbLower, arbUpper, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t) && arbLen > 0) {
                throw t;
            }
        }
    }

    private static void anchor() {
        try {
            String result = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(result)) {
                throw new RuntimeException(
                    "[oracle:anchor-exact] metamorphic violation: exact regression input returned wrong value"
                        + " input=0123456789 lower=15 upper=20 append=null"
                        + " lhs=" + printable(result)
                        + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
        }
    }

    private static boolean isRootCauseFromAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
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
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName().toLowerCase();
        return name.contains("invalid") || name.contains("validation");
    }

    private static String normalizeNoSpace(String s) {
        if (s == null) {
            return "A";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                sb.append('X');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}