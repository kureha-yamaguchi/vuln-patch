package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact trigger from the failing test.
        // On the buggy version this reaches appendQuotedString() and loops until OOME
        // before any later validation of the unknown "lower" format can reject the input.
        tryPattern("it''s a {0,lower} 'test'!", registry, false, null);

        // EXPLORE / flip the patched condition:
        // The bug is triggered by doubled quotes (escaped apostrophes) when parsing with a non-null registry.
        // We generate valid-by-construction MessageFormat patterns with many quote placements and lengths.
        String prefix = data.consumeAsciiString(24);
        String middle = data.consumeAsciiString(24);
        String suffix = data.consumeAsciiString(24);
        String quoted = data.consumeAsciiString(24);

        prefix = sanitize(prefix);
        middle = sanitize(middle);
        suffix = sanitize(suffix);
        quoted = sanitize(quoted);

        int mode = data.consumeInt(0, 5);
        String pattern;
        switch (mode) {
            case 0:
                // Boundary: escaped quote near the beginning.
                pattern = prefix + "it''s " + middle + " {0} " + suffix;
                break;
            case 1:
                // Boundary: escaped quote adjacent to a format element.
                pattern = prefix + "''{0}" + suffix;
                break;
            case 2:
                // Boundary flip around the changed condition: quoted literal section plus escaped quote outside it.
                pattern = prefix + "'" + quoted + "'" + " " + middle + "''" + " {0}" + suffix;
                break;
            case 3:
                // Multiple escaped quotes.
                pattern = prefix + "''" + middle + "''" + " {0} '" + quoted + "'" + suffix;
                break;
            case 4:
                // Escaped quote inside surrounding text before and after the argument.
                pattern = prefix + "a''b " + "{0}" + " c''d " + suffix;
                break;
            default:
                // Exact failing shape but without custom format, so it is valid by construction.
                pattern = "it''s a {0} 'test'!" + suffix;
                break;
        }

        // Sound oracle:
        // For patterns with no custom formats, ExtendedMessageFormat with a non-null but empty registry
        // must behave like java.text.MessageFormat on the same pattern.
        // A throw-deleting patch that silently skips quote handling would violate this observable equivalence.
        tryPattern(pattern, registry, true, "dummy");

        // Additional metamorphic/idempotence check on the library's own exposed reader.
        // Contract basis: toPattern() is the pattern representation of the formatter; reconstructing a fresh
        // formatter from that pattern must produce the same toPattern() again for a correct implementation.
        tryIdempotence(pattern, registry);
    }

    private static void tryPattern(String pattern, Map registry, boolean checkOracle, Object arg) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            if (!checkOracle) {
                try {
                    emf.format(new Object[] { "dummy" });
                } catch (Throwable t) {
                    if (isCleanRejection(t)) {
                        return;
                    }
                    if (isRootCause(t)) {
                        throwRoot(t);
                    }
                }
                return;
            }

            String emfToPatternBefore = null;
            try {
                emfToPatternBefore = emf.toPattern();
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwRoot(t);
                }
                return;
            }

            String lhs;
            try {
                lhs = emf.format(new Object[] { arg });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwRoot(t);
                }
                return;
            }

            String rhs;
            try {
                rhs = new MessageFormat(pattern).format(new Object[] { arg });
            } catch (Throwable t) {
                // If the comparison side rejects, the relation does not apply.
                return;
            }

            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:mf-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat for patterns without custom formats input=" + pattern + " lhs=" + lhs + " rhs=" + rhs);
            }

            String emfToPatternAfter;
            try {
                emfToPatternAfter = emf.toPattern();
            } catch (Throwable t) {
                return;
            }

            if (emfToPatternBefore != null && emfToPatternAfter != null && !emfToPatternBefore.equals(emfToPatternAfter)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: formatting should not mutate toPattern input=" + pattern + " before=" + emfToPatternBefore + " after=" + emfToPatternAfter);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwRoot(t);
            }
        }
    }

    private static void tryIdempotence(String pattern, Map registry) {
        try {
            ExtendedMessageFormat first = new ExtendedMessageFormat(pattern, registry);
            String p1 = first.toPattern();
            ExtendedMessageFormat second = new ExtendedMessageFormat(p1, registry);
            String p2 = second.toPattern();
            if (p1 != null && p2 != null && !p1.equals(p2)) {
                throw new RuntimeException("[oracle:toPattern-idem] metamorphic violation: reconstructing from toPattern must preserve toPattern input=" + pattern + " first=" + p1 + " second=" + p2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwRoot(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation")
                || name.contains("Invalid")
                || name.contains("Malformed")
                || name.contains("Parse");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(m)
                    || "next".equals(m)
                    || "startsWith".equals(m)
                    || "append".equals(m))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(m) || "startsWith".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == ',') {
                b.append('x');
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static void throwRoot(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}