package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        probeCrashAndMaybePropagate("0123456789", 15, 20, null, true);

        String base = data.consumeAsciiString(64);
        if (base == null) {
            return;
        }

        String noSpace = removeSpaces(base);
        if (noSpace.length() == 0) {
            noSpace = "A";
        }

        int len = noSpace.length();

        int lower;
        int upperChoice = data.consumeInt(0, 5);
        int appendChoice = data.consumeInt(0, 3);

        switch (data.consumeInt(0, 4)) {
            case 0:
                lower = len;
                break;
            case 1:
                lower = len + 1 + data.consumeInt(0, 32);
                break;
            case 2:
                lower = len + data.consumeInt(0, 32);
                break;
            case 3:
                lower = 15;
                break;
            default:
                lower = len + Math.abs(data.consumeByte());
                break;
        }

        int upper;
        switch (upperChoice) {
            case 0:
                upper = -1;
                break;
            case 1:
                upper = len;
                break;
            case 2:
                upper = len + 1 + data.consumeInt(0, 32);
                break;
            case 3:
                upper = lower;
                break;
            case 4:
                upper = lower + data.consumeInt(0, 16);
                break;
            default:
                upper = 20;
                break;
        }

        String appendToEnd;
        switch (appendChoice) {
            case 0:
                appendToEnd = null;
                break;
            case 1:
                appendToEnd = "";
                break;
            case 2:
                appendToEnd = data.consumeAsciiString(8);
                break;
            default:
                appendToEnd = "-";
                break;
        }

        probeCrashAndMaybePropagate(noSpace, lower, upper, appendToEnd, true);

        String spaced = buildSpacedVariant(noSpace, data);
        int lower2 = Math.max(spaced.length(), lower);
        int upper2 = data.consumeBoolean() ? -1 : lower2 + data.consumeInt(0, 16);
        probeCrashAndMaybePropagate(spaced, lower2, upper2, appendToEnd, true);

        /*
         * Contract asserted:
         * The method comment/code says:
         * - if the lower value is greater than the length of the string, set to the length of the string
         * - if the upper value is -1 or greater than the length of the string, set to the length of the string
         * For a string with no spaces, this means abbreviate(str, lower>=len, upper=-1 or upper>len, append)
         * must return the original string unchanged. A patch that merely suppresses the exception but returns
         * the wrong substring would violate this observable result.
         */
        checkNoSpaceFullReturnOracle(noSpace, lower, upper, appendToEnd);

        /*
         * Metamorphic relation from the same documented normalization:
         * once upper is treated as "to the end" (-1 or >len), all such values are equivalent.
         * Therefore, on no-space strings and lower>=len, calls with upper=-1 and upper=len+k must agree.
         * If either call throws, this check does not apply and is skipped.
         */
        checkEquivalentUpperOracle(noSpace, lower, appendToEnd, len + 1 + data.consumeInt(0, 32));
    }

    private static void probeCrashAndMaybePropagate(String str, int lower, int upper, String appendToEnd, boolean validByConstruction) {
        try {
            WordUtils.abbreviate(str, lower, upper, appendToEnd);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
        } catch (Error t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void checkNoSpaceFullReturnOracle(String str, int lower, int upper, String appendToEnd) {
        if (str == null || str.length() == 0) {
            return;
        }
        if (str.indexOf(' ') >= 0) {
            return;
        }
        if (lower < str.length()) {
            return;
        }
        if (!(upper == -1 || upper >= str.length())) {
            return;
        }

        final String result;
        try {
            result = WordUtils.abbreviate(str, lower, upper, appendToEnd);
        } catch (Throwable t) {
            return;
        }

        if (!str.equals(result)) {
            throw new RuntimeException(
                "[oracle:no-space-full] metamorphic violation: expected original string when lower>=len and upper reaches end input="
                    + printable(str) + " lower=" + lower + " upper=" + upper + " append=" + printable(appendToEnd)
                    + " lhs=" + printable(result) + " rhs=" + printable(str));
        }
    }

    private static void checkEquivalentUpperOracle(String str, int lower, String appendToEnd, int largeUpper) {
        if (str == null || str.length() == 0) {
            return;
        }
        if (str.indexOf(' ') >= 0) {
            return;
        }
        if (lower < str.length()) {
            return;
        }
        if (largeUpper <= str.length()) {
            largeUpper = str.length() + 1;
        }

        final String lhs;
        final String rhs;
        try {
            lhs = WordUtils.abbreviate(str, lower, -1, appendToEnd);
            rhs = WordUtils.abbreviate(str, lower, largeUpper, appendToEnd);
        } catch (Throwable t) {
            return;
        }

        if (lhs == null ? rhs != null : !lhs.equals(rhs)) {
            throw new RuntimeException(
                "[oracle:upper-equiv] metamorphic violation: upper=-1 and upper>len must agree after normalization input="
                    + printable(str) + " lower=" + lower + " upper1=-1 upper2=" + largeUpper
                    + " append=" + printable(appendToEnd) + " lhs=" + printable(lhs) + " rhs=" + printable(rhs));
        }
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
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static String removeSpaces(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String buildSpacedVariant(String s, FuzzedDataProvider data) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        if (s.length() == 1) {
            return s + " " + s;
        }
        int pos = data.consumeInt(1, s.length() - 1);
        return s.substring(0, pos) + " " + s.substring(pos);
    }

    private static String printable(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}