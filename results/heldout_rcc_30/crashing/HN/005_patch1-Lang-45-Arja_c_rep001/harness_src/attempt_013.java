package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Contract asserted below:
        // In WordUtils.abbreviate, "if the upper value is -1 (i.e. no limit) or is greater
        // than the length of the string, set to the length of the string", and
        // "only if abbreviation has occured do we append the appendToEnd value".
        // Therefore, for a non-empty string with no spaces, when upper is -1 or >= str.length(),
        // the result must be exactly the original string, independent of appendToEnd.

        // ANCHOR: exact failing test input first.
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException(
                    "[oracle:anchor] metamorphic violation: exact regression input must return original string input=0123456789 lower=15 upper=20 append=null lhs="
                        + String.valueOf(anchor) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCauseFromAbbreviate(t)) {
                throw t;
            }
            return;
        }

        // EXPLORE: generate many real inputs with the triggering property:
        // non-empty strings without spaces, lower beyond the string length, and upper at or beyond
        // the string length (or -1). These are valid-by-construction for the documented clamping behavior.
        String raw = data.consumeAsciiString(64);
        String str = removeSpaces(raw);
        if (str.length() == 0) {
            str = "A";
        }

        int len = str.length();
        int lower = len + data.consumeInt(1, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(0, 32);
        }

        String appendA = data.consumeBoolean() ? null : data.consumeAsciiString(16);
        String appendB = data.consumeBoolean() ? null : data.consumeAsciiString(16);

        String r1;
        try {
            r1 = WordUtils.abbreviate(str, lower, upper, appendA);
        } catch (RuntimeException t) {
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
            return;
        }

        if (!str.equals(r1)) {
            throw new RuntimeException(
                "[oracle:full-string] metamorphic violation: upper at or beyond length must clamp to full string for no-space input input="
                    + String.valueOf(str) + " lower=" + lower + " upper=" + upper + " append="
                    + String.valueOf(appendA) + " lhs=" + String.valueOf(r1) + " rhs=" + str);
        }

        String r2;
        try {
            r2 = WordUtils.abbreviate(str, lower, upper, appendB);
        } catch (RuntimeException t) {
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
            return;
        }

        if (!r1.equals(r2)) {
            throw new RuntimeException(
                "[oracle:append-ignored] metamorphic violation: appendToEnd must be ignored when no abbreviation occurs input="
                    + String.valueOf(str) + " lower=" + lower + " upper=" + upper + " lhs="
                    + String.valueOf(r1) + " rhs=" + String.valueOf(r2));
        }
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
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
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c == ' ' ? 'A' : c);
        }
        return sb.toString();
    }
}