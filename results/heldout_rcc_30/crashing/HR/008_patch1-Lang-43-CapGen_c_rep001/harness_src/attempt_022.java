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
            pos.setIndex(source == null ? 0 : source.length());
            return source == null ? null : source.toLowerCase(Locale.ROOT);
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
            pos.setIndex(source == null ? 0 : source.length());
            return source == null ? null : source.toUpperCase(Locale.ROOT);
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

    private static String cleanLiteral(String s) {
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
        String anchorFormatted = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorFormatted)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: input=" + anchorPattern + " got=" + anchorFormatted);
        }
        String anchorBefore = anchor.toPattern();
        String anchorAgain = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();
        if (!anchorBefore.equals(anchorAfter) || !anchorFormatted.equals(anchorAgain)) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: before=" + anchorBefore + " after=" + anchorAfter + " once=" + anchorFormatted + " twice=" + anchorAgain);
        }

        String left = cleanLiteral(data.consumeAsciiString(20));
        String right = cleanLiteral(data.consumeAsciiString(20));
        String tail = cleanLiteral(data.consumeAsciiString(20));
        String arg = cleanLiteral(data.consumeAsciiString(20));
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        int variant = data.consumeInt(0, 3);
        String pattern;
        if (variant == 0) {
            pattern = left + "''" + right + " {0,lower} '" + tail + "'";
        } else if (variant == 1) {
            pattern = "''" + left + " {0,lower} '" + tail + "'";
        } else if (variant == 2) {
            pattern = left + " '' " + right + " {0,upper} '" + tail + "'";
        } else {
            pattern = left + "''s {0,lower} '" + tail + "'!";
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String out2 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after) || !out1.equals(out2)) {
            throw new RuntimeException("[oracle:determinism] metamorphic violation: pattern=" + pattern + " before=" + before + " after=" + after + " out1=" + out1 + " out2=" + out2);
        }

        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(before, registry);
        String out3 = reparsed.format(new Object[] { arg });
        if (!out1.equals(out3)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " toPattern=" + before + " lhs=" + out1 + " rhs=" + out3);
        }
    }
}