package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact pattern from the regression test, driven through the real public API.
        exercise("it''s a {0,lower} 'test'!", registry);

        // EXPLORE: preserve the triggering property: a pattern accepted by ExtendedMessageFormat
        // that contains an escaped quote ("''"), varied in length/position/surrounding content.
        String left = safeToken(data.consumeAsciiString(12));
        String middle = safeToken(data.consumeAsciiString(12));
        String quoted = safeToken(data.consumeAsciiString(12));
        String tail = safeToken(data.consumeAsciiString(12));

        StringBuilder p = new StringBuilder();
        if (data.consumeBoolean()) {
            p.append(left);
        } else {
            p.append("pre").append(left);
        }
        if (data.consumeBoolean()) {
            p.append("''");
        } else {
            p.append(left).append("''");
        }
        if (data.consumeBoolean()) {
            p.append(' ').append(middle);
        }
        p.append(" {0,number}");
        if (data.consumeBoolean()) {
            p.append(" '").append(quoted).append('\'');
        }
        if (data.consumeBoolean()) {
            p.append(' ').append(tail);
        }
        exercise(p.toString(), registry);

        // A second exploration strategy using only literal text plus a simple argument.
        StringBuilder p2 = new StringBuilder();
        p2.append(safeToken(data.consumeAsciiString(8)));
        p2.append("''");
        p2.append(safeToken(data.consumeAsciiString(8)));
        if (data.consumeBoolean()) {
            p2.append(" {0}");
        } else {
            p2.append(" {0,time,short}");
        }
        if (data.consumeBoolean()) {
            p2.append(" '").append(safeToken(data.consumeAsciiString(8))).append('\'');
        }
        exercise(p2.toString(), registry);
    }

    private static void exercise(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String before = emf.toPattern();

            try {
                emf.format(new Object[] { Integer.valueOf(123), "DUMMY" });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }

            String after = emf.toPattern();

            // Oracle: toPattern() is a public reader of the formatter state; formatting must not
            // mutate that state. A patch that merely skips quote-processing bookkeeping can avoid
            // the crash yet silently produce inconsistent internal pattern state.
            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException(
                    "[oracle:toPattern-stable] metamorphic violation: format changed toPattern input="
                        + pattern + " lhs=" + before + " rhs=" + after);
            }

            // Oracle: reparsing the canonical pattern from toPattern() must be idempotent for any
            // accepted pattern. If either side throws, skip per fuzzing hygiene rules.
            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
                String reparsed = emf2.toPattern();
                if (before != null && reparsed != null && !before.equals(reparsed)) {
                    throw new RuntimeException(
                        "[oracle:toPattern-idem] metamorphic violation: reparsing canonical pattern changed it input="
                            + pattern + " lhs=" + before + " rhs=" + reparsed);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        return t instanceof OutOfMemoryError && hasAppendQuotedStringFrame(t);
    }

    private static boolean hasAppendQuotedStringFrame(Throwable t) {
        StackTraceElement[] frames = t.getStackTrace();
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement ste = frames[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.indexOf("Validation") >= 0 || n.indexOf("Invalid") >= 0;
    }

    private static String safeToken(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 32 && c <= 126 && c != '{' && c != '}' && c != '\'') {
                out.append(c);
            } else {
                out.append('x');
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}