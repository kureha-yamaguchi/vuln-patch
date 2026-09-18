package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase());
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase());
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String prefix = sanitize(data.consumeAsciiString(16));
        String mid = sanitize(data.consumeAsciiString(16));
        String suffix = sanitize(data.consumeAsciiString(16));
        boolean quotedTail = data.consumeBoolean();
        boolean useLower = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (prefix.length() > 0) {
            pattern.append(prefix);
        } else {
            pattern.append('a');
        }
        pattern.append("''");
        pattern.append(mid.length() > 0 ? mid : "b");
        pattern.append(" {0,");
        pattern.append(useLower ? "lower" : "upper");
        pattern.append("} ");
        if (quotedTail) {
            pattern.append('\'');
            pattern.append(suffix.length() > 0 ? suffix : "test");
            pattern.append('\'');
        } else {
            pattern.append(suffix.length() > 0 ? suffix : "test");
        }

        new ExtendedMessageFormat(pattern.toString(), registry);
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == ',') {
                out.append('x');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}