package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact failing-test pattern first, through the real public API.
        tryPattern("it''s a {0,lower} 'test'!", registry, new Object[] { "DUMMY" }, false);

        // EXPLORE: generate many patterns with the same root-cause property:
        // a doubled quote in normal text, which drives ExtendedMessageFormat parsing
        // through appendQuotedString while scanning a real pattern.
        String prefix = sanitizeText(data.consumeAsciiString(24));
        String infix = sanitizeText(data.consumeAsciiString(24));
        String suffix = sanitizeText(data.consumeAsciiString(24));
        String quoted = sanitizeQuotedLiteral(data.consumeAsciiString(24));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0}";
                break;
            case 1:
                pattern = nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0} '" + quoted + "'";
                break;
            case 2:
                pattern = "'" + quoted + "' " + nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0}";
                break;
            case 3:
                pattern = nonEmpty(prefix) + " {0} " + nonEmpty(infix) + "''" + nonEmpty(suffix);
                break;
            case 4:
                pattern = nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0,number,integer}";
                break;
            default:
                pattern = nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0,date,short}";
                break;
        }

        Object arg;
        if (pattern.indexOf("{0,date,short}") >= 0) {
            arg = new java.util.Date(Math.abs((long) data.consumeInt()) % 1000000000L);
        } else if (pattern.indexOf("{0,number,integer}") >= 0) {
            arg = Integer.valueOf(data.consumeInt(-1000000, 1000000));
        } else {
            arg = sanitizeText(data.consumeAsciiString(20));
        }

        // Metamorphic/post-condition:
        // With no custom registry formats in use, ExtendedMessageFormat is obligated to behave
        // like MessageFormat for the same valid pattern and arguments. A "fix" that merely skips
        // the quoted-string bookkeeping could stop crashing but silently produce the wrong pattern
        // or formatted output. We therefore compare two REAL library calls on valid-by-construction
        // inputs and also require toPattern() round-tripping to agree.
        try {
            MessageFormat mf = new MessageFormat(pattern);
            String expected = mf.format(new Object[] { arg });

            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
            String before = emf1.toPattern();
            String actual = emf1.format(new Object[] { arg });
            String after = emf1.toPattern();

            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input=" + pattern + " lhs=" + before + " rhs=" + after);
            }
            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:mf-equivalence] metamorphic violation: ExtendedMessageFormat must match MessageFormat when no custom formats are used input=" + pattern + " lhs=" + actual + " rhs=" + expected);
            }

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
            String reparsed = emf2.toPattern();
            if (!safeEquals(before, reparsed)) {
                throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: reparsing toPattern should preserve it input=" + pattern + " lhs=" + before + " rhs=" + reparsed);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw rethrowUnchecked(t);
            }
        }
    }

    private static void tryPattern(String pattern, Map registry, Object[] args, boolean swallowAllNonRoot) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            emf.format(args);
            emf.toPattern();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw rethrowUnchecked(t);
            }
            if (!swallowAllNonRoot) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Validation") || name.contains("Invalid")) {
                return true;
            }
        }
        return false;
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
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException rethrowUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        return new RuntimeException(t);
    }

    private static String sanitizeText(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (Character.isISOControl(c)) {
                b.append('y');
            } else {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            b.append('x');
        }
        return b.toString();
    }

    private static String sanitizeQuotedLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "q";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                b.append("''");
            } else if (c == '{' || c == '}') {
                b.append('q');
            } else if (Character.isISOControl(c)) {
                b.append('r');
            } else {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            b.append('q');
        }
        return b.toString();
    }

    private static String nonEmpty(String s) {
        return (s == null || s.length() == 0) ? "x" : s;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}