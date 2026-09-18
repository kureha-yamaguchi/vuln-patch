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
        REGISTRY.put("upper", new UpperCaseFormatFactory());
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchor, REGISTRY);
        String anchorOut = anchorEmf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: pattern=" + anchor + " output=" + anchorOut);
        }

        String a = clean(data.consumeAsciiString(24));
        String b = clean(data.consumeAsciiString(24));
        String c = clean(data.consumeAsciiString(24));
        String d = clean(data.consumeAsciiString(24));

        String quoteRun = data.consumeBoolean() ? "''" : "'''";
        String literal1 = data.consumeBoolean() ? "'" + a + "'" : a + quoteRun + b;
        String literal2 = data.consumeBoolean() ? "'" + c + "'" : c + quoteRun + d;

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = literal1 + " {0,lower} " + literal2;
                break;
            case 1:
                pattern = a + quoteRun + " {0,upper} '" + b + "'";
                break;
            case 2:
                pattern = "'" + a + "' {0,lower} " + b + quoteRun + c;
                break;
            case 3:
                pattern = a + " {0,lower} " + b + quoteRun + " " + c;
                break;
            default:
                pattern = a + quoteRun + b + " {0,lower} '" + c + "' " + d;
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, REGISTRY);
        String out1 = emf1.format(new Object[] { "DuMmY" });
        String canon = emf1.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(canon, REGISTRY);
        String out2 = emf2.format(new Object[] { "DuMmY" });

        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: pattern=" + pattern + " canon=" + canon + " lhs=" + out1 + " rhs=" + out2);
        }

        String canon2 = emf2.toPattern();
        if (!canon.equals(canon2)) {
            throw new RuntimeException("[oracle:topattern-idempotent] metamorphic violation: pattern=" + pattern + " first=" + canon + " second=" + canon2);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        return s.replace('{', 'x').replace('}', 'y');
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

        private LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start).toLowerCase(locale);
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        private UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start).toUpperCase(locale);
        }
    }
}