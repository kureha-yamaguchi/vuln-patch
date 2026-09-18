package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map nonNullRegistry = new HashMap();

        ExtendedMessageFormat direct = new ExtendedMessageFormat("''", nonNullRegistry);
        String directOut = direct.format(new Object[0]);
        String jdkDirectOut = new MessageFormat("''").format(new Object[0]);
        if (!jdkDirectOut.equals(directOut)) {
            throw new RuntimeException(
                    "[oracle:direct-quote] metamorphic violation: pattern='' lhs=" + directOut + " rhs=" + jdkDirectOut);
        }
        if (!"''".equals(direct.toPattern())) {
            throw new RuntimeException(
                    "[oracle:direct-roundtrip] metamorphic violation: pattern='' toPattern=" + direct.toPattern());
        }

        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String exactPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat exact = new ExtendedMessageFormat(exactPattern, registry);
        String exactOut = exact.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(exactOut)) {
            throw new RuntimeException(
                    "[oracle:anchor] metamorphic violation: pattern=" + exactPattern + " out=" + exactOut);
        }
        if (!exactPattern.equals(exact.toPattern())) {
            throw new RuntimeException(
                    "[oracle:anchor-roundtrip] metamorphic violation: pattern=" + exactPattern + " toPattern=" + exact.toPattern());
        }

        String literal1 = sanitizeLiteral(data.consumeAsciiString(12));
        String literal2 = sanitizeLiteral(data.consumeAsciiString(12));
        String literal3 = sanitizeLiteral(data.consumeAsciiString(12));
        String quoted = sanitizeQuoted(data.consumeAsciiString(12));
        String arg = data.consumeString(24);

        String pattern = "''" + literal1 + "{0,lower}" + literal2 + " '" + quoted + "'" + literal3;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });

        String plainPattern = pattern.replace("{0,lower}", "{0}");
        String expected = new MessageFormat(plainPattern).format(new Object[] { String.valueOf(arg).toLowerCase() });

        if (!expected.equals(actual)) {
            throw new RuntimeException(
                    "[oracle:lower-equiv] metamorphic violation: pattern=" + pattern
                            + " actual=" + actual + " expected=" + expected);
        }
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
            return toAppendTo.append(upper ? s.toUpperCase(locale) : s.toLowerCase(locale));
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }
}