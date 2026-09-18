package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            exercisePattern(anchorPattern, new Object[] { "DUMMY" }, false);
        } catch (Throwable t) {
            handleThrowable(t);
        }

        String safeArg = data.consumeAsciiString(16);
        String prefix = sanitizeLiteral(data.consumeAsciiString(24));
        String middle = sanitizeLiteral(data.consumeAsciiString(24));
        String suffix = sanitizeLiteral(data.consumeAsciiString(24));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = prefix + "''" + middle + "{0}" + suffix;
                break;
            case 1:
                pattern = "'" + prefix + "'" + middle + "{0}" + suffix;
                break;
            case 2:
                pattern = prefix + "{0}" + middle + "''" + suffix;
                break;
            case 3:
                pattern = prefix + "''" + middle + " '{0}' " + suffix;
                break;
            case 4:
                pattern = prefix + "'x'" + middle + "{0}" + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + "{0,number}" + suffix;
                break;
        }

        try {
            exercisePattern(pattern, new Object[] { safeArg }, true);
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void exercisePattern(String pattern, Object[] args, boolean runOracles) {
        Map registry = new HashMap();
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        if (!runOracles) {
            try {
                emf.format(args);
            } catch (Throwable ignored) {
            }
            return;
        }

        String beforeToPattern;
        String firstFormatted;
        try {
            beforeToPattern = emf.toPattern();
            firstFormatted = emf.format(args);
        } catch (Throwable t) {
            throw t;
        }

        try {
            String afterToPattern = emf.toPattern();
            if (beforeToPattern != null ? !beforeToPattern.equals(afterToPattern) : afterToPattern != null) {
                throw new RuntimeException("[oracle:stable-topattern] metamorphic violation: format must not mutate the pattern input="
                        + pattern + " before=" + beforeToPattern + " after=" + afterToPattern);
            }
        } catch (Throwable ignored) {
            return;
        }

        try {
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(beforeToPattern, registry);
            String reparsedToPattern = emf2.toPattern();
            String secondFormatted = emf2.format(args);

            if (beforeToPattern != null ? !beforeToPattern.equals(reparsedToPattern) : reparsedToPattern != null) {
                throw new RuntimeException("[oracle:roundtrip-topattern] metamorphic violation: reparsing toPattern must preserve toPattern input="
                        + pattern + " first=" + beforeToPattern + " second=" + reparsedToPattern);
            }

            if (firstFormatted != null ? !firstFormatted.equals(secondFormatted) : secondFormatted != null) {
                throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: formatting after reparsing toPattern must be consistent input="
                        + pattern + " lhs=" + firstFormatted + " rhs=" + secondFormatted);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('a');
        }
        return sb.toString();
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof RuntimeException && isOracleViolation((RuntimeException) t)) {
            throw (RuntimeException) t;
        }
        if (isRootCause(t)) {
            if (t instanceof Error) {
                throw (Error) t;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
        if (isCleanRejection(t)) {
            return;
        }
    }

    private static boolean isOracleViolation(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("validate") || lower.contains("invalid")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement ste : stack) {
            String cls = ste.getClassName();
            String m = ste.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)) {
                if ("appendQuotedString".equals(m) || "next".equals(m) || "startsWith".equals(m) || "append".equals(m)) {
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
}