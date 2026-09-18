package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            String anchored = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchored)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: documented/tested out-of-range bounds should abbreviate to the full original string input=0123456789 lhs=" + anchored + " rhs=0123456789");
            }
        } catch (Throwable t) {
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if (throughTarget && (t instanceof StringIndexOutOfBoundsException)) {
                throw (RuntimeException) t;
            }
            return;
        }

        String base = data.consumeAsciiString(32);
        if (base.length() == 0) {
            base = "X";
        }
        if (data.consumeBoolean()) {
            String left = data.consumeAsciiString(8);
            String right = data.consumeAsciiString(8);
            if (left.length() == 0) {
                left = "L";
            }
            if (right.length() == 0) {
                right = "R";
            }
            base = left + " " + base + " " + right;
        }

        int len = base.length();
        int lower = len + data.consumeInt(1, 20);
        int upperBeyondLen = len + data.consumeInt(1, 20);
        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        String lhs;
        try {
            lhs = WordUtils.abbreviate(base, lower, -1, appendToEnd);
        } catch (Throwable t) {
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if (throughTarget && (t instanceof StringIndexOutOfBoundsException)) {
                throw (RuntimeException) t;
            }
            return;
        }

        String rhs;
        try {
            rhs = WordUtils.abbreviate(base, lower, upperBeyondLen, appendToEnd);
        } catch (Throwable t) {
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if (throughTarget && (t instanceof StringIndexOutOfBoundsException)) {
                throw (RuntimeException) t;
            }
            return;
        }

        /*
         * Contract asserted from the method's own comments and the regression test:
         * - if lower is greater than the string length, it is treated as the string length
         * - if upper is -1 or greater than the string length, it is treated as the string length
         * Therefore, for any non-null/non-empty string with lower > length and upper == -1
         * or upper > length, the result must be the original string, with no append string added.
         * A patch that only suppresses the crash or special-cases one bound would violate this.
         */
        if (!base.equals(lhs)) {
            throw new RuntimeException("[oracle:full-string-minus1] metamorphic violation: lower>length and upper==-1 must return the original string input=" + base + " lhs=" + lhs + " rhs=" + base);
        }
        if (!base.equals(rhs)) {
            throw new RuntimeException("[oracle:full-string-overflow] metamorphic violation: lower>length and upper>length must return the original string input=" + base + " lhs=" + rhs + " rhs=" + base);
        }
        if (!lhs.equals(rhs)) {
            throw new RuntimeException("[oracle:equiv-upper] metamorphic violation: upper==-1 and upper>length are documented-equivalent here input=" + base + " lhs=" + lhs + " rhs=" + rhs);
        }
    }
}