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
        exercise("it''s a {0,lower} 'test'!", "DUMMY", "it's a dummy test!");

        String left = letters(data.consumeAsciiString(10));
        String mid = letters(data.consumeAsciiString(10));
        String quoted = letters(data.consumeAsciiString(10));
        String right = letters(data.consumeAsciiString(10));
        String arg = nonEmptyLetters(data.consumeAsciiString(10));
        boolean lower = data.consumeBoolean();

        String fmt = lower ? "lower" : "upper";
        String xform = lower ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        String pattern;
        String expected;

        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = left + "''" + mid + " {0," + fmt + "} '" + quoted + "'!";
                expected = left + "'" + mid + " " + xform + " " + quoted + "!";
                break;
            case 1:
                pattern = left + "''" + mid + "{0," + fmt + "} '" + quoted + "' " + right;
                expected = left + "'" + mid + xform + " " + quoted + " " + right;
                break;
            case 2:
                pattern = left + " {0," + fmt + "} " + mid + "''" + right + " '" + quoted + "'";
                expected = left + " " + xform + " " + mid + "'" + right + " " + quoted;
                break;
            case 3:
                pattern = left + "''" + mid + " {0," + fmt + "}";
                expected = left + "'" + mid + " " + xform;
                break;
            default:
                pattern = "{0," + fmt + "} " + left + "''" + mid + " '" + quoted + "'";
                expected = xform + " " + left + "'" + mid + " " + quoted;
                break;
        }

        exercise(pattern, arg, expected);
    }

    private static void exercise(String pattern, String arg, String expected) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        String out = emf.format(new Object[] { arg });
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:format-output] metamorphic violation: valid pattern formatted unexpectedly input="
                    + pattern + " lhs=" + out + " rhs=" + expected);
        }

        String roundTrip = emf.toPattern();
        if (!pattern.equals(roundTrip)) {
            throw new RuntimeException("[oracle:pattern-roundtrip] metamorphic violation: toPattern changed valid pattern input="
                    + pattern + " lhs=" + roundTrip + " rhs=" + pattern);
        }
    }

    private static String letters(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            return "a";
        }
        return sb.toString();
    }

    private static String nonEmptyLetters(String s) {
        String r = letters(s);
        return r.length() == 0 ? "Dummy" : r;
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
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

    private static final class LowerCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}