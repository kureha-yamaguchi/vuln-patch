package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static final Object[] FORMAT_ARGS = new Object[] { "DUMMY" };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercise("it''s a {0,lower} 'test'!", registry, false);

        String pattern = buildPattern(data);
        exercise(pattern, registry, true);
    }

    private static String buildPattern(FuzzedDataProvider data) {
        String prefix = sanitize(data.consumeAsciiString(24));
        String mid = sanitize(data.consumeAsciiString(24));
        String suffix = sanitize(data.consumeAsciiString(24));
        int shape = data.consumeInt(0, 4);

        switch (shape) {
            case 0:
                return prefix + "''" + mid + " {0} " + suffix;
            case 1:
                return prefix + "'" + mid + "' {0} " + suffix;
            case 2:
                return prefix + "''" + mid + " '' " + suffix + " {0}";
            case 3:
                return "'" + prefix + "'" + mid + " {0} '" + suffix + "'";
            default:
                return prefix + " {0} '' " + mid + " '" + suffix + "'";
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        return s.replace('{', 'x').replace('}', 'y');
    }

    private static void exercise(String pattern, Map registry, boolean doChecks) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            if (doChecks) {
                String before = emf.toPattern();

                try {
                    emf.format(FORMAT_ARGS);
                } catch (Throwable t) {
                    if (isCleanRejection(t)) {
                        return;
                    }
                    if (isRootCause(t)) {
                        throw propagate(t);
                    }
                    return;
                }

                String after = emf.toPattern();
                if (!safeEquals(before, after)) {
                    throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format changed toPattern input=" + pattern + " before=" + before + " after=" + after);
                }

                try {
                    ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
                    String reparsed = emf2.toPattern();
                    String reparsed2 = emf2.toPattern();
                    if (!safeEquals(before, reparsed) || !safeEquals(reparsed, reparsed2)) {
                        throw new RuntimeException("[oracle:topattern-idempotent] metamorphic violation: canonical toPattern not stable input=" + pattern + " first=" + before + " second=" + reparsed + " third=" + reparsed2);
                    }
                } catch (Throwable t) {
                    if (isCleanRejection(t)) {
                        return;
                    }
                    if (isRootCause(t)) {
                        throw propagate(t);
                    }
                    return;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw propagate(t);
            }
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method)
                        || "next".equals(method)
                        || "startsWith".equals(method)
                        || "append".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}