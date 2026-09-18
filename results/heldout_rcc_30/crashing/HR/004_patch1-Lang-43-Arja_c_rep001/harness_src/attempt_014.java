package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Map REGISTRY = new HashMap();

    static {
        REGISTRY.put("lower", new LowerCaseFormatFactory());
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", REGISTRY);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=it''s a {0,lower} 'test'! got=" + anchorOut);
        }

        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitize(data.consumeAsciiString(16));
        String d = sanitize(data.consumeAsciiString(16));

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = a + "''" + b + " {0,lower} '" + c + "'";
                break;
            case 1:
                pattern = "'" + a + "' " + b + "''" + c + " {0,lower}";
                break;
            case 2:
                pattern = a + " {0,lower} '' " + b + " '" + c + "'";
                break;
            default:
                pattern = a + "''" + b + " {0,lower} '" + c + "' " + d;
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, REGISTRY);
        String out1 = emf1.format(new Object[] { "DuMmY" });
        String canonical = emf1.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(canonical, REGISTRY);
        String out2 = emf2.format(new Object[] { "DuMmY" });

        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: pattern=" + pattern + " canonical=" + canonical + " lhs=" + out1 + " rhs=" + out2);
        }

        String canonical2 = emf2.toPattern();
        if (!canonical.equals(canonical2)) {
            throw new RuntimeException("[oracle:topattern-idempotent] metamorphic violation: pattern=" + pattern + " first=" + canonical + " second=" + canonical2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        return s.replace('\'', 'x').replace('{', 'x').replace('}', 'y');
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        private LowerCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int index = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(index).toLowerCase(locale);
        }
    }
}