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
        HashMap registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // Exact anchor from the failing test. This is the real public entry point and the same setup.
        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(anchor, registry);

        // Observable contract from the test: the parsed pattern must format identically to the test.
        String out = emf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(out)) {
            throw new RuntimeException("[oracle:seed-output] metamorphic violation: actual=" + out);
        }

        // Formatting should be read-only with respect to the externally visible pattern.
        String pBefore = emf.toPattern();
        emf.format(new Object[] { "DUMMY" });
        String pAfter = emf.toPattern();
        if (!pBefore.equals(pAfter)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: before=" + pBefore + " after=" + pAfter);
        }

        // Explore the changed condition: inputs containing doubled quotes outside format elements
        // must still parse and preserve formatting behavior through the same constructor path.
        String prefix = clean(data.consumeAsciiString(16));
        String middle = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeAsciiString(16));
        String lit = cleanNonEmpty(data.consumeAsciiString(12));
        String arg = cleanNonEmpty(data.consumeAsciiString(16));

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = prefix + "''" + middle + " {0,lower}";
                break;
            case 1:
                pattern = prefix + "''" + middle + " {0,lower} '" + lit + "'";
                break;
            case 2:
                pattern = "''" + prefix + " {0,lower} " + suffix;
                break;
            case 3:
                pattern = prefix + " {0,lower} ''" + middle;
                break;
            case 4:
                pattern = "'" + lit + "' " + prefix + "''" + middle + " {0,upper}";
                break;
            default:
                pattern = prefix + "''" + middle + " {0,lower} 'test'!";
                break;
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);
        String patternRoundTrip = emf2.toPattern();
        String out1 = emf2.format(new Object[] { arg });

        // Round-trip guarantee: rebuilding from toPattern() should preserve behavior.
        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(patternRoundTrip, registry);
        String out2 = emf3.format(new Object[] { arg });
        if (!out1.equals(out2) || !patternRoundTrip.equals(emf3.toPattern())) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: input=" + pattern + " p1=" + patternRoundTrip + " p2=" + emf3.toPattern() + " out1=" + out1 + " out2=" + out2);
        }
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
        String r = clean(s);
        return r.length() == 0 ? "x" : r;
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
            toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
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
            toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }
}