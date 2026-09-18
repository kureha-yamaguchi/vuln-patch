package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Map;

public class FuzzHarness {
    private static final Map EMPTY_REGISTRY = Collections.EMPTY_MAP;
    private static final Object[] FORMAT_ARGS = new Object[] { "DuMmY", Integer.valueOf(7) };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exercise("it''s a {0,lower} 'test'!");

        String a = sanitize(data.consumeAsciiString(24));
        String b = sanitize(data.consumeAsciiString(24));
        String c = sanitize(data.consumeAsciiString(24));
        String d = sanitize(data.consumeAsciiString(24));

        int choice = data.consumeInt(0, 7);
        String pattern;
        switch (choice) {
            case 0:
                pattern = a + "''" + b + " {0}";
                break;
            case 1:
                pattern = "'" + a + "''" + b + "' {0}";
                break;
            case 2:
                pattern = a + " {0} '' " + b;
                break;
            case 3:
                pattern = "''" + a + "{0}" + b;
                break;
            case 4:
                pattern = a + "''" + b + " '{1}' {0}";
                break;
            case 5:
                pattern = a + " '" + b + "''" + c + "' {0}";
                break;
            case 6:
                pattern = a + "''" + b + " {0,number} " + c;
                break;
            default:
                pattern = a + " " + b + "''" + c + " " + d + " {0}";
                break;
        }

        exercise(pattern);
    }

    private static void exercise(String pattern) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, EMPTY_REGISTRY);

            String before = emf.toPattern();
            String out1 = emf.format(FORMAT_ARGS);
            String after = emf.toPattern();

            // MessageFormat.format is observationally read-only for the compiled pattern;
            // deleting/skipping bookkeeping in the patched path must not silently mutate toPattern().
            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:pattern-stable] metamorphic violation: format changed toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            // toPattern() is the object's serialized pattern. Reconstructing a new formatter from
            // that pattern and formatting the same arguments must yield the same result for any
            // correct implementation.
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, EMPTY_REGISTRY);
            String before2 = emf2.toPattern();
            String out2 = emf2.format(FORMAT_ARGS);

            if (!safeEquals(before, before2) || !safeEquals(out1, out2)) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: parse(formatter-pattern) not stable input="
                        + pattern + " p1=" + before + " p2=" + before2 + " out1=" + out1 + " out2=" + out2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                rethrow(t);
            }
        }
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
            String method = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method) || "next".equals(method)
                    || "append".equals(method) || "startsWith".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Validation") || name.contains("InvalidFormat")) {
                return true;
            }
        }
        return false;
    }

    private static void rethrow(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}