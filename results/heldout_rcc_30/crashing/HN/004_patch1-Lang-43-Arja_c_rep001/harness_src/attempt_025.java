package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact pattern from the failing test, driven through the real public API.
        // The bug is reached during ExtendedMessageFormat construction/applyPattern.
        try {
            Map registry = new HashMap();
            new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw (OutOfMemoryError) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        // EXPLORE: root-cause property is a pattern containing an escaped quote ('')
        // that is parsed by ExtendedMessageFormat. Build many valid-by-construction
        // MessageFormat-compatible patterns so a correct implementation is obliged to handle them.
        String pre = sanitize(data.consumeAsciiString(16));
        String mid = sanitize(data.consumeAsciiString(16));
        String post = sanitize(data.consumeAsciiString(16));
        String literal = sanitize(data.consumeAsciiString(16));
        String arg = sanitize(data.consumeString(32));
        if (arg.length() == 0) {
            arg = "X";
        }
        if (literal.length() == 0) {
            literal = "lit";
        }

        int variant = data.consumeInt(0, 4);
        String pattern;
        switch (variant) {
            case 0:
                pattern = pre + "''" + mid + " {0} '" + literal + "'" + post;
                break;
            case 1:
                pattern = "'" + literal + "' " + pre + "''" + mid + " {0}" + post;
                break;
            case 2:
                pattern = pre + " {0} " + mid + "''" + post + " '" + literal + "'";
                break;
            case 3:
                pattern = pre + "''" + mid + " '{0}' " + post + " {0}";
                break;
            default:
                pattern = pre + " {0} " + mid + "''" + post;
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
            String before = emf.toPattern();
            String actual = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            // Oracle: with no custom registry, ExtendedMessageFormat must remain compatible with
            // java.text.MessageFormat for ordinary MessageFormat patterns. A throw-deleting patch in
            // appendQuotedString can silently change quote parsing, yielding different output/pattern.
            String expectedPattern;
            String expected;
            try {
                MessageFormat mf = new MessageFormat(pattern);
                expectedPattern = mf.toPattern();
                expected = mf.format(new Object[] { arg });
            } catch (Throwable oracleThrow) {
                return;
            }

            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat output must match MessageFormat on standard patterns input="
                        + pattern + " lhs=" + actual + " rhs=" + expected);
            }
            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                        + pattern + " lhs=" + before + " rhs=" + after);
            }
            if (!safeEquals(after, expectedPattern)) {
                throw new RuntimeException("[oracle:topattern-eq] metamorphic violation: ExtendedMessageFormat toPattern must match MessageFormat on standard patterns input="
                        + pattern + " lhs=" + after + " rhs=" + expectedPattern);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}' && c >= 32) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}