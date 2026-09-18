package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Map REGISTRY = buildRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat emfAnchor = new ExtendedMessageFormat(anchor, REGISTRY);
        String anchorOut = emfAnchor.format(new Object[] {"DUMMY"});
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: expected it's a dummy test! got=" + anchorOut);
        }

        String prefix = clean(data.consumeAsciiString(12));
        String infix = clean(data.consumeAsciiString(12));
        String suffix = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        int shape = data.consumeInt(0, 5);
        String pattern;
        switch (shape) {
            case 0:
                pattern = prefix + "''" + infix + " {0," + fmt + "} '" + quoted + "'" + suffix;
                break;
            case 1:
                pattern = "''" + prefix + " {0," + fmt + "} '" + quoted + "'" + suffix;
                break;
            case 2:
                pattern = prefix + "''s a {0," + fmt + "} '" + quoted + "'!";
                break;
            case 3:
                pattern = prefix + "''" + infix + "{0," + fmt + "} '" + quoted + "'";
                break;
            case 4:
                pattern = prefix + "''" + infix + " {0," + fmt + "} '" + quoted + "'!";
                break;
            default:
                pattern = prefix + "''" + infix + " and {0," + fmt + "} '" + quoted + "' " + suffix;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String before = emf.toPattern();
        String out1 = emf.format(new Object[] {"DUMMY"});
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: before=" + before + " after=" + after + " input=" + pattern);
        }

        ExtendedMessageFormat roundTrip = new ExtendedMessageFormat(before, REGISTRY);
        String out2 = roundTrip.format(new Object[] {"DUMMY"});
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " first=" + out1 + " second=" + out2);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('a');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('a');
        }
        return sb.toString();
    }

    private static Map buildRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
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