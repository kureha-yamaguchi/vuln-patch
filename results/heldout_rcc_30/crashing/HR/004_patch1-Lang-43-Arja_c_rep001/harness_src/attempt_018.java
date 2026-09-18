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
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern
                    + " expected=it's a dummy test! actual=" + anchorOut);
        }
        String anchorRoundTrip = new ExtendedMessageFormat(anchor.toPattern(), registry).format(new Object[] { "DUMMY" });
        if (!anchorOut.equals(anchorRoundTrip)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: input=" + anchorPattern
                    + " lhs=" + anchorOut + " rhs=" + anchorRoundTrip);
        }

        String arg = data.consumeAsciiString(32);
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String a = sanitize(data.consumeAsciiString(20));
        String b = sanitize(data.consumeAsciiString(20));
        String c = sanitize(data.consumeAsciiString(20));
        String d = sanitize(data.consumeAsciiString(20));

        String pattern;
        switch (data.consumeInt(0, 7)) {
            case 0:
                pattern = a + "''" + b + "{0,lower}" + c;
                break;
            case 1:
                pattern = a + "{0,lower}" + b + "''" + c;
                break;
            case 2:
                pattern = "''" + a + "{0,lower}" + b;
                break;
            case 3:
                pattern = a + "''" + b + " {0,upper} " + c;
                break;
            case 4:
                pattern = a + " 'Q' " + b + "''" + c + "{0,lower}" + d;
                break;
            case 5:
                pattern = a + "{0,lower}" + b + " 'X' " + c + "''" + d;
                break;
            case 6:
                pattern = a + "''" + b + "''" + c + "{0,lower}" + d;
                break;
            default:
                pattern = a + "{0,lower}" + b + "''" + c + "''" + d;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { arg });

        String reparsedPattern = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reparsedPattern, registry);
        String out2 = emf2.format(new Object[] { arg });

        if (!out.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern
                    + " toPattern=" + reparsedPattern + " lhs=" + out + " rhs=" + out2);
        }
        if (!reparsedPattern.equals(emf2.toPattern())) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern
                    + " first=" + reparsedPattern + " second=" + emf2.toPattern());
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else if (ch == '\'') {
                sb.append('q');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static Map makeRegistry() {
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
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}