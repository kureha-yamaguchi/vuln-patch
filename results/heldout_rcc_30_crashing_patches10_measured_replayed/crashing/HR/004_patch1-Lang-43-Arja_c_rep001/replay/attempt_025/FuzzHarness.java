package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercise("it''s a {0,lower} 'test'!", registry, false);

        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            String pattern = buildPattern(data, i);
            exercise(pattern, registry, true);
        }
    }

    private static void exercise(String pattern, Map registry, boolean runOracle) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            if (!runOracle) {
                return;
            }

            String reported = emf.toPattern();

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reported, registry);
                String independent = emf2.toPattern();

                /* Contract/oracle:
                 * toPattern() is the object's own pattern representation. Reconstructing an
                 * ExtendedMessageFormat from that representation and reading toPattern() again
                 * must be stable for any correct implementation. A throw-deleting or
                 * quote-skipping patch can avoid the crash while silently corrupting the
                 * recorded pattern, which this idempotent round-trip exposes.
                 */
                if (!reported.equals(independent)) {
                    throw new RuntimeException(
                        "[oracle:toPattern-idempotence] metamorphic violation: input=" + pattern
                            + " lhs=" + reported + " rhs=" + independent);
                }

                String f1 = emf.format(new Object[] { "DuMmY" });
                String f2 = emf2.format(new Object[] { "DuMmY" });
                if (!f1.equals(f2)) {
                    throw new RuntimeException(
                        "[oracle:format-consistency] metamorphic violation: input=" + pattern
                            + " lhs=" + f1 + " rhs=" + f2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                if (t instanceof Error) {
                    throw (Error) t;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                if (t instanceof Error) {
                    throw (Error) t;
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
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
        String n = t.getClass().getName();
        return n.contains("Validation") || n.contains("Invalid");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String m = ste.getMethodName();
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

    private static String buildPattern(FuzzedDataProvider data, int variant) {
        String a = safeLiteral(data.consumeAsciiString(12));
        String b = safeLiteral(data.consumeAsciiString(12));
        String c = safeLiteral(data.consumeAsciiString(12));

        if (a.length() == 0) {
            a = "a";
        }
        if (b.length() == 0) {
            b = "b";
        }
        if (c.length() == 0) {
            c = "c";
        }

        switch (variant % 4) {
            case 0:
                return a + "''" + b + " {0} " + c;
            case 1:
                return "'" + a + "''" + b + "' {0}";
            case 2:
                return a + " {0} '' " + b;
            default:
                return a + "''" + b + " '{1}' {0} " + c;
        }
    }

    private static String safeLiteral(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length() && out.length() < 24; i++) {
            char ch = s.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == ' ') {
                out.append(ch);
            }
        }
        return out.toString();
    }
}