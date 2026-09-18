package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(String.valueOf(obj).toLowerCase());
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(String.valueOf(obj).toUpperCase());
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String clean(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static void drive(String pattern, String arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, makeRegistry());

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toPattern changed across format input=" + pattern + " before=" + before + " after=" + after);
        }

        String out2 = emf.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:det] metamorphic violation: repeated format must be deterministic input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        drive("it''s a {0,lower} 'test'!", "DUMMY");

        int variants = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < variants; i++) {
            String pre = clean(data.consumeAsciiString(8));
            String mid = clean(data.consumeAsciiString(8));
            String lit = clean(data.consumeAsciiString(8));
            String post = clean(data.consumeAsciiString(8));
            String arg = clean(data.consumeAsciiString(8));

            if (mid.length() == 0) {
                mid = "x";
            }
            if (lit.length() == 0) {
                lit = "test";
            }
            if (arg.length() == 0) {
                arg = "DUMMY";
            }

            String fmt = data.consumeBoolean() ? "lower" : "upper";
            boolean withSpaceBeforeFormat = data.consumeBoolean();
            boolean withSpaceBeforeQuoted = data.consumeBoolean();

            StringBuilder pattern = new StringBuilder();
            pattern.append(pre);
            pattern.append("''");
            pattern.append(mid);
            if (withSpaceBeforeFormat) {
                pattern.append(' ');
            }
            pattern.append("{0,");
            pattern.append(fmt);
            pattern.append("}");
            if (withSpaceBeforeQuoted) {
                pattern.append(' ');
            }
            pattern.append('\'');
            pattern.append(lit);
            pattern.append('\'');
            pattern.append(post);

            drive(pattern.toString(), arg);
        }
    }
}