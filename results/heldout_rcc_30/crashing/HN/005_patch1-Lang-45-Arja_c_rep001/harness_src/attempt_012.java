package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test. On the buggy build this must reach
        // WordUtils.abbreviate and fail with StringIndexOutOfBoundsException there.
        try {
            String anchored = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchored)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: documented clamping for lower/upper beyond string length should return full string input=0123456789 lower=15 upper=20 lhs=" + anchored + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCauseInAbbreviate(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String left = data.consumeAsciiString(20);
        String right = data.consumeAsciiString(20);
        boolean addSpace = data.consumeBoolean();
        boolean addTrailingSpace = data.consumeBoolean();

        String str = left;
        if (addSpace) {
            str = str + " " + right;
        } else {
            str = str + right;
        }
        if (addTrailingSpace) {
            str = str + " ";
        }
        if (str.length() == 0) {
            str = "X";
        }

        int len = str.length();
        int lower = len + data.consumeInt(1, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String appendToEnd = null;
        if (data.consumeBoolean()) {
            appendToEnd = data.consumeAsciiString(8);
        }

        try {
            String result = WordUtils.abbreviate(str, lower, upper, appendToEnd);

            // Post-condition from the method's own documented behavior:
            // - "if the lower value is greater than the length of the string, set to the length of the string"
            // - "if the upper value is -1 ... or is greater than the length of the string, set to the length of the string"
            // With lower > len and upper in {-1, > len}, both clamp to len, indexOf(..., lower) is effectively beyond
            // the end, no abbreviation occurs, and the method must return the original string unchanged.
            if (!str.equals(result)) {
                throw new RuntimeException("[oracle:full-string] metamorphic violation: lower/upper clamped beyond end should yield original string input=" + printable(str) + " lower=" + lower + " upper=" + upper + " append=" + printable(appendToEnd) + " lhs=" + printable(result) + " rhs=" + printable(str));
            }

            // Metamorphic relation: for these valid-by-construction inputs, upper == -1 and any upper > len are
            // documented to clamp to the same effective upper bound (str.length()), so results must agree.
            try {
                String unlimited = WordUtils.abbreviate(str, lower, -1, appendToEnd);
                if (!result.equals(unlimited)) {
                    throw new RuntimeException("[oracle:upper-clamp] metamorphic violation: upper=-1 and upper>length should agree after clamping input=" + printable(str) + " lower=" + lower + " upper=" + upper + " append=" + printable(appendToEnd) + " lhs=" + printable(result) + " rhs=" + printable(unlimited));
                }
            } catch (RuntimeException inner) {
                if (isOracleFailure(inner)) {
                    throw inner;
                }
                if (isRootCauseInAbbreviate(inner)) {
                    throw inner;
                }
                if (isCleanRejection(inner)) {
                    return;
                }
                return;
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCauseInAbbreviate(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseInAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return s == null ? "null" : s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }
}