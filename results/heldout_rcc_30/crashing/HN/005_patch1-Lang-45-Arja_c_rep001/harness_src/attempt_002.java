package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        String str = data.consumeAsciiString(40);
        if (str == null || str.length() == 0) {
            str = "A";
        }

        int len = str.length();
        int lower = len + data.consumeInt(0, 50);
        int upper = lower + data.consumeInt(0, 50);
        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(10);

        exerciseValidCase(str, lower, upper, appendToEnd);

        if (data.consumeBoolean()) {
            exerciseValidCase(str, lower, -1, appendToEnd);
        }
    }

    private static void anchor() {
        final String s = "0123456789";
        try {
            String out = WordUtils.abbreviate(s, 15, 20, null);
            if (!s.equals(out)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: lower/upper beyond length must return original string input="
                        + s + " lhs=" + out + " rhs=" + s);
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }
    }

    private static void exerciseValidCase(String str, int lower, int upper, String appendToEnd) {
        try {
            String out = WordUtils.abbreviate(str, lower, upper, appendToEnd);

            /*
             * Contract asserted: the method's own tests require
             * WordUtils.abbreviate("0123456789", 15, 20, null) == "0123456789".
             * More generally, when lower >= str.length() and upper is also at/above it,
             * a correct implementation clamps to the string length and returns the original
             * string without appending appendToEnd. A patch that merely suppresses the crash
             * but returns a truncated/modified value would violate this observable result.
             */
            if (!str.equals(out)) {
                throw new RuntimeException("[oracle:result] metamorphic violation: lower/upper at or beyond string length must yield original string input="
                        + printable(str) + " lower=" + lower + " upper=" + upper + " append="
                        + printable(appendToEnd) + " lhs=" + printable(out) + " rhs=" + printable(str));
            }

            if (upper != -1) {
                try {
                    String outUnlimited = WordUtils.abbreviate(str, lower, -1, appendToEnd);
                    if (!out.equals(outUnlimited)) {
                        throw new RuntimeException("[oracle:unlimited] metamorphic violation: for lower beyond end, explicit upper beyond end and upper=-1 must agree input="
                                + printable(str) + " lower=" + lower + " upper=" + upper + " append="
                                + printable(appendToEnd) + " lhs=" + printable(out) + " rhs=" + printable(outUnlimited));
                    }
                } catch (RuntimeException t) {
                    handleThrowable(t, false);
                }
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }
    }

    private static void handleThrowable(RuntimeException t, boolean validByConstruction) {
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

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid") || name.contains("Validation");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}