package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercise("it''s a {0} 'test'!", "it's a dummy test!", registry);

        String left = sanitize(data.consumeAsciiString(24));
        String middle = sanitize(data.consumeAsciiString(24));
        String right = sanitize(data.consumeAsciiString(24));

        if (left.length() == 0) {
            left = "a";
        }
        if (middle.length() == 0) {
            middle = "b";
        }
        if (right.length() == 0) {
            right = "c";
        }

        int variant = data.consumeInt(0, 7);
        String pattern;
        String expected;
        switch (variant) {
            case 0:
                pattern = "''" + left + "{0}" + right;
                expected = "'" + left + "dummy" + right;
                break;
            case 1:
                pattern = left + "''" + middle + "{0}" + right;
                expected = left + "'" + middle + "dummy" + right;
                break;
            case 2:
                pattern = left + "{0}" + "''" + right;
                expected = left + "dummy'" + right;
                break;
            case 3:
                pattern = left + "''" + "{0}" + "''" + right;
                expected = left + "'dummy'" + right;
                break;
            case 4:
                pattern = left + " {0} ''" + right;
                expected = left + " dummy '" + right;
                break;
            case 5:
                pattern = left + "''" + middle + " {0} 'x' " + right;
                expected = left + "'" + middle + " dummy x " + right;
                break;
            case 6:
                pattern = "'" + left + "' " + "''" + " {0} " + right;
                expected = left + " ' dummy " + right;
                break;
            default:
                pattern = left + "''" + middle + "''" + right + " {0}";
                expected = left + "'" + middle + "'" + right + " dummy";
                break;
        }

        exercise(pattern, expected, registry);
    }

    private static void exercise(String pattern, String expected, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String toPattern = emf.toPattern();
            if (!pattern.equals(toPattern)) {
                throw new RuntimeException(
                        "[oracle:toPattern] metamorphic violation: input=" + pattern
                                + " toPattern=" + toPattern);
            }

            String formatted = emf.format(new Object[] { "dummy" });
            if (!expected.equals(formatted)) {
                throw new RuntimeException(
                        "[oracle:format] metamorphic violation: input=" + pattern
                                + " expected=" + expected + " actual=" + formatted);
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(toPattern, registry);
                String formatted2 = emf2.format(new Object[] { "dummy" });
                if (!formatted.equals(formatted2)) {
                    throw new RuntimeException(
                            "[oracle:roundtrip] metamorphic violation: input=" + pattern
                                    + " lhs=" + formatted + " rhs=" + formatted2);
                }
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throw (RuntimeException) t;
                }
                if (isRootCause(t)) {
                    rethrow(t);
                }
                if (isCleanRejection(t)) {
                    return;
                }
            }
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isRootCause(t)) {
                rethrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static String sanitize(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == ' ') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.indexOf("Validation") >= 0 || name.indexOf("Invalid") >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            String cls = stack[i].getClassName();
            String method = stack[i].getMethodName();
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

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static void rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}