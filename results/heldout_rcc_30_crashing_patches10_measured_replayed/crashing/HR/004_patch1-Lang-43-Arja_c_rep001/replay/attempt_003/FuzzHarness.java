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

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " out=" + anchorOut);
        }
        String anchorToPattern = anchor.toPattern();
        ExtendedMessageFormat anchorRoundTrip = new ExtendedMessageFormat(anchorToPattern, registry);
        String anchorOut2 = anchorRoundTrip.format(new Object[] { "DUMMY" });
        if (!anchorOut.equals(anchorOut2)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorOut2);
        }

        String a = sanitizeLiteral(data.consumeAsciiString(20));
        String b = sanitizeLiteral(data.consumeAsciiString(20));
        String c = sanitizeLiteral(data.consumeAsciiString(20));
        String d = sanitizeLiteral(data.consumeAsciiString(20));
        String arg = sanitizeLiteral(data.consumeAsciiString(20));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = a + "''" + b + " {" + "0," + fmt + "} '" + c + "'" + d;
                break;
            case 1:
                pattern = a + "''" + b + "{0," + fmt + "}" + c;
                break;
            case 2:
                pattern = a + " '' " + b + " {0," + fmt + "} '" + c + "' " + d;
                break;
            case 3:
                pattern = a + "{0," + fmt + "}" + b + "''" + c + "'x'" + d;
                break;
            default:
                pattern = a + "''" + b + " {0," + fmt + "} " + c;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String once = emf.format(new Object[] { arg });

        String tp = emf.toPattern();
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(tp, registry);
        String twice = reparsed.format(new Object[] { arg });

        if (!once.equals(twice)) {
            throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + once + " rhs=" + twice);
        }

        String tp2 = reparsed.toPattern();
        if (!tp.equals(tp2)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " lhs=" + tp + " rhs=" + tp2);
        }
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '{' && ch != '}' && ch != '\'') {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('a');
        }
        return sb.toString();
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

        LowerCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
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

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
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