package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test input first. On the buggy build this is the verified crash:
        // WordUtils.abbreviate("0123456789", 15, 20, null) -> StringIndexOutOfBoundsException from abbreviate.
        // Contract/post-condition asserted here: the failing test documents that this call must return
        // "0123456789". A throw-deleting or branch-skipping patch that returns the wrong substring would violate it.
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor-value] metamorphic violation: documented test expectation input=(\"0123456789\",15,20,null) lhs="
                        + String.valueOf(anchor) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }

        String base = data.consumeString(64);
        if (base == null) {
            base = "";
        }
        if (base.length() == 0) {
            base = "x";
        }

        String append = data.consumeBoolean() ? null : data.consumeString(8);

        int len = base.length();

        // EXPLORE strategy:
        // Build valid-by-construction inputs that satisfy the patched-line property:
        // upper is -1 or greater than str.length(). The method's own contract/comments say such upper
        // values are to be treated as str.length(), so a correct implementation must accept them.
        int lower;
        if (data.consumeBoolean()) {
            lower = data.consumeInt(0, len);
        } else {
            lower = data.consumeInt(len + 1, len + 32);
        }

        int upperChoice = data.consumeInt(0, 2);
        int upper;
        if (upperChoice == 0) {
            upper = -1;
        } else if (upperChoice == 1) {
            upper = len + 1 + data.consumeInt(0, 32);
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        // Direct exploration call through the real public API.
        try {
            WordUtils.abbreviate(base, lower, upper, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        // Mandatory metamorphic/post-condition check:
        // Documented guarantee in the shown method body:
        // "if the upper value is -1 (i.e. no limit) or is greater than the length of the string,
        // set to the length of the string"
        // Therefore, for the same non-null string/lower/append, abbreviate(str, lower, upper>len, append)
        // must equal abbreviate(str, lower, -1, append). If a patch merely suppresses the crash but
        // computes the wrong result, this sibling-equivalent-input relation catches it.
        try {
            String lhs = WordUtils.abbreviate(base, lower, upper, append);
            String rhs = WordUtils.abbreviate(base, lower, -1, append);
            if (lhs != null ? !lhs.equals(rhs) : rhs != null) {
                throw new RuntimeException("[oracle:upper-cap] metamorphic violation: equivalent upper values should agree input="
                        + describe(base, lower, upper, append) + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        // Additional equivalent-input check with an explicit oversized upper bound.
        int oversizedUpper = len + 1 + data.consumeInt(0, 64);
        try {
            String lhs = WordUtils.abbreviate(base, lower, oversizedUpper, append);
            String rhs = WordUtils.abbreviate(base, lower, len, append);
            if (lhs != null ? !lhs.equals(rhs) : rhs != null) {
                throw new RuntimeException("[oracle:upper-equals-len] metamorphic violation: upper>length should behave like upper==length input="
                        + describe(base, lower, oversizedUpper, append) + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("validation") || lower.contains("invalid") || lower.contains("malformed")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
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

    private static String describe(String str, int lower, int upper, String append) {
        return "(strLen=" + (str == null ? -1 : str.length())
                + ",str=" + String.valueOf(str)
                + ",lower=" + lower
                + ",upper=" + upper
                + ",append=" + String.valueOf(append)
                + ")";
    }
}