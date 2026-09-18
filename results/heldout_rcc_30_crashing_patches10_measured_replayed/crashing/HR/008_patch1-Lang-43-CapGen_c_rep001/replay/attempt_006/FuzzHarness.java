package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Map REGISTRY = createRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Anchor: exact trigger from the failing test. On the buggy version this constructor call
        // reaches ExtendedMessageFormat.applyPattern -> appendQuotedString with pos at a quote and
        // escapingOn=true, and appendQuotedString fails to advance pos before returning, causing the
        // outer loop to spin until OutOfMemoryError.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", REGISTRY);

        // Explore the patched condition boundary with related real patterns that begin a quoted
        // segment via doubled quotes and still use a registered custom format so the same parser
        // path is exercised through the public API.
        String prefix = clean(data.consumeAsciiString(16));
        String middle = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeAsciiString(16));
        String literal = cleanNonEmpty(data.consumeAsciiString(12));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = prefix + "''" + middle + " {0,lower}";
                break;
            case 1:
                pattern = prefix + "''" + middle + " {0,lower} '" + literal + "'";
                break;
            case 2:
                pattern = "''" + prefix + " {0,lower} " + suffix;
                break;
            case 3:
                pattern = prefix + " {0,lower} ''" + middle;
                break;
            case 4:
                pattern = prefix + "''" + middle + " {0,upper} '" + literal + "' " + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + " {0,lower} 'test'!";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        // Post-condition: formatting must not mutate the object's pattern representation.
        String before = emf.toPattern();
        emf.format(new Object[] { cleanNonEmpty(data.consumeAsciiString(16)) });
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: before=" + before + " after=" + after + " input=" + pattern);
        }
    }

    private static Map createRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String cleanNonEmpty(String s) {
        String out = clean(s);
        return out.length() == 0 ? "x" : out;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }
}