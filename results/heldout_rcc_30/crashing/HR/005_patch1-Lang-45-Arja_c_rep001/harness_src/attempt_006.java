package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String base = data.consumeAsciiString(32);
        if (base == null || base.length() == 0) {
            base = "0123456789";
        }
        String append1 = data.consumeBoolean() ? null : data.consumeAsciiString(8);
        int lower = base.length() + 1 + data.consumeInt(0, 8);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = base.length() + data.consumeInt(0, 8);
        }
        tryAbbreviate(base, lower, upper, append1, true);

        String prefix = noSpaces(data.consumeAsciiString(12));
        String suffix = noSpaces(data.consumeAsciiString(12));
        if (prefix.length() == 0) {
            prefix = "A";
        }
        String spaced = prefix + " " + suffix;
        int spaceIndex = prefix.length();
        int oracleLower = data.consumeInt(0, spaceIndex);
        int oracleUpper = data.consumeInt(spaceIndex, spaced.length() + 8);
        String oracleAppend = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String actual = WordUtils.abbreviate(spaced, oracleLower, oracleUpper, oracleAppend);

            /*
             * Contract visible in WordUtils.abbreviate:
             * if the first space found at/after lower is at or before upper,
             * the result is substring(0, index) + defaultString(appendToEnd).
             * This cross-check exercises the real helper calls StringUtils.indexOf
             * and StringUtils.defaultString and would catch a throw-deleting or
             * wrong-clamping patch that returns the wrong text instead of crashing.
             */
            int idx = StringUtils.indexOf(spaced, " ", oracleLower);
            if (idx != -1 && idx <= oracleUpper) {
                String expected = StringUtils.substring(spaced, 0, idx) + StringUtils.defaultString(oracleAppend);
                if (!expected.equals(actual)) {
                    throw new RuntimeException(
                        "[oracle:space-branch-compose] metamorphic violation: " +
                        "input=" + spaced +
                        " lower=" + oracleLower +
                        " upper=" + oracleUpper +
                        " append=" + oracleAppend +
                        " lhs=" + actual +
                        " rhs=" + expected);
                }
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runAnchor() {
        try {
            anchorCall();
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static String anchorCall() {
        return WordUtils.abbreviate("0123456789", 15, 20, null);
    }

    private static String tryAbbreviate(String str, int lower, int upper, String appendToEnd, boolean validByConstruction) {
        try {
            return WordUtils.abbreviate(str, lower, upper, appendToEnd);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return null;
            }
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            return null;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
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
            String cls = trace[i].getClassName();
            String method = trace[i].getMethodName();
            if (("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(method))
                || ("org.apache.commons.lang.StringUtils".equals(cls)
                    && ("indexOf".equals(method) || "defaultString".equals(method)))) {
                return true;
            }
        }
        return false;
    }

    private static String noSpaces(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        return s.replace(' ', 'X');
    }
}