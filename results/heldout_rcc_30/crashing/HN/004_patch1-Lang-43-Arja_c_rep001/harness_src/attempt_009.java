package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact failing-test pattern first. The buggy build reaches
        // ExtendedMessageFormat.appendQuotedString() and blows up with OOME.
        tryExtendedMessageFormat("it''s a {0,lower} 'test'!", registry, new Object[] { "DUMMY" }, false);

        // EXPLORE: vary surrounding text while preserving the root-cause property:
        // a MessageFormat pattern containing an escaped quote token ("''").
        String a = sanitizeLiteral(data.consumeAsciiString(24));
        String b = sanitizeLiteral(data.consumeAsciiString(24));
        String c = sanitizeLiteral(data.consumeAsciiString(24));
        String d = sanitizeLiteral(data.consumeAsciiString(24));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0} " + c;
                break;
            case 1:
                pattern = a + " {0} " + b + "''" + c;
                break;
            case 2:
                pattern = "'" + a + "'" + b + "''" + c + " {0}";
                break;
            case 3:
                pattern = a + "''" + b + " '{1}' " + c + " {0}";
                break;
            case 4:
                pattern = a + " {0} " + b + " '' " + c + " {1}";
                break;
            default:
                pattern = a + "''" + b + " '{quoted}' " + c + " {0} " + d;
                break;
        }

        Object[] args = new Object[] {
            data.consumeRemainingAsString(),
            sanitizeLiteral(data.consumeAsciiString(16))
        };

        tryExtendedMessageFormat(pattern, null, args, true);
    }

    private static void tryExtendedMessageFormat(String pattern, Map registry, Object[] args, boolean runOracle) {
        try {
            ExtendedMessageFormat emf = (registry == null)
                    ? new ExtendedMessageFormat(pattern)
                    : new ExtendedMessageFormat(pattern, registry);

            String before = emf.toPattern();
            emf.format(args);
            String after = emf.toPattern();

            if (runOracle) {
                // Oracle: with no custom registry, ExtendedMessageFormat must preserve ordinary
                // MessageFormat behavior for valid MessageFormat patterns. A throw-deleting or
                // bookkeeping-skipping patch in quote handling can silently change parsing/output
                // while no exception is thrown, so compare real-library results.
                try {
                    MessageFormat mf = new MessageFormat(pattern);
                    String lhs = emf.format(args);
                    String rhs = mf.format(args);
                    if (!safeEquals(lhs, rhs)) {
                        throw new RuntimeException(
                                "[oracle:msgfmt-equiv] metamorphic violation: ExtendedMessageFormat without registry must match MessageFormat on the same valid pattern input="
                                        + pattern + " lhs=" + lhs + " rhs=" + rhs);
                    }
                    String mfPattern = mf.toPattern();
                    if (!safeEquals(after, mfPattern)) {
                        throw new RuntimeException(
                                "[oracle:topattern-equiv] metamorphic violation: toPattern must match MessageFormat without custom formats input="
                                        + pattern + " lhs=" + after + " rhs=" + mfPattern);
                    }
                    if (!safeEquals(before, after)) {
                        throw new RuntimeException(
                                "[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input="
                                        + pattern + " lhs=" + before + " rhs=" + after);
                    }
                } catch (Throwable t) {
                    if (t instanceof RuntimeException && ((RuntimeException) t).getMessage() != null
                            && ((RuntimeException) t).getMessage().startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                    return;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw (Error) t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
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
        String name = t.getClass().getName();
        return name.contains("Validation") || name.contains("Invalid");
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}