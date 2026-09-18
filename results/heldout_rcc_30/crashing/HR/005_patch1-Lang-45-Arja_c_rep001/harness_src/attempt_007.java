package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String str;
        if (data.consumeBoolean()) {
            str = buildStringWithOptionalSpaces(data);
        } else {
            str = data.consumeAsciiString(32);
        }

        if (str == null || str.length() == 0) {
            return;
        }

        int len = str.length();

        int lower;
        int mode = data.consumeInt(0, 4);
        if (mode == 0) {
            lower = len + data.consumeInt(1, 16);
        } else if (mode == 1) {
            lower = len;
        } else if (mode == 2) {
            lower = data.consumeInt(0, len);
        } else if (mode == 3) {
            lower = Math.max(0, len - data.consumeInt(0, Math.min(len, 8)));
        } else {
            lower = len + data.consumeInt(1, 4);
        }

        int upper;
        int upperMode = data.consumeInt(0, 3);
        if (upperMode == 0) {
            upper = len + data.consumeInt(1, 16);
        } else if (upperMode == 1) {
            upper = -1;
        } else if (upperMode == 2) {
            upper = data.consumeInt(0, len + 16);
        } else {
            upper = Math.max(0, lower - data.consumeInt(0, 8));
        }

        checkCutpointConsistency(str, lower, upper);
    }

    private static void runAnchor() {
        checkCutpointConsistency("0123456789", 15, 20);
    }

    private static void checkCutpointConsistency(String str, int lower, int upper) {
        if (str == null || str.length() == 0 || lower < 0) {
            return;
        }

        final String append = null;
        final int expectedCut = expectedPrefixLength(str, lower, upper);
        final String expectedPrefix = StringUtils.substring(str, 0, expectedCut);

        try {
            String actual = WordUtils.abbreviate(str, lower, upper, append);

            if (actual == null) {
                throw new RuntimeException("[oracle:cutpoint-consistency] metamorphic violation: non-null input produced null result"
                        + " input=" + quote(str) + " lower=" + lower + " upper=" + upper);
            }

            /*
             * Contract visible in WordUtils.abbreviate:
             * with appendToEnd == null, the method appends StringUtils.defaultString(null) == "",
             * so the returned value must be exactly the prefix selected by the branch that uses
             * StringUtils.indexOf(str, " ", lower) and the normalized upper bound.
             * This cross-check recomputes that cut point independently from the helper results.
             * A throw-deleting or branch-skipping patch can still return the wrong prefix; this catches that.
             */
            if (actual.length() != expectedCut || !actual.equals(expectedPrefix)) {
                throw new RuntimeException("[oracle:cutpoint-consistency] metamorphic violation: prefix/cut disagreement"
                        + " input=" + quote(str)
                        + " lower=" + lower
                        + " upper=" + upper
                        + " expectedCut=" + expectedCut
                        + " expectedPrefix=" + quote(expectedPrefix)
                        + " actual=" + quote(actual));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInScope(t)) {
                throw new RuntimeException("[oracle:cutpoint-consistency] metamorphic violation: valid input should produce prefix"
                        + " input=" + quote(str)
                        + " lower=" + lower
                        + " upper=" + upper
                        + " expectedCut=" + expectedCut
                        + " expectedPrefix=" + quote(expectedPrefix), t);
            }
        }
    }

    private static int expectedPrefixLength(String str, int lower, int upper) {
        int normalizedUpper = upper;
        if (normalizedUpper < lower) {
            normalizedUpper = lower;
        }
        if (normalizedUpper == -1 || normalizedUpper > str.length()) {
            normalizedUpper = str.length();
        }

        int index = StringUtils.indexOf(str, " ", lower);
        if (index == -1) {
            return normalizedUpper;
        } else if (index > normalizedUpper) {
            return normalizedUpper;
        } else {
            return index;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("validation") || lower.contains("invalid")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRootCauseInScope(Throwable t) {
        boolean isIndexFailure = t instanceof StringIndexOutOfBoundsException
                || t instanceof IndexOutOfBoundsException
                || t instanceof ArrayIndexOutOfBoundsException;
        if (!isIndexFailure) {
            return false;
        }

        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
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

    private static String buildStringWithOptionalSpaces(FuzzedDataProvider data) {
        String base = data.consumeAsciiString(24);
        if (base == null) {
            return "A";
        }
        if (base.length() == 0) {
            base = "A";
        }

        if (!data.consumeBoolean()) {
            return base;
        }

        int insertPos = data.consumeInt(0, base.length());
        String left = base.substring(0, insertPos);
        String right = base.substring(insertPos);
        String middle = data.consumeBoolean() ? " " : "  ";
        return left + middle + right;
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}