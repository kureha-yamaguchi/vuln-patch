package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test input first. On the buggy build this reaches
        // WordUtils.abbreviate and throws StringIndexOutOfBoundsException from abbreviate.
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor-eq] metamorphic violation: exact failing-test call must return the full original string input=0123456789 lower=15 upper=20 lhs=" + String.valueOf(anchor) + " rhs=0123456789");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throwUnchecked(t);
            }
            return;
        }

        // EXPLORE:
        // Documented guarantee from the method body/comments:
        // - "if the upper value is -1 ... or is greater than the length of the string, set to the length of the string"
        // Therefore, for any non-null string and any fixed lower/appendToEnd,
        // abbreviate(str, lower, -1, append) and abbreviate(str, lower, upper>str.length(), append)
        // must produce the same observable result.
        // A patch that merely suppresses the crash or skips the intended clamp would violate this.
        String str = data.consumeString(64);
        if (str == null) {
            str = "";
        }

        String appendToEnd = data.consumeBoolean() ? null : data.consumeString(16);

        int len = str.length();
        int lower = data.consumeInt(0, len + 20);
        int upperBeyondLen = len + data.consumeInt(1, 20);

        String lhs;
        try {
            lhs = WordUtils.abbreviate(str, lower, -1, appendToEnd);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throwUnchecked(t);
            }
            return;
        }

        String rhs;
        try {
            rhs = WordUtils.abbreviate(str, lower, upperBeyondLen, appendToEnd);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException("[oracle:abbrev-upper-clamp] metamorphic violation: abbreviate(str, lower, -1, append) must equal abbreviate(str, lower, upper>len, append) input="
                    + debug(str) + " lower=" + lower + " upper=" + upperBeyondLen + " append=" + debug(appendToEnd)
                    + " lhs=" + debug(lhs) + " rhs=" + debug(rhs));
        }

        // Additional direct post-condition on a non-degenerate no-space string built by construction:
        // if upper > str.length(), the code must clamp upper to str.length();
        // for a string with no spaces, that means the result is the full original string
        // (and no appendToEnd is added because no abbreviation occurred).
        String noSpace = data.consumeAsciiString(64).replace(' ', 'X');
        if (noSpace.length() == 0) {
            noSpace = "X";
        }
        int lower2 = data.consumeInt(noSpace.length(), noSpace.length() + 20);
        int upper2 = noSpace.length() + data.consumeInt(1, 20);

        try {
            String out = WordUtils.abbreviate(noSpace, lower2, upper2, null);
            if (!noSpace.equals(out)) {
                throw new RuntimeException("[oracle:abbrev-full] metamorphic violation: for a no-space string with upper>len, abbreviate must return the full original string input="
                        + debug(noSpace) + " lower=" + lower2 + " upper=" + upper2 + " lhs=" + debug(out) + " rhs=" + debug(noSpace));
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
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

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String debug(String s) {
        return String.valueOf(s);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}