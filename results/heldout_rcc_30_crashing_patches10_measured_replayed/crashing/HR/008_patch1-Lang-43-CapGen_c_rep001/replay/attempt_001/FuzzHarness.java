package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        runCase(ANCHOR_PATTERN, registry);

        String a = sanitize(data.consumeAsciiString(32));
        String b = sanitize(data.consumeAsciiString(32));
        String c = sanitize(data.consumeAsciiString(32));
        String d = sanitize(data.consumeAsciiString(32));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String[] patterns = new String[] {
            "''" + nonEmpty(a) + " {0," + fmt + "} '" + nonEmpty(b) + "'",
            nonEmpty(a) + "''" + nonEmpty(b) + " {0," + fmt + "} '" + nonEmpty(c) + "'",
            nonEmpty(a) + " {0," + fmt + "} '' " + nonEmpty(b),
            nonEmpty(a) + " '' " + nonEmpty(b) + " {0," + fmt + "} '" + nonEmpty(c) + "' " + nonEmpty(d),
            "pre '' mid {0," + fmt + "} 'lit' post",
            nonEmpty(a) + "''{0," + fmt + "}",
            "{0," + fmt + "} '' " + nonEmpty(a) + " '" + nonEmpty(b) + "'"
        };

        for (int i = 0; i < patterns.length; i++) {
            runCase(patterns[i], registry);
        }
    }

    private static void runCase(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);

            String before = emf.toPattern();
            String out1;
            try {
                out1 = emf.format(new Object[] { "DuMmY" });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw rethrow(t);
                }
                return;
            }
            String after = emf.toPattern();

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);
                String before2 = emf2.toPattern();
                String out2 = emf2.format(new Object[] { "DuMmY" });
                String after2 = emf2.toPattern();

                // Contract used for this oracle:
                // formatting is a read-only operation with respect to the message pattern,
                // so toPattern() must not change before/after format();
                // and two independently constructed objects with the same pattern/registry
                // must expose the same toPattern() and produce the same formatted output.
                if (!safeEquals(before, after)) {
                    throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                            + pattern + " before=" + before + " after=" + after);
                }
                if (!safeEquals(before2, after2)) {
                    throw new RuntimeException("[oracle:topattern-stable2] metamorphic violation: toPattern changed across format on fresh object input="
                            + pattern + " before=" + before2 + " after=" + after2);
                }
                if (!safeEquals(before, before2) || !safeEquals(after, after2)) {
                    throw new RuntimeException("[oracle:constructor-determinism] metamorphic violation: identical constructions disagree input="
                            + pattern + " lhsBefore=" + before + " rhsBefore=" + before2
                            + " lhsAfter=" + after + " rhsAfter=" + after2);
                }
                if (!safeEquals(out1, out2)) {
                    throw new RuntimeException("[oracle:format-determinism] metamorphic violation: identical constructions disagree on output input="
                            + pattern + " lhs=" + out1 + " rhs=" + out2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw rethrow(t);
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw rethrow(t);
            }
        }
    }

    private static Map makeRegistry() {
        HashMap registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String n = cur.getClass().getName();
            if (n.contains("Validate") || n.contains("Validation") || n.contains("Invalid")) {
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
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(m)
                        || "next".equals(m)
                        || "append".equals(m)
                        || "startsWith".equals(m))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(m) || "startsWith".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        return new RuntimeException(t);
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '{' || c == '}' || c == ',') {
                sb.append('x');
            } else if (c < 32 || c > 126) {
                sb.append('x');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String nonEmpty(String s) {
        return (s == null || s.length() == 0) ? "x" : s;
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}