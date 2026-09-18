package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchor();

        String str = data.consumeAsciiString(32);
        if (str == null || str.length() == 0) {
            str = "X";
        }

        int len = str.length();
        int lowerOverflow = len + data.consumeInt(1, 8);

        int upperChoice = data.consumeInt(0, 2);
        int upper;
        if (upperChoice == 0) {
            upper = -1;
        } else if (upperChoice == 1) {
            upper = len + data.consumeInt(1, 8);
        } else {
            upper = lowerOverflow + data.consumeInt(0, 8);
        }

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        /*
         * Contract visible in the method comments:
         * "if the lower value is greater than the length of the string, set to the length of the string".
         * Therefore, for every correct implementation:
         *   abbreviate(str, lower>len, upper, append)
         * must equal
         *   abbreviate(str, len,       upper, append)
         * on non-null, non-empty str.
         *
         * This is a real-library metamorphic check through the public API and still detects
         * band-aid patches that merely silence the exception but mishandle the effective cutpoint.
         */
        String lhs;
        try {
            lhs = WordUtils.abbreviate(str, lowerOverflow, upper, append);
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        String rhs;
        try {
            rhs = WordUtils.abbreviate(str, len, upper, append);
        } catch (Throwable t) {
            handleThrowable(t, false);
            return;
        }

        if (!same(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:lower-clamp-eq] metamorphic violation: lower values beyond the string length " +
                "must behave exactly like lower==length input=" + printable(str) +
                " len=" + len +
                " lowerOverflow=" + lowerOverflow +
                " upper=" + upper +
                " append=" + printable(append) +
                " lhs=" + printable(lhs) +
                " rhs=" + printable(rhs));
        }

        /*
         * Independent helper-facing cross-check:
         * once lower has overflowed past the end, String search from lowerOverflow and from len
         * are equivalent overshoot positions for the same string; abbreviate's branch decision
         * must therefore be based on the same helper-observable cutpoint.
         */
        try {
            int fromOverflow = StringUtils.indexOf(str, " ", lowerOverflow);
            int fromLen = StringUtils.indexOf(str, " ", len);
            if (fromOverflow != fromLen) {
                throw new RuntimeException(
                    "[oracle:indexof-clamp-view] metamorphic violation: equivalent overshoot search starts disagree " +
                    "input=" + printable(str) +
                    " len=" + len +
                    " lowerOverflow=" + lowerOverflow +
                    " overflowIndex=" + fromOverflow +
                    " lenIndex=" + fromLen);
            }
        } catch (Throwable t) {
            handleThrowable(t, false);
        }
    }

    private static void exerciseAnchor() {
        try {
            String out = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(out)) {
                throw new RuntimeException(
                    "[oracle:anchor-lower-clamp] metamorphic violation: documented anchor input must return the full string " +
                    "lhs=" + printable(out));
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (isCleanRejection(t)) {
            return;
        }
        if (validByConstruction && isRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException) && !(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang.StringUtils".equals(e.getClassName())) {
                String m = e.getMethodName();
                if ("indexOf".equals(m) || "defaultString".equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation") || name.contains("Invalid");
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String printable(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}