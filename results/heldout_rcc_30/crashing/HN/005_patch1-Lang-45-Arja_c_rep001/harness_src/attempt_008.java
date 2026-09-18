package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression test input from WordUtilsTest.
        checkAbbreviateIdentity("0123456789", 15, 20, null, "anchor", true);

        // EXPLORE:
        // Documented contract from the method comments shown in the patch:
        // - "if the lower value is greater than the length of the string, set to the length of the string"
        // - "if the upper value is -1 ... or is greater than the length of the string, set to the length of the string"
        //
        // Therefore, for any non-null, non-empty string with lower >= str.length() and
        // upper == -1 or upper > str.length(), a correct implementation must behave as if
        // lower == upper == str.length(). Since StringUtils.indexOf(str, " ", lowerClamped)
        // then finds no space, result must be str.substring(0, str.length()) == str, and
        // appendToEnd must not be appended because no abbreviation occurred.
        //
        // This directly checks an observable post-condition; a "fix" that merely avoids the
        // crash but returns the wrong substring would violate it.
        String str = data.consumeString(64);
        if (str == null || str.length() == 0) {
            return;
        }

        int len = str.length();

        int lowerChoice = data.consumeInt(0, 2);
        int lower;
        if (lowerChoice == 0) {
            lower = len;
        } else if (lowerChoice == 1) {
            lower = len + data.consumeInt(1, 64);
        } else {
            lower = len + Math.abs(data.consumeInt() % 64);
            if (lower < len) {
                lower = len;
            }
        }

        String appendToEnd = data.consumeBoolean() ? null : "";

        int upperMode = data.consumeInt(0, 2);
        int upper;
        if (upperMode == 0) {
            upper = -1;
        } else if (upperMode == 1) {
            upper = len + data.consumeInt(1, 64);
        } else {
            upper = len + Math.abs(data.consumeInt() % 64) + 1;
        }

        checkAbbreviateIdentity(str, lower, upper, appendToEnd, "identity", true);

        // Additional boundary-focused variants around the exact property.
        checkAbbreviateIdentity(str, len, len + 1, null, "boundary-upper-plus-one", true);
        checkAbbreviateIdentity(str, len + 1, len + 1, "", "boundary-lower-upper-equal-past-end", true);
        checkAbbreviateIdentity(str, len + data.consumeInt(1, 8), -1, null, "boundary-upper-unlimited", true);
    }

    private static void checkAbbreviateIdentity(String str, int lower, int upper, String appendToEnd,
                                                String oracleId, boolean validByConstruction) {
        try {
            String result = WordUtils.abbreviate(str, lower, upper, appendToEnd);
            if (!str.equals(result)) {
                throw new RuntimeException(
                    "[oracle:" + oracleId + "] metamorphic violation: expected identity when lower>=len and upper==-1 or >len"
                        + " input=" + quote(str)
                        + " lower=" + lower
                        + " upper=" + upper
                        + " append=" + quote(appendToEnd)
                        + " lhs=" + quote(result)
                        + " rhs=" + quote(str));
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
            String n = c.getName();
            String s = c.getSimpleName();
            if (n.contains("Validation") || n.contains("Invalid") || s.contains("Validation") || s.contains("Invalid")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                    && "abbreviate".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}