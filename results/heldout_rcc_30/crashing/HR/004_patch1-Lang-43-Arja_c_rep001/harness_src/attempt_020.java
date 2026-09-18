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

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: pattern=" + anchorPattern + " actual=" + anchorOut);
        }

        String anchorPattern2 = anchor.toPattern();
        ExtendedMessageFormat anchor2 = new ExtendedMessageFormat(anchorPattern2, registry);
        String anchorOut2 = anchor2.format(new Object[] { "DUMMY" });
        if (!anchorOut.equals(anchorOut2)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: pattern=" + anchorPattern2 + " lhs=" + anchorOut + " rhs=" + anchorOut2);
        }

        String p1 = sanitize(data.consumeAsciiString(12));
        String p2 = sanitize(data.consumeAsciiString(12));
        String p3 = sanitize(data.consumeAsciiString(12));
        String p4 = sanitize(data.consumeAsciiString(12));
        String arg = data.consumeAsciiString(24);
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        String pattern;
        switch (data.consumeInt(0, 7)) {
            case 0:
                pattern = p1 + "''" + p2 + "{0,lower}" + p3;
                break;
            case 1:
                pattern = p1 + "{0,lower}" + p2 + "''" + p3;
                break;
            case 2:
                pattern = "it''s " + p1 + "{0,lower} " + p2;
                break;
            case 3:
                pattern = p1 + "''" + p2 + " {0,upper} " + p3;
                break;
            case 4:
                pattern = p1 + "''" + p2 + " '" + p3 + "' {0,lower}";
                break;
            case 5:
                pattern = "'" + p1 + "' " + p2 + "''" + p3 + " {0,lower} " + p4;
                break;
            case 6:
                pattern = p1 + "{0,lower} '" + p2 + "' " + p3 + "''" + p4;
                break;
            default:
                pattern = p1 + "''" + p2 + "''" + p3 + "{0,lower}" + p4;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { arg });
        String toPattern = emf.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(toPattern, registry);
        String out2 = emf2.format(new Object[] { arg });
        if (!out.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " toPattern=" + toPattern + " lhs=" + out + " rhs=" + out2);
        }

        String toPattern2 = emf2.toPattern();
        if (!toPattern.equals(toPattern2)) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern + " first=" + toPattern + " second=" + toPattern2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder();
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
            toAppendTo.append(lower ? s.toLowerCase(this.locale) : s.toUpperCase(this.locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}