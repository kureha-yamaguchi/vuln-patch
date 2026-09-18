package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseExactAnchor();

        String raw = data.consumeAsciiString(32);
        String str = makeNonSpaceNonEmpty(raw);
        int len = str.length();

        int lower = data.consumeInt(0, len - 1);
        int upper = data.consumeInt(0, len - 1);

        String a = data.consumeAsciiString(8);
        String b = data.consumeRemainingAsString();
        if (b == null) {
            b = "";
        }

        String lhs;
        String rhsBase;
        try {
            lhs = callAbbreviateChecked(str, lower, upper, a + b);
            rhsBase = callAbbreviateChecked(str, lower, upper, a);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            throw t;
        }

        if (lhs == null || rhsBase == null) {
            return;
        }

        String rhs = rhsBase + b;

        /*
         * Contract used for this oracle:
         * for a non-empty string with no spaces and with upper < str.length(),
         * WordUtils.abbreviate takes the index==-1 branch, returns str.substring(0, upper')
         * and, because abbreviation definitely occurred, appends defaultString(appendToEnd).
         * Therefore changing appendToEnd from "a" to "a+b" must change only the suffix:
         * abbreviate(str,l,u,a+b) == abbreviate(str,l,u,a) + b.
         * A patch that merely suppresses the historic exception or silently uses the wrong
         * clamp/cutpoint will violate this observable relation.
         */
        if (!lhs.equals(rhs)) {
            throw new RuntimeException(
                "[oracle:append-associativity] metamorphic violation: " +
                "abbreviate(str,lower,upper,a+b) must equal abbreviate(str,lower,upper,a)+b " +
                "input=" + ArrayUtils.toString(new Object[] { str, Integer.valueOf(lower), Integer.valueOf(upper), a, b }) +
                " lhs=" + lhs + " rhs=" + rhs);
        }
    }

    private static void exerciseExactAnchor() {
        final String s = "0123456789";
        try {
            String out = WordUtils.abbreviate(s, 15, 20, null);
            if (!s.equals(out)) {
                throw new RuntimeException(
                    "[oracle:anchor-valid-full] metamorphic violation: valid overshoot input must return the original string " +
                    "input=" + ArrayUtils.toString(new Object[] { s, Integer.valueOf(15), Integer.valueOf(20), null }) +
                    " out=" + out);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromTarget(t)) {
                throw new RuntimeException(
                    "[oracle:anchor-valid-full] valid overshoot input triggered target failure instead of returning the original string",
                    t);
            }
        }
    }

    private static String callAbbreviateChecked(String str, int lower, int upper, String append) {
        try {
            return WordUtils.abbreviate(str, lower, upper, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                throw t;
            }
            if (isRootCauseFromTarget(t)) {
                throw new RuntimeException(
                    "[oracle:valid-no-space] valid-by-construction input should abbreviate without throwing " +
                    "input=" + ArrayUtils.toString(new Object[] {
                        str, Integer.valueOf(lower), Integer.valueOf(upper), append
                    }),
                    t);
            }
            throw t;
        }
    }

    private static boolean isRootCauseFromTarget(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException) && !(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if (e == null) {
                continue;
            }
            String cn = e.getClassName();
            String mn = e.getMethodName();
            if (("org.apache.commons.lang.WordUtils".equals(cn) && "abbreviate".equals(mn))
                    || ("org.apache.commons.lang.StringUtils".equals(cn)
                        && ("indexOf".equals(mn) || "defaultString".equals(mn)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static String makeNonSpaceNonEmpty(String s) {
        if (s == null || s.length() == 0) {
            return "A";
        }
        StringBuffer buf = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                c = 'X';
            }
            buf.append(c);
        }
        if (buf.length() == 0) {
            buf.append('A');
        }
        return buf.toString();
    }
}