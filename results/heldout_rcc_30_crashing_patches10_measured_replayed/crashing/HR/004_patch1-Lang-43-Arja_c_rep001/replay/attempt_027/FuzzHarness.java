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

        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchor, registry);
        String anchorOut = anchorEmf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: input=" + anchor + " output=" + anchorOut);
        }

        String prefix = clean(data.consumeAsciiString(12));
        String middle = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String arg = clean(data.consumeAsciiString(12));
        boolean useLower = data.consumeBoolean();

        if (prefix.length() == 0) {
            prefix = "it";
        }
        if (middle.length() == 0) {
            middle = "s";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String fmt = useLower ? "lower" : "upper";
        String pattern = prefix + "''" + middle + " {0," + fmt + "} '" + quoted + "'!";

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { arg });

        String expectedArg = useLower ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);
        String expected = prefix + "'" + middle + " " + expectedArg + " " + quoted + "!";
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:format] metamorphic violation: input=" + pattern + " output=" + out + " expected=" + expected);
        }

        String p1 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        if (!p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-idempotence] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    public static class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    public static class LowerCaseFormat extends Format {
        private final Locale locale;

        public LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    public static class UpperCaseFormat extends Format {
        private final Locale locale;

        public UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}