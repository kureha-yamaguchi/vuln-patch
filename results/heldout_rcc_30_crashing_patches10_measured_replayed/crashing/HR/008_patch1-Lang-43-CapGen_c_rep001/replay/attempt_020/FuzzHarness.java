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

        String pattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String actual = emf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(actual)) {
            throw new RuntimeException("[oracle:exact-output] metamorphic violation: expected=it's a dummy test! actual=" + actual);
        }

        String before = emf.toPattern();
        String after = emf.toPattern();
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: before=" + before + " after=" + after);
        }

        String fuzz = sanitize(data.consumeAsciiString(16));
        String varied = fuzz + "''s a {0,lower} 'test'!";
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(varied, registry);
        String out1 = emf2.format(new Object[] { "DUMMY" });
        String out2 = emf2.format(new Object[] { "dummy" });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:lowercase-format] metamorphic violation: pattern=" + varied + " upper=" + out1 + " lower=" + out2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "it";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',' || ch == '\'' || ch < 0x20) {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        return sb.length() == 0 ? "it" : sb.toString();
    }

    public static class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    public static class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    public static class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase());
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source.toLowerCase();
        }
    }

    public static class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase());
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source.toUpperCase();
        }
    }
}