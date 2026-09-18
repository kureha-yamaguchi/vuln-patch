package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input returned wrong value input=0123456789 lower=15 upper=20 append=null result=" + String.valueOf(anchor));
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String s = data.consumeAsciiString(20);
        if (s.length() == 0) {
            s = "A";
        }

        if (data.consumeBoolean() && s.length() > 1) {
            int pos = data.consumeInt(1, s.length() - 1);
            s = s.substring(0, pos) + " " + s.substring(pos);
        }

        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(5);
        int lower = data.consumeInt(0, s.length());
        int upperTooLarge = s.length() + 1 + data.consumeInt(0, 20);

        String lhs;
        try {
            lhs = WordUtils.abbreviate(s, lower, upperTooLarge, appendToEnd);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String rhs;
        try {
            rhs = WordUtils.abbreviate(s, lower, -1, appendToEnd);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /*
         * Contract asserted from the method body/comments:
         * "if the upper value is -1 (i.e. no limit) or is greater than the length
         * of the string, set to the length of the string".
         * Therefore, for any non-null, non-empty string and any lower within [0, len],
         * abbreviate(str, lower, len+k, append) must equal abbreviate(str, lower, -1, append).
         * A patch that merely suppresses the exception or skips the normalization would
         * break this observable equivalence.
         */
        if (!String.valueOf(lhs).equals(String.valueOf(rhs))) {
            throw new RuntimeException(
                "[oracle:upper-clamp] metamorphic violation: upper>length must behave like upper=-1 input="
                    + quote(s) + " lower=" + lower + " upper=" + upperTooLarge + " append=" + quote(appendToEnd)
                    + " lhs=" + quote(lhs) + " rhs=" + quote(rhs));
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
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

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}