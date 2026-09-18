package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }

        String str = data.consumeString(64);
        if (str == null) {
            str = "";
        }
        if (str.length() == 0) {
            str = "A";
        }

        String appendToEnd;
        if (data.consumeBoolean()) {
            appendToEnd = null;
        } else {
            appendToEnd = data.consumeString(8);
        }

        int len = str.length();
        int lower = len + data.consumeInt(1, 32);
        int upperVariant = len + data.consumeInt(1, 32);

        try {
            WordUtils.abbreviate(str, lower, upperVariant, appendToEnd);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        try {
            WordUtils.abbreviate(str, lower, -1, appendToEnd);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        /*
         * Oracle: abbreviate's documented normalization says:
         * "if the upper value is -1 (i.e. no limit) or is greater than the length
         * of the string, set to the length of the string".
         * Therefore, for the same (valid-by-construction) string/lower/appendToEnd,
         * calls that differ only by upper == -1 versus upper > str.length() must
         * produce the same observable result.
         * A patch that merely suppresses the crash or changes control flow without
         * performing the documented normalization can violate this equality.
         */
        try {
            String lhs = WordUtils.abbreviate(str, lower, -1, appendToEnd);
            String rhs = WordUtils.abbreviate(str, lower, upperVariant, appendToEnd);
            if (lhs != null ? !lhs.equals(rhs) : rhs != null) {
                throw new RuntimeException(
                    "[oracle:upper-normalization] metamorphic violation: upper=-1 and upper>length must agree input="
                        + describe(str, lower, upperVariant, appendToEnd)
                        + " lhs=" + String.valueOf(lhs)
                        + " rhs=" + String.valueOf(rhs));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleViolation(t)) {
                throw t;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isOracleViolation(RuntimeException t) {
        return t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isRootCause(RuntimeException t) {
        return t instanceof StringIndexOutOfBoundsException
            && hasAbbreviateFrame(t);
    }

    private static boolean hasAbbreviateFrame(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(RuntimeException t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid")
            || name.contains("Validation")
            || name.contains("Malformed");
    }

    private static String describe(String str, int lower, int upper, String appendToEnd) {
        return "{str=" + String.valueOf(str)
            + ",len=" + str.length()
            + ",lower=" + lower
            + ",upper=" + upper
            + ",appendToEnd=" + String.valueOf(appendToEnd)
            + "}";
    }
}