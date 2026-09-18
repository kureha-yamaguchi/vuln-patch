package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test. On the buggy build this reaches
        // WordUtils.abbreviate and throws StringIndexOutOfBoundsException.
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException t) {
            boolean passesAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    passesAbbreviate = true;
                    break;
                }
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (passesAbbreviate && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
        }

        String base = data.consumeString(64);
        if (base == null) {
            return;
        }

        // Fence degenerate inputs for the oracle: use non-empty strings by construction.
        if (base.length() == 0) {
            base = "A";
        }

        // Explore the root-cause property: upper == -1 or upper > str.length().
        // Also vary lower at/around/above the string length, since the failing test does so.
        int len = base.length();
        int mode = data.consumeInt(0, 5);
        int lower;
        if (mode == 0) {
            lower = len;
        } else if (mode == 1) {
            lower = len + data.consumeInt(1, 8);
        } else if (mode == 2) {
            lower = Math.max(0, len - data.consumeInt(0, Math.min(len, 8)));
        } else {
            lower = data.consumeInt(0, len + 8);
        }

        String appendToEnd;
        int appendMode = data.consumeInt(0, 3);
        if (appendMode == 0) {
            appendToEnd = null;
        } else if (appendMode == 1) {
            appendToEnd = "";
        } else {
            appendToEnd = data.consumeAsciiString(8);
        }

        int upper;
        int upperMode = data.consumeInt(0, 3);
        if (upperMode == 0) {
            upper = -1;
        } else if (upperMode == 1) {
            upper = len + data.consumeInt(1, 12);
        } else if (upperMode == 2) {
            upper = len;
        } else {
            upper = data.consumeInt(0, len + 12);
        }

        // First real call through the public API with varied inputs satisfying the patched-line property.
        try {
            WordUtils.abbreviate(base, lower, upper, appendToEnd);
        } catch (RuntimeException t) {
            boolean passesAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    passesAbbreviate = true;
                    break;
                }
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            // Ground-truth throwable is generic, so only propagate when the input is valid-by-construction:
            // the documented contract in the method comments says upper == -1 or upper > str.length()
            // must be normalized to str.length(), so these inputs must be accepted.
            if (passesAbbreviate
                    && t instanceof StringIndexOutOfBoundsException
                    && (upper == -1 || upper > len)
                    && base.length() > 0) {
                throw t;
            }
            return;
        }

        // Mandatory metamorphic check.
        // Contract guarantee from the method's own comments/code:
        // "if the upper value is -1 ... or is greater than the length of the string, set to the length of the string".
        // Therefore, for every correct implementation:
        // abbreviate(str, lower, upper, append) == abbreviate(str, lower, str.length(), append)
        // whenever upper == -1 or upper > str.length().
        // A patch that merely deletes the crashing operation or makes the branch unreachable can violate this
        // observable equality without throwing.
        if (base.length() > 0 && (upper == -1 || upper > len)) {
            try {
                String lhs = WordUtils.abbreviate(base, lower, upper, appendToEnd);
                String rhs = WordUtils.abbreviate(base, lower, len, appendToEnd);
                if (lhs == null ? rhs != null : !lhs.equals(rhs)) {
                    throw new RuntimeException(
                            "[oracle:upper-clamp] metamorphic violation: abbreviate must clamp upper=-1/>len to str.length()"
                                    + " input="
                                    + base
                                    + " lower="
                                    + lower
                                    + " upper="
                                    + upper
                                    + " len="
                                    + len
                                    + " append="
                                    + String.valueOf(appendToEnd)
                                    + " lhs="
                                    + String.valueOf(lhs)
                                    + " rhs="
                                    + String.valueOf(rhs));
                }
            } catch (RuntimeException t) {
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:upper-clamp]")) {
                    throw t;
                }
                return;
            }
        }
    }
}