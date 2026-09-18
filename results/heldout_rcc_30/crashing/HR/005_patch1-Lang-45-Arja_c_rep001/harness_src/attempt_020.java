package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseExactSeed();

        String fuzz = data.consumeAsciiString(32);
        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        String noSpace = forceNoSpaces(fuzz);
        if (noSpace.length() == 0) {
            noSpace = "A";
        }

        int lower = data.consumeInt(0, noSpace.length() + 4);
        int upper = noSpace.length() + data.consumeInt(1, 6);
        callAndMaybeReport(noSpace, lower, upper, append, true);

        String left = forceNoSpaces(data.consumeAsciiString(12));
        if (left.length() == 0) {
            left = "L";
        }
        String right = forceNoSpaces(data.consumeAsciiString(12));
        String base = left + " " + right;
        String prefix = forceNoSpaces(data.consumeAsciiString(8));
        int baseLower = data.consumeInt(0, left.length());
        int baseUpper = base.length() + data.consumeInt(1, 6);

        try {
            String rhs = WordUtils.abbreviate(base, baseLower, baseUpper, append);
            String lhs = WordUtils.abbreviate(prefix + base, baseLower + prefix.length(),
                    baseUpper + prefix.length(), append);

            if (rhs != null && lhs != null) {
                String expected = prefix + rhs;
                /*
                 * Contract behind this oracle:
                 * abbreviate finds the first " " at or after lower via StringUtils.indexOf,
                 * then returns the prefix up to that space plus StringUtils.defaultString(appendToEnd).
                 * Prepending only non-space characters and shifting lower/upper by the same amount
                 * preserves that cut point, so the new result must be exactly the old result with
                 * the prefix added in front. A patch that merely silences the crash but mishandles
                 * the >length clamp can still violate this relation.
                 */
                if (!lhs.equals(expected)) {
                    throw new RuntimeException("[oracle:shifted-prefix-space] metamorphic violation: "
                            + "base=" + quote(base)
                            + " prefix=" + quote(prefix)
                            + " lower=" + baseLower
                            + " upper=" + baseUpper
                            + " append=" + quote(append)
                            + " lhs=" + quote(lhs)
                            + " rhs=" + quote(rhs)
                            + " expected=" + quote(expected));
                }
            }
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isOracleViolation(e)) {
                throw e;
            }
            if (isRelevantRootCause(e)) {
                throw e;
            }
        }
    }

    private static void exerciseExactSeed() {
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException e) {
            if (isRelevantRootCause(e)) {
                throw e;
            }
        }
    }

    private static void callAndMaybeReport(String str, int lower, int upper, String append, boolean validByConstruction) {
        try {
            WordUtils.abbreviate(str, lower, upper, append);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (validByConstruction && isRelevantRootCause(e)) {
                throw e;
            }
        }
    }

    private static boolean isRelevantRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            String cls = trace[i].getClassName();
            String method = trace[i].getMethodName();
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

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isOracleViolation(RuntimeException e) {
        String msg = e.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static String forceNoSpaces(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        return s.replace(' ', 'X');
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}