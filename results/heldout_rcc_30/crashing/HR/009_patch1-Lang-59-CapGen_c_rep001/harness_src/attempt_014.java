package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseExactSeed();

        String prefix = data.consumeAsciiString(8);
        String body = data.consumeString(24);
        if (body.length() == 0) {
            body = "X";
        }
        char pad = (char) ('!' + data.consumeInt(0, 90));

        int len = body.length();
        int which = data.consumeInt(0, 4);
        int width;
        switch (which) {
            case 0:
                width = Math.max(1, len - 1);
                break;
            case 1:
                width = len;
                break;
            case 2:
                width = len + 1;
                break;
            case 3:
                width = Math.max(1, data.consumeInt(1, len));
                break;
            default:
                width = data.consumeInt(1, len + 2);
                break;
        }

        checkFixedWidthAgainstEquivalentConstruction(prefix, body, width, pad);

        String prefix2 = data.consumeAsciiString(8);
        String body2 = data.consumeAsciiString(32);
        if (body2.length() == 0) {
            body2 = "AB";
        }
        int near = data.consumeBoolean() ? Math.max(1, body2.length() - 1) : body2.length();
        checkFixedWidthAgainstEquivalentConstruction(prefix2, body2, near, pad);
    }

    private static void exerciseExactSeed() {
        checkFixedWidthAgainstEquivalentConstruction("", "foo", 1, '-');
    }

    private static void checkFixedWidthAgainstEquivalentConstruction(
            String prefix, String str, int width, char padChar) {
        if (str == null || width <= 0) {
            return;
        }

        StrBuilder lhs = new StrBuilder(Math.max(1, prefix.length()));
        lhs.append(prefix);
        lhs.minimizeCapacity();

        StrBuilder rhs = new StrBuilder(Math.max(1, prefix.length()));
        rhs.append(prefix);
        rhs.minimizeCapacity();

        String lhsOut;
        String rhsOut;
        try {
            try {
                lhs.appendFixedWidthPadRight(str, width, padChar);
            } catch (RuntimeException t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCauseAioobe(t)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:flip-boundary] appendFixedWidthPadRight crashed on valid input"
                                    + " prefix=" + quote(prefix)
                                    + " str=" + quote(str)
                                    + " width=" + width
                                    + " pad=" + ((int) padChar));
                }
                return;
            }

            /*
             * Contract from appendFixedWidthPadRight's body/javadoc:
             * - if str.length() >= width, append exactly the first width chars;
             * - otherwise append the whole string and then right-pad to width.
             * Therefore this call must agree with an equivalent construction using
             * other real StrBuilder APIs. This catches throw-deleting or silent
             * wrong-output patches even when no exception is thrown.
             */
            if (str.length() >= width) {
                rhs.append(str, 0, width);
            } else {
                rhs.append(str);
                rhs.appendPadding(width - str.length(), padChar);
            }

            lhsOut = lhs.toString();
            rhsOut = rhs.toString();
        } catch (RuntimeException t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (!lhsOut.equals(rhsOut)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:flip-boundary] equivalent real-library constructions disagree"
                            + " prefix=" + quote(prefix)
                            + " str=" + quote(str)
                            + " width=" + width
                            + " pad=" + ((int) padChar)
                            + " lhs=" + quote(lhsOut)
                            + " rhs=" + quote(rhsOut));
        }

        /*
         * Independent consistency check:
         * StrBuilder.length() reports the number of characters currently stored,
         * and toString() exposes exactly those contents. Thus toString().length()
         * must equal length() for every correct implementation.
         */
        if (lhs.length() != lhsOut.length()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:length-vs-string] inconsistent reported length"
                            + " width=" + width
                            + " reported=" + lhs.length()
                            + " actual=" + lhsOut.length()
                            + " out=" + quote(lhsOut));
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCauseAioobe(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                    && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                    && ("ensureCapacity".equals(ste.getMethodName())
                    || "getNullText".equals(ste.getMethodName()))) {
                return true;
            }
        }
        return false;
    }

    private static String quote(String s) {
        return String.valueOf(s);
    }
}