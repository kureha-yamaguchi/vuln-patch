package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression input from WordUtilsTest.testAbbreviate.
        try {
            String anchored = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchored)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input must return original string input=0123456789 lower=15 upper=20 append=null result=" + String.valueOf(anchored));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInAbbreviate(t)) {
                throw t;
            }
        }

        String str = data.consumeString(64);
        if (str == null) {
            str = "";
        }

        int extra = data.consumeInt(1, 32);
        int lower = data.consumeInt(0, str.length() + 32);
        int upper = data.consumeBoolean() ? -1 : str.length() + extra;
        String appendToEnd = data.consumeBoolean() ? null : data.consumeString(16);

        // EXPLORE crash surface through the real public API.
        // Valid-by-construction: non-null string, non-negative lower, and upper is either -1 or > str.length(),
        // which the method's own comments say must be normalized to str.length().
        try {
            WordUtils.abbreviate(str, lower, upper, appendToEnd);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInAbbreviate(t) && isValidByConstruction(str, lower, upper)) {
                throw t;
            }
            return;
        }

        // Mandatory oracle:
        // Contract visible in the method comments/code: "if the upper value is -1 ... or is greater
        // than the length of the string, set to the length of the string".
        // Therefore, for any fixed (str, lower, appendToEnd), calls with upper == -1 and upper > str.length()
        // must be equivalent. A throw-deleting or overfitted patch that changes behavior instead of correctly
        // normalizing upper would violate this observable sibling-input relation.
        try {
            String lhs = WordUtils.abbreviate(str, lower, -1, appendToEnd);
            String rhs = WordUtils.abbreviate(str, lower, str.length() + extra, appendToEnd);
            if (!safeEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:upper-normalization] metamorphic violation: abbreviate(str, lower, -1, append) must equal abbreviate(str, lower, upper>len, append) input="
                        + printable(str) + " lower=" + lower + " upper1=-1 upper2=" + (str.length() + extra)
                        + " append=" + printable(appendToEnd) + " lhs=" + printable(lhs) + " rhs=" + printable(rhs));
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (isRootCauseInAbbreviate(t) && isValidByConstruction(str, lower, -1)) {
                throw t;
            }
        }
    }

    private static boolean isValidByConstruction(String str, int lower, int upper) {
        return str != null && lower >= 0 && (upper == -1 || upper > str.length());
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

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}