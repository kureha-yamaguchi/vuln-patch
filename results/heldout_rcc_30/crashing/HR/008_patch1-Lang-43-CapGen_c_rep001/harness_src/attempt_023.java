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
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            if (source == null) {
                pos.setIndex(0);
                return null;
            }
            pos.setIndex(source.length());
            return source.toLowerCase(Locale.ROOT);
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            if (source == null) {
                pos.setIndex(0);
                return null;
            }
            pos.setIndex(source.length());
            return source.toUpperCase(Locale.ROOT);
        }
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String literal(String s) {
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

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: pattern=" + anchorPattern + " out=" + anchorOut);
        }
        String anchorBefore = anchor.toPattern();
        String anchorAfter = anchor.toPattern();
        if (!anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: before=" + anchorBefore + " after=" + anchorAfter);
        }

        String prefix = literal(data.consumeAsciiString(16));
        String middle = literal(data.consumeAsciiString(16));
        String suffix = literal(data.consumeAsciiString(16));
        String arg = literal(data.consumeAsciiString(16));
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        int variant = data.consumeInt(0, 3);
        String pattern;
        String expected;
        if (variant == 0) {
            pattern = prefix + "''" + middle + " {0,lower} '" + suffix + "'";
            expected = prefix + "'" + middle + " " + arg.toLowerCase(Locale.ROOT) + " " + suffix;
        } else if (variant == 1) {
            pattern = "''" + prefix + " {0,lower} '" + suffix + "'";
            expected = "'" + prefix + " " + arg.toLowerCase(Locale.ROOT) + " " + suffix;
        } else if (variant == 2) {
            pattern = prefix + "''s a {0,lower} '" + suffix + "'!";
            expected = prefix + "'s a " + arg.toLowerCase(Locale.ROOT) + " " + suffix + "!";
        } else {
            pattern = prefix + "''" + middle + " {0,upper} '" + suffix + "'";
            expected = prefix + "'" + middle + " " + arg.toUpperCase(Locale.ROOT) + " " + suffix;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String out2 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern + " before=" + before + " after=" + after);
        }
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:determinism] metamorphic violation: pattern=" + pattern + " out1=" + out1 + " out2=" + out2);
        }
        if (!expected.equals(out1)) {
            throw new RuntimeException("[oracle:format] metamorphic violation: pattern=" + pattern + " expected=" + expected + " got=" + out1);
        }

        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(before, registry);
        String out3 = reparsed.format(new Object[] { arg });
        if (!out1.equals(out3)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " toPattern=" + before + " lhs=" + out1 + " rhs=" + out3);
        }
    }
}