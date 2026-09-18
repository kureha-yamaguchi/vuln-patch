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

    private static void exercise(String pattern, String arg, String expected) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, makeRegistry());

        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toPattern changed across format input=" + pattern + " before=" + before + " after=" + after);
        }

        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:fmt] metamorphic violation: formatting with escaped quote and quoted literal must preserve apostrophe semantics input=" + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        String actual2 = emf.format(new Object[] { arg });
        if (!actual.equals(actual2)) {
            throw new RuntimeException("[oracle:det] metamorphic violation: repeated format on same input must be deterministic input=" + pattern + " lhs=" + actual + " rhs=" + actual2);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exercise("it''s a {0,lower} 'test'!", "DUMMY", "it's a dummy test!");

        String pre = clean(data.consumeAsciiString(6));
        String word1 = clean(data.consumeAsciiString(6));
        String literal = clean(data.consumeAsciiString(6));
        String post = clean(data.consumeAsciiString(6));
        String arg = clean(data.consumeAsciiString(6));

        if (word1.length() == 0) {
            word1 = "a";
        }
        if (literal.length() == 0) {
            literal = "test";
        }
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        boolean lower = data.consumeBoolean();
        boolean space1 = data.consumeBoolean();
        boolean space2 = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        pattern.append(pre);
        pattern.append("it''s");
        if (space1) {
            pattern.append(' ');
        }
        pattern.append(word1);
        if (space1) {
            pattern.append(' ');
        }
        pattern.append("{0,");
        pattern.append(lower ? "lower" : "upper");
        pattern.append("}");
        if (space2) {
            pattern.append(' ');
        }
        pattern.append('\'');
        pattern.append(literal);
        pattern.append('\'');
        pattern.append(post);

        StringBuilder expected = new StringBuilder();
        expected.append(pre);
        expected.append("it's");
        if (space1) {
            expected.append(' ');
        }
        expected.append(word1);
        if (space1) {
            expected.append(' ');
        }
        expected.append(lower ? arg.toLowerCase() : arg.toUpperCase());
        if (space2) {
            expected.append(' ');
        }
        expected.append(literal);
        expected.append(post);

        exercise(pattern.toString(), arg, expected.toString());
    }
}