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
    private static final String ANCHOR = "it''s a {0,lower} 'test'!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        trigger(ANCHOR);

        String prefix = lettersOnly(data.consumeAsciiString(20));
        String suffix = lettersOnly(data.consumeAsciiString(20));
        String quoted = lettersOnly(data.consumeAsciiString(20));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern = prefix + "it''s " + suffix + " {0," + fmt + "} '" + quoted + "'!";
        trigger(pattern);
    }

    private static void trigger(String pattern) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        emf.format(new Object[] { "DUMMY" });

        String roundTrip = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(roundTrip, REGISTRY);
        String a = emf.format(new Object[] { "DuMmY" });
        String b = emf2.format(new Object[] { "DuMmY" });
        if (!a.equals(b)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " lhs=" + a + " rhs=" + b);
        }
    }

    private static String lettersOnly(String in) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < in.length() && sb.length() < 20; i++) {
            char c = in.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static Map buildRegistry() {
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
        private static final long serialVersionUID = 1L;
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }
}