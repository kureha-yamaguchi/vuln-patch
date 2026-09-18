package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact failing-test pattern first, through the real public API.
        // We cannot register the test-only factories here, so we still drive the same constructor path
        // and accept clean rejection if this build reaches registry validation before the bug.
        exerciseForCrashOnly("it''s a {0,lower} 'test'!", registry);

        // EXPLORE: generate many valid-by-construction MessageFormat-compatible patterns that contain:
        // - an escaped quote ("''"), which reaches the escapingOn branch, and
        // - a quoted literal section ("'x'"), which exercises quoted-string parsing.
        //
        // Contract asserted below:
        // For patterns without custom registry formats, ExtendedMessageFormat is a MessageFormat extension,
        // so formatting the same valid pattern/arguments must match MessageFormat. A patch that merely skips
        // quote-handling bookkeeping can avoid the crash but silently produce a different parse/format result.
        String a = visibleAscii(data.consumeAsciiString(12));
        String b = visibleAscii(data.consumeAsciiString(12));
        String c = visibleAscii(data.consumeAsciiString(12));
        String d = visibleAscii(data.consumeAsciiString(12));
        String arg = visibleAscii(data.consumeAsciiString(12));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0} '" + c + "'" + d;
                break;
            case 1:
                pattern = "'" + a + "' " + b + "''" + c + " {0}";
                break;
            case 2:
                pattern = a + " {0} '' " + b + " '" + c + "'";
                break;
            case 3:
                pattern = a + "''" + b + " '{0}' " + c + " {0}";
                break;
            case 4:
                pattern = "'" + a + "''" + b + "' {0} " + c;
                break;
            default:
                pattern = a + " '' " + b + " {0} '" + c + "' " + d;
                break;
        }

        exerciseWithOracle(pattern, registry, arg);

        // A second exploration family with some surrounding fuzz noise while preserving validity.
        String tail = visibleAscii(data.consumeAsciiString(10));
        String pattern2 = visibleAscii(data.consumeAsciiString(8)) + "''"
                + visibleAscii(data.consumeAsciiString(8)) + " {0} '"
                + visibleAscii(data.consumeAsciiString(8)) + "' "
                + tail;
        exerciseWithOracle(pattern2, registry, arg);
    }

    private static void exerciseForCrashOnly(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            emf.format(new Object[] {"DUMMY"});
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void exerciseWithOracle(String pattern, Map registry, String arg) {
        try {
            MessageFormat mf = new MessageFormat(pattern);
            String expected = mf.format(new Object[] {arg});

            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String before = emf.toPattern();
            String actual = emf.format(new Object[] {arg});
            String after = emf.toPattern();

            if (!before.equals(after)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input="
                        + pattern + " before=" + before + " after=" + after);
            }
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:mf-equiv] metamorphic violation: ExtendedMessageFormat output differs from MessageFormat input="
                        + pattern + " lhs=" + actual + " rhs=" + expected);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
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
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation")
                || name.contains("Invalid")
                || name.contains("Malformed")
                || name.contains("FormatException");
    }

    private static String visibleAscii(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}') {
                sb.append('X');
            } else if (ch <= 0x20 || ch >= 0x7f) {
                sb.append('Y');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('Z');
        }
        return sb.toString();
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}