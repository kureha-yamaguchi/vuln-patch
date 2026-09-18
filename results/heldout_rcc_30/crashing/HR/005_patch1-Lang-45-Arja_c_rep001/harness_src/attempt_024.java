package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchorCrash();

        String raw = data.consumeString(64);
        String append = data.consumeBoolean() ? null : data.consumeString(16);

        // Explore the root cause with valid-by-construction inputs:
        // the method explicitly documents/clamps lower/upper beyond the string length.
        String noSpace = removeSpaces(raw);
        if (noSpace.length() == 0) {
            noSpace = "A";
        }
        int len = noSpace.length();
        int lower = len + data.consumeInt(0, 16);
        int upperChoice = data.consumeInt(0, 2);
        int upper;
        if (upperChoice == 0) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(0, 16);
        }
        tryRootCause(noSpace, lower, upper, append, true);

        // Boundary-focused explore around the patched condition.
        int boundaryDelta = data.consumeInt(0, 2);
        int nearUpper = len + boundaryDelta; // len, len+1, len+2
        tryRootCause(noSpace, lower, nearUpper, append, true);

        // Independent oracle using a different reachable function path:
        // By construction there is a space exactly at 'lower'.
        // The implementation computes index = StringUtils.indexOf(str, " ", lower)
        // and when that index <= upper it returns str.substring(0, index) + defaultString(appendToEnd).
        // A throw-deleting or branch-skipping patch would violate this observable result.
        String left = ensureNonEmpty(removeSpaces(data.consumeAsciiString(16)));
        String right = ensureNonEmpty(removeSpaces(data.consumeAsciiString(16)));
        String suffix = removeSpaces(data.consumeAsciiString(16));
        String spaced = left + " " + right + suffix;
        int exactLower = left.length();
        int exactUpper = exactLower + data.consumeInt(0, Math.max(0, spaced.length() - exactLower));
        checkSpaceAtLowerOracle(spaced, exactLower, exactUpper, append);

        // Also exercise defaultString through a null append on the same branch.
        checkSpaceAtLowerOracle(spaced, exactLower, exactUpper, null);
    }

    private static void exerciseAnchorCrash() {
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void tryRootCause(String str, int lower, int upper, String append, boolean validByConstruction) {
        try {
            WordUtils.abbreviate(str, lower, upper, append);
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void checkSpaceAtLowerOracle(String str, int lower, int upper, String append) {
        String actual;
        try {
            actual = WordUtils.abbreviate(str, lower, upper, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        int idx;
        try {
            idx = StringUtils.indexOf(str, " ", lower);
        } catch (RuntimeException t) {
            return;
        }
        if (idx != lower) {
            return;
        }
        if (upper < lower) {
            upper = lower;
        }
        if (upper < idx) {
            return;
        }

        String expected;
        try {
            expected = str.substring(0, idx) + StringUtils.defaultString(append);
        } catch (RuntimeException t) {
            return;
        }

        if (!expected.equals(actual)) {
            throw new RuntimeException(
                "[oracle:space-at-lower] metamorphic violation: exact-space cut must use indexOf/defaultString branch"
                    + " input=" + safe(str)
                    + " lower=" + lower
                    + " upper=" + upper
                    + " append=" + safe(append)
                    + " idx=" + idx
                    + " lhs=" + safe(actual)
                    + " rhs=" + safe(expected));
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            String cls = stack[i].getClassName();
            String method = stack[i].getMethodName();
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

    private static String removeSpaces(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer out = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String ensureNonEmpty(String s) {
        return s.length() == 0 ? "X" : s;
    }

    private static String safe(String s) {
        return s == null ? "null" : s;
    }
}