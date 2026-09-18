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

        ExtendedMessageFormat anchor = new ExtendedMessageFormat("", Locale.getDefault(), registry);
        try {
            anchor.applyPattern("it''s a {0,lower} 'test'!");
        } catch (IllegalArgumentException ignored) {
        }

        String left = sanitize(data.consumeAsciiString(24));
        String middle = sanitize(data.consumeAsciiString(24));
        String right = sanitize(data.consumeAsciiString(24));
        String arg = sanitize(data.consumeAsciiString(24));
        if (arg.length() == 0) {
            arg = "X";
        }

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = left + "''" + middle + "{0}" + right;
                break;
            case 1:
                pattern = left + "{0}" + middle + "''" + right;
                break;
            case 2:
                pattern = left + "''" + middle + " '{0}' " + right + "{0}";
                break;
            case 3:
                pattern = left + "'q'" + middle + "''" + right + "{0}";
                break;
            default:
                pattern = left + "''" + middle + " {0} " + right;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat("", Locale.getDefault(), registry);
        emf.applyPattern(pattern);

        String before = emf.toPattern();
        String formatted1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:stable-topattern] metamorphic violation: format must not change toPattern input=" + pattern + " before=" + before + " after=" + after);
        }

        ExtendedMessageFormat reparsed = new ExtendedMessageFormat("", Locale.getDefault(), registry);
        reparsed.applyPattern(before);
        String formatted2 = reparsed.format(new Object[] { arg });
        String reparsedPattern = reparsed.toPattern();

        if (!formatted1.equals(formatted2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: parsing toPattern and formatting again must preserve output input=" + pattern + " lhs=" + formatted1 + " rhs=" + formatted2);
        }

        if (!before.equals(reparsedPattern)) {
            throw new RuntimeException("[oracle:roundtrip-topattern] metamorphic violation: applying toPattern and reading it back must be idempotent input=" + pattern + " lhs=" + before + " rhs=" + reparsedPattern);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuffer out = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '{' && ch != '}' && ch != '\'') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('a');
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
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        private LowerCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(s.toLowerCase(locale));
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        private UpperCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(s.toUpperCase(locale));
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}