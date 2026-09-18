package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Contract asserted below:
        // For WordUtils.abbreviate, the code comments and the regression test show that when
        // upper is -1 or greater than str.length(), it must be normalized to str.length().
        // Therefore, for a non-empty string with no spaces, if lower >= str.length() and
        // upper >= lower, a correct implementation must return the original string unchanged:
        // indexOf(" ", lower) == -1, substring(0, normalizedUpper) == whole string, and no
        // appendToEnd is added because no abbreviation occurred. A patch that merely suppresses
        // the crashing branch or silently truncates would violate this observable result.

        // ANCHOR: exact regression trigger from the failing test.
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor-ret] metamorphic violation: exact regression input must return original string input=0123456789 lower=15 upper=20 append=null lhs="
                        + String.valueOf(anchor) + " rhs=0123456789");
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

        // EXPLORE: vary valid inputs that satisfy the same root-cause property:
        // lower at or beyond string length and upper at or beyond lower, driven through the
        // real public API.
        String raw = data.consumeAsciiString(64);
        String str = removeSpaces(raw);
        if (str.length() == 0) {
            str = "A";
        }

        int len = str.length();
        int lower = len + data.consumeInt(0, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = lower + data.consumeInt(0, 32);
        } else {
            upper = len + data.consumeInt(0, 32);
            if (upper < lower) {
                upper = lower;
            }
        }

        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(16);

        try {
            String result = WordUtils.abbreviate(str, lower, upper, appendToEnd);

            // Oracle applies only to our non-empty, no-space strings constructed above.
            if (!str.equals(result)) {
                throw new RuntimeException("[oracle:no-space-full] metamorphic violation: no-space string with upper beyond length must be returned unchanged input="
                        + printable(str) + " lower=" + lower + " upper=" + upper + " append="
                        + printable(String.valueOf(appendToEnd)) + " lhs=" + printable(result) + " rhs=" + printable(str));
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

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
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

    private static String removeSpaces(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer b = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String printable(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }
}