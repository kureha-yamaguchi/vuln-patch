package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            String result = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(result)) {
                throw new RuntimeException("[oracle:anchor-eq] metamorphic violation: documented test case must return original string when lower and upper exceed length input=0123456789 lower=15 upper=20 append=null lhs=" + String.valueOf(result) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String base = buildNonSpaceString(data);
        if (base.length() == 0) {
            return;
        }

        int len = base.length();
        int lower = len + data.consumeInt(0, 32);
        int upperChoice = data.consumeInt(0, 3);
        int upper;
        if (upperChoice == 0) {
            upper = -1;
        } else if (upperChoice == 1) {
            upper = len + data.consumeInt(0, 32);
        } else if (upperChoice == 2) {
            upper = lower + data.consumeInt(0, 32);
        } else {
            upper = data.consumeInt(Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);
        }
        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String result = WordUtils.abbreviate(base, lower, upper, appendToEnd);

            /*
             * Contract asserted:
             * In abbreviate, if lower exceeds the string length, lower is treated as the end of the string
             * by the subsequent search; if upper is -1 or greater than the string length, upper is set to the
             * string length; and appendToEnd is added only if abbreviation has occurred.
             * For a non-empty string with no spaces and lower >= str.length(), a correct implementation must
             * return the original string unchanged and must not append appendToEnd, because no abbreviation occurs.
             * A patch that merely suppresses the exception or skips the bound adjustment can return the wrong
             * substring or append marker, violating this observable post-condition.
             */
            if (!base.equals(result)) {
                throw new RuntimeException("[oracle:nospace-full] metamorphic violation: no-space input with lower>=length and upper at/above length must round to full original string input=" + printable(base) + " lower=" + lower + " upper=" + upper + " append=" + printable(appendToEnd) + " lhs=" + printable(result) + " rhs=" + printable(base));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracle(t)) {
                throw t;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
        }

        trySiblingAgreement(data, base);
    }

    private static void trySiblingAgreement(FuzzedDataProvider data, String base) {
        String wrapInput = base + " " + buildNonSpaceString(data) + " " + buildNonSpaceString(data);
        if (wrapInput.trim().length() == 0) {
            return;
        }
        int wrapLength = data.consumeInt(1, 20);
        try {
            String lhs = WordUtils.wrap(wrapInput, wrapLength);
            String rhs = WordUtils.wrap(wrapInput, wrapLength, null, false);
            if (lhs != null && rhs != null && !lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:wrap-overload] metamorphic violation: documented sibling overloads with default newline/null and wrapLongWords=false must agree input=" + printable(wrapInput) + " wrapLength=" + wrapLength + " lhs=" + printable(lhs) + " rhs=" + printable(rhs));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracle(t)) {
                throw t;
            }
        }
    }

    private static String buildNonSpaceString(FuzzedDataProvider data) {
        String s = data.consumeAsciiString(32);
        if (s == null) {
            return "A";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                c = 'X';
            }
            if (c < 33 || c > 126) {
                c = 'Y';
            }
            sb.append(c);
        }
        if (sb.length() == 0) {
            sb.append('A');
        }
        return sb.toString();
    }

    private static boolean isRootCauseFromAbbreviate(Throwable t) {
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

    private static boolean isOracle(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static String printable(String s) {
        return String.valueOf(s);
    }
}