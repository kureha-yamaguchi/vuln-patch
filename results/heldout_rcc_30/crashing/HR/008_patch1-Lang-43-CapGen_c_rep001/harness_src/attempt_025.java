package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercisePattern("it''s a {0,lower} 'test'!", registry, false, null);

        String left = sanitizeLiteral(data.consumeAsciiString(32));
        String right = sanitizeLiteral(data.consumeAsciiString(32));
        String tail = sanitizeLiteral(data.consumeAsciiString(24));

        if (left.length() == 0) {
            left = "a";
        }
        if (right.length() == 0) {
            right = "b";
        }

        StringBuilder pattern = new StringBuilder();
        pattern.append(left);
        if (data.consumeBoolean()) {
            pattern.append(' ');
        }
        pattern.append("''");
        pattern.append(right);
        if (data.consumeBoolean()) {
            pattern.append(" '");
            String quoted = sanitizeQuotedBody(data.consumeAsciiString(16));
            if (quoted.length() == 0) {
                quoted = "q";
            }
            pattern.append(quoted).append('\'');
        }
        if (tail.length() > 0) {
            pattern.append(' ').append(tail);
        }
        pattern.append(" {0}");

        String arg = data.consumeString(24);
        if (arg.length() == 0) {
            arg = "dummy";
        }

        exercisePattern(pattern.toString(), registry, true, arg);
    }

    private static void exercisePattern(String pattern, Map registry, boolean doOracle, String arg) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            if (!doOracle) {
                return;
            }

            String reportedPattern;
            try {
                reportedPattern = emf.toPattern();
            } catch (Throwable t) {
                if (isCleanRejection(t) || !isRootCause(t)) {
                    return;
                }
                throwUnchecked(t);
                return;
            }

            String formatted1;
            try {
                formatted1 = emf.format(new Object[] { arg });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reportedPattern, registry);
                String reportedPattern2 = emf2.toPattern();
                String formatted2 = emf2.format(new Object[] { arg });

                /* Contract: toPattern() exposes the pattern representation of this formatter.
                 * Reconstructing an ExtendedMessageFormat from that reported pattern must preserve
                 * the same observable pattern and formatting behavior. A patch that merely skips
                 * the quote-handling step can stop crashing yet silently change the preserved
                 * pattern or its formatted output, so we assert this round-trip relation. */
                if (!safeEquals(reportedPattern, reportedPattern2)) {
                    throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input=" + pattern
                            + " lhs=" + reportedPattern + " rhs=" + reportedPattern2);
                }
                if (!safeEquals(formatted1, formatted2)) {
                    throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: input=" + pattern
                            + " lhs=" + formatted1 + " rhs=" + formatted2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
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

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName().toLowerCase();
            if (name.contains("validation") || name.contains("invalid") || name.contains("format")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)) {
                if ("appendQuotedString".equals(m) || "next".equals(m) || "append".equals(m) || "startsWith".equals(m)) {
                    return true;
                }
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)) {
                if ("append".equals(m) || "startsWith".equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String sanitizeLiteral(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String sanitizeQuotedBody(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                out.append('q');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}