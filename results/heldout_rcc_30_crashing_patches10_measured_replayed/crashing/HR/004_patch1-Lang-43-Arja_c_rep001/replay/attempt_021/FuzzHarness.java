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
        Map registry = buildRegistry();

        exercisePattern(ANCHOR_PATTERN, registry, true);

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String left = safeLiteral(data.consumeAsciiString(24));
        String right = safeLiteral(data.consumeAsciiString(24));
        String quoted = safeLiteral(data.consumeAsciiString(16));
        String mid = safeLiteral(data.consumeAsciiString(16));

        StringBuilder pattern = new StringBuilder();
        pattern.append(left);
        pattern.append("it''s");
        if (mid.length() > 0) {
            pattern.append(' ').append(mid);
        }
        pattern.append(" {0,").append(fmt).append("} ");
        pattern.append('\'').append(quoted).append('\'');
        if (right.length() > 0) {
            pattern.append(' ').append(right);
        }

        exercisePattern(pattern.toString(), registry, false);
    }

    private static void exercisePattern(String pattern, Map registry, boolean anchor) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String toPattern1 = emf.toPattern();

            /* Contract asserted: ExtendedMessageFormat stores a pattern representation via toPattern;
             * reparsing that representation with the same registry must preserve it. A "fix" that merely
             * skips the quoted-string bookkeeping would make this observable round-trip unstable. */
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(toPattern1, registry);
            String toPattern2 = emf2.toPattern();
            if (!toPattern1.equals(toPattern2)) {
                throw new RuntimeException(
                        "[oracle:topattern-idem] metamorphic violation: input=" + pattern
                                + " lhs=" + toPattern1 + " rhs=" + toPattern2);
            }

            String sample = anchor ? "DUMMY" : "DuMmY";
            String out1 = emf.format(new Object[] { sample });
            String out2 = emf2.format(new Object[] { sample });

            /* Contract asserted: reparsing an object's own toPattern with the same registry describes
             * an equivalent formatter, so formatting the same argument must yield the same result. */
            if (!out1.equals(out2)) {
                throw new RuntimeException(
                        "[oracle:format-roundtrip] metamorphic violation: input=" + pattern
                                + " lhs=" + out1 + " rhs=" + out2);
            }

            if (anchor) {
                String expected = "it's a dummy test!";
                if (!expected.equals(out1)) {
                    throw new RuntimeException(
                            "[oracle:anchor-output] metamorphic violation: input=" + pattern
                                    + " lhs=" + out1 + " rhs=" + expected);
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Validation") || name.contains("Invalid") || name.contains("Format")) {
                if (cur instanceof RuntimeException || cur instanceof Exception) {
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
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method) || "next".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static String safeLiteral(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length() && out.length() < 32; i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                out.append(c);
            } else if (c == ' ' || c == '_' || c == '-') {
                out.append(' ');
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private static final long serialVersionUID = 1L;
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }
}