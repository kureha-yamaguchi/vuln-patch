package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = buildRegistry();

        runPattern("it''s a {0,lower} 'test'!", "DUMMY", registry, true);

        String a = sanitizeLiteral(data.consumeAsciiString(12));
        String b = sanitizeLiteral(data.consumeAsciiString(12));
        String c = sanitizeLiteral(data.consumeAsciiString(12));
        String quoted = sanitizeQuoted(data.consumeAsciiString(8));
        String arg = data.consumeString(24);

        StringBuilder pattern = new StringBuilder();
        pattern.append(a);
        pattern.append("''");
        if (b.length() > 0) {
            pattern.append(b).append(' ');
        }
        pattern.append("{0,lower}");
        pattern.append(" '").append(quoted).append("'");
        pattern.append(c);

        runPattern(pattern.toString(), arg, registry, false);
    }

    private static void runPattern(String pattern, String arg, Map registry, boolean anchor) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String toPattern = emf.toPattern();
            if (!pattern.equals(toPattern)) {
                throw new RuntimeException(
                        "[oracle:toPattern] metamorphic violation: original pattern must round-trip through toPattern "
                                + "input=" + pattern + " toPattern=" + toPattern);
            }

            String lhs = emf.format(new Object[] { arg });

            String equivalentPattern = pattern.replace("{0,lower}", "{0}");
            ExtendedMessageFormat plain = new ExtendedMessageFormat(equivalentPattern);
            String rhs = plain.format(new Object[] { String.valueOf(arg).toLowerCase() });

            if (!lhs.equals(rhs)) {
                throw new RuntimeException(
                        "[oracle:lower-equiv] metamorphic violation: formatting with {0,lower} must match formatting "
                                + "the equivalent plain pattern with a lowercased argument input=" + pattern
                                + " arg=" + arg + " lhs=" + lhs + " rhs=" + rhs);
            }

            if (anchor) {
                String expected = "it's a dummy test!";
                if (!expected.equals(lhs)) {
                    throw new RuntimeException(
                            "[oracle:anchor] metamorphic violation: anchor output mismatch input=" + pattern
                                    + " lhs=" + lhs + " expected=" + expected);
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
            if (name.contains("Validate") || name.contains("Validation")) {
                return true;
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
            String m = ste.getMethodName();
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

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'' || ch == ',') {
                continue;
            }
            if (Character.isISOControl(ch)) {
                continue;
            }
            out.append(ch);
        }
        return out.toString();
    }

    private static String sanitizeQuoted(String s) {
        String v = sanitizeLiteral(s);
        return v.length() == 0 ? "x" : v;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean upper;
        private final Locale locale;

        CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            s = upper ? s.toUpperCase(locale) : s.toLowerCase(locale);
            return toAppendTo.append(s);
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }
}