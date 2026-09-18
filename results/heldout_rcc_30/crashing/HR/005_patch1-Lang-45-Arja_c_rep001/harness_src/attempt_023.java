package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchor();

        String str = data.consumeAsciiString(64);
        if (str == null) {
            return;
        }

        int len = str.length();
        int mode = data.consumeInt(0, 3);

        int lower;
        int upper;

        if (mode == 0) {
            lower = len + data.consumeInt(1, 8);
            upper = lower + data.consumeInt(0, 8);
        } else if (mode == 1) {
            lower = len + data.consumeInt(0, 8);
            upper = -1;
        } else if (mode == 2) {
            lower = len;
            upper = len + data.consumeInt(1, 8);
        } else {
            lower = len + data.consumeInt(1, 8);
            upper = len + data.consumeInt(1, 8);
            if (upper < lower) {
                upper = lower;
            }
        }

        String appendA = data.consumeBoolean() ? null : data.consumeAsciiString(16);
        String appendB = data.consumeBoolean() ? "" : data.consumeAsciiString(16);

        checkOvershootContract(str, lower, upper, appendA, appendB);
    }

    private static void exerciseAnchor() {
        String str = "0123456789";
        int lower = 15;
        int upper = 20;
        try {
            String out = WordUtils.abbreviate(str, lower, upper, null);
            String expected = StringUtils.substring(str, 0, str.length());
            if (!safeEquals(out, expected)) {
                throw new RuntimeException("[oracle:anchor-helper-agreement] metamorphic violation: lower/upper past end must yield the full original string input="
                        + str + " lower=" + lower + " upper=" + upper + " out=" + out + " expected=" + expected);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void checkOvershootContract(String str, int lower, int upper, String appendA, String appendB) {
        if (str == null) {
            return;
        }

        int len = str.length();
        if (lower < len) {
            return;
        }
        if (!(upper == -1 || upper >= lower)) {
            return;
        }

        String expected = StringUtils.substring(str, 0, len);

        String resultA;
        try {
            resultA = WordUtils.abbreviate(str, lower, upper, appendA);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        if (!safeEquals(resultA, expected)) {
            throw new RuntimeException("[oracle:overshoot-helper-agreement] metamorphic violation: documented test shows lower beyond length with upper beyond length is accepted and returns the original string input="
                    + str + " len=" + len + " lower=" + lower + " upper=" + upper + " append=" + appendA
                    + " result=" + resultA + " expected=" + expected);
        }

        try {
            String resultB = WordUtils.abbreviate(str, lower, upper, appendB);
            if (!safeEquals(resultA, resultB)) {
                throw new RuntimeException("[oracle:overshoot-append-independence] metamorphic violation: when lower starts at or beyond the end, StringUtils.indexOf(..., lower) is -1 and no abbreviation occurs, so appendToEnd must be observationally irrelevant input="
                        + str + " len=" + len + " lower=" + lower + " upper=" + upper
                        + " appendA=" + appendA + " appendB=" + appendB
                        + " resultA=" + resultA + " resultB=" + resultB);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException) && !(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(method)) {
                return true;
            }
            if ("org.apache.commons.lang.StringUtils".equals(cls)
                    && ("indexOf".equals(method) || "defaultString".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}