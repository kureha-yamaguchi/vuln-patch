package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact failing-test pattern first.
        drivePattern("it''s a {0,lower} 'test'!", registry, "DUMMY");

        // EXPLORE: generate many real MessageFormat-compatible patterns that still
        // exercise escaped-quote handling on the public ExtendedMessageFormat API.
        String a = sanitizeLiteral(data.consumeAsciiString(24));
        String b = sanitizeLiteral(data.consumeAsciiString(24));
        String c = sanitizeLiteral(data.consumeAsciiString(24));
        String d = sanitizeLiteral(data.consumeAsciiString(24));

        if (a.length() == 0) {
            a = "a";
        }
        if (b.length() == 0) {
            b = "b";
        }
        if (c.length() == 0) {
            c = "c";
        }

        StringBuilder pattern = new StringBuilder();
        pattern.append(a);

        // Root-cause property: include at least one escaped quote sequence outside
        // a format element so appendQuotedString is reached with escaping enabled.
        int escapedQuoteCount = data.consumeInt(1, 3);
        for (int i = 0; i < escapedQuoteCount; i++) {
            pattern.append("''");
            pattern.append((i % 2 == 0) ? b : c);
        }

        if (data.consumeBoolean()) {
            pattern.append(' ');
        }
        pattern.append("{0}");

        // Add a well-formed quoted literal section to vary positions/surroundings.
        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append('\'').append(c).append('\'');
        }

        if (data.consumeBoolean()) {
            pattern.append(d);
        }

        drivePattern(pattern.toString(), registry, data.consumeString(32));
    }

    private static void drivePattern(String pattern, Map registry, Object arg) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            // Contract/metamorphic check:
            // For patterns using only standard MessageFormat syntax (i.e. no custom
            // registry formats), ExtendedMessageFormat should behave like the real
            // JDK MessageFormat on the same pattern and arguments.
            // Also, format(...) is observationally read-only with respect to the
            // applied pattern, so toPattern() must not change across formatting.
            if (pattern.indexOf(",lower") < 0 && pattern.indexOf(",upper") < 0) {
                try {
                    String before = emf.toPattern();
                    String lhs = emf.format(new Object[] { arg });

                    MessageFormat mf = new MessageFormat(pattern);
                    String rhs = mf.format(new Object[] { arg });

                    String after = emf.toPattern();
                    if (!safeEquals(before, after)) {
                        throw new RuntimeException(
                                "[oracle:topattern-stable] metamorphic violation: format changed toPattern input="
                                        + pattern + " lhs=" + before + " rhs=" + after);
                    }
                    if (!safeEquals(lhs, rhs)) {
                        throw new RuntimeException(
                                "[oracle:mf-equivalence] metamorphic violation: ExtendedMessageFormat and MessageFormat disagree input="
                                        + pattern + " lhs=" + lhs + " rhs=" + rhs);
                    }
                } catch (Throwable t) {
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                    if (t instanceof RuntimeException && isOracleFailure((RuntimeException) t)) {
                        throw (RuntimeException) t;
                    }
                    if (isCleanRejection(t)) {
                        return;
                    }
                    return;
                }
            } else {
                try {
                    emf.format(new Object[] { arg });
                } catch (Throwable t) {
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                    if (isCleanRejection(t)) {
                        return;
                    }
                    return;
                }
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String cn = t.getClass().getName();
        return cn != null && (cn.contains("Validation") || cn.contains("Invalid"));
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                continue;
            }
            out.append(ch);
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