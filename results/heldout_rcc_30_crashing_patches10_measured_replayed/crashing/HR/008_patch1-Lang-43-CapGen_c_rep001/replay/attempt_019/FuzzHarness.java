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
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // ANCHOR: exact failing-test setup. On the buggy version, any leading quote handling
        // in applyPattern() with a non-null registry reaches appendQuotedString() and loops
        // because pos is not advanced before the escapingOn fast-path.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: expected=it's a dummy test! actual=" + anchorOut);
        }
        String anchorPattern1 = anchor.toPattern();
        String anchorPattern2 = anchor.toPattern();
        if (anchorPattern1 == null ? anchorPattern2 != null : !anchorPattern1.equals(anchorPattern2)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: first=" + anchorPattern1 + " second=" + anchorPattern2);
        }

        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitize(data.consumeAsciiString(16));
        String d = sanitize(data.consumeAsciiString(16));

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = a + "''" + b + " {0,lower} '" + c + "'";
                break;
            case 1:
                pattern = "'" + a + "' " + b + "'' " + "{0,lower} " + c;
                break;
            case 2:
                pattern = a + " {0,lower} ''" + b + " '" + c + "'";
                break;
            case 3:
                pattern = "it''s " + a + " {0,lower} '" + b + "' " + c;
                break;
            case 4:
                pattern = "'" + a + "'" + " {0,lower} " + b + "''" + c + "'" + d + "'";
                break;
            default:
                pattern = a + "''" + b + " {0,lower} '" + c + "' " + d;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        // Sound metamorphic relation: the "lower" format lower-cases its argument, so formatting
        // the same valid pattern with "DUMMY" and "dummy" must produce the same output.
        String outUpper = emf.format(new Object[] { "DUMMY" });
        String outLower = emf.format(new Object[] { "dummy" });
        if (!outUpper.equals(outLower)) {
            throw new RuntimeException("[oracle:lower-eq] metamorphic violation: pattern=" + pattern + " upper=" + outUpper + " lower=" + outLower);
        }

        // Observable post-condition: formatting should not mutate the stored pattern representation.
        String p1 = emf.toPattern();
        String p2 = emf.toPattern();
        if (p1 == null ? p2 != null : !p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: pattern=" + pattern + " first=" + p1 + " second=" + p2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',' || ch == '\'') {
                sb.append('x');
            } else if (ch < 0x20) {
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

    public static class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    public static class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    public static class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            toAppendTo.append(obj.toString().toLowerCase());
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source.toLowerCase();
        }
    }

    public static class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            toAppendTo.append(obj.toString().toUpperCase());
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source.toUpperCase();
        }
    }
}