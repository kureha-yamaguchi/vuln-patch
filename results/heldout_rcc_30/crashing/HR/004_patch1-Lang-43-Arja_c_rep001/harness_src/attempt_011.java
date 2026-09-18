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
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorFormatted = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorFormatted)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: got=" + anchorFormatted);
        }
        if (!"it''s a {0,lower} 'test'!".equals(anchor.toPattern())) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: toPattern=" + anchor.toPattern());
        }

        String before = filterLiteral(data.consumeAsciiString(12));
        String between = filterLiteral(data.consumeAsciiString(12));
        String quoted = filterQuoted(data.consumeAsciiString(12));
        String after = filterLiteral(data.consumeAsciiString(12));
        String arg = data.consumeString(24);

        String pattern = before + "''" + between + "{0,lower} '" + quoted + "'" + after;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });

        String equivalent = pattern.replace("{0,lower}", "{0}");
        ExtendedMessageFormat plain = new ExtendedMessageFormat(equivalent);
        String expected = plain.format(new Object[] { String.valueOf(arg).toLowerCase() });

        if (!actual.equals(expected)) {
            throw new RuntimeException(
                "[oracle:lower-equiv] metamorphic violation: pattern=" + pattern
                    + " actual=" + actual + " expected=" + expected);
        }
    }

    private static String filterLiteral(String s) {
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

    private static String filterQuoted(String s) {
        String v = filterLiteral(s);
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