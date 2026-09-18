package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    public static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    public static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            if (source != null) {
                pos.setIndex(source.length());
                return source.toLowerCase(Locale.ROOT);
            }
            pos.setIndex(0);
            return null;
        }
    }

    public static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            if (source != null) {
                pos.setIndex(source.length());
                return source.toUpperCase(Locale.ROOT);
            }
            pos.setIndex(0);
            return null;
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String a = clean(data.consumeAsciiString(12));
        String b = clean(data.consumeAsciiString(12));
        String c = clean(data.consumeAsciiString(12));
        if (a.length() == 0) {
            a = "it";
        }
        if (b.length() == 0) {
            b = "s";
        }
        if (c.length() == 0) {
            c = "test";
        }

        int which = data.consumeInt(0, 3);
        String pattern;
        if (which == 0) {
            pattern = a + "''" + b + " a {0,lower} '" + c + "'!";
        } else if (which == 1) {
            pattern = "''" + a + " {0,lower} '" + c + "'!";
        } else if (which == 2) {
            pattern = a + " '' " + b + " {0,lower} '" + c + "'!";
        } else {
            pattern = a + "''s a {0,lower} '" + c + "'!";
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { "DUMMY" });
        String before = emf.toPattern();
        String after = emf.toPattern();
        if (!before.equals(after) || out == null) {
            throw new RuntimeException("[oracle:stable] metamorphic violation: pattern=" + pattern + " before=" + before + " after=" + after + " out=" + out);
        }
    }
}