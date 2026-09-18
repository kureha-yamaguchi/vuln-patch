package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input must format as the test expects input=" + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        String left = literal(data.consumeAsciiString(12));
        String mid = literal(data.consumeAsciiString(12));
        String right = literal(data.consumeAsciiString(12));
        String arg = data.consumeRemainingAsString();

        String pattern;
        String expectedPattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = left + "''" + mid + " {0,lower} " + right;
                expectedPattern = left + "''" + mid + " {0} " + right;
                break;
            case 1:
                pattern = left + " {0,lower} " + mid + "''" + right;
                expectedPattern = left + " {0} " + mid + "''" + right;
                break;
            case 2:
                pattern = left + "''s " + mid + " {0,lower} '" + right + "'";
                expectedPattern = left + "''s " + mid + " {0} '" + right + "'";
                break;
            case 3:
                pattern = "'" + left + "' " + mid + "''" + right + " {0,lower}";
                expectedPattern = "'" + left + "' " + mid + "''" + right + " {0}";
                break;
            case 4:
                pattern = left + " {0,upper} " + mid + "''" + right;
                expectedPattern = left + " {0} " + mid + "''" + right;
                break;
            default:
                pattern = left + "''" + mid + " '{quoted}' " + right + " {0,lower}";
                expectedPattern = left + "''" + mid + " '{quoted}' " + right + " {0}";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        Object expectedArg;
        if (pattern.indexOf("{0,upper}") >= 0) {
            expectedArg = arg == null ? null : arg.toUpperCase();
        } else {
            expectedArg = arg == null ? null : arg.toLowerCase();
        }
        String expected = new MessageFormat(expectedPattern).format(new Object[] { expectedArg });

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:format-equiv] metamorphic violation: custom lower/upper formatting with escaped quotes must match equivalent MessageFormat on transformed argument input=" + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
        }
    }

    private static String literal(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
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

    private static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase());
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int index = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(index).toLowerCase();
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase());
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int index = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(index).toUpperCase();
        }
    }
}