package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Map REGISTRY = makeRegistry();
    private static final Object[] ANCHOR_ARGS = new Object[] { "DUMMY" };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, REGISTRY);
        String anchorOut = anchor.format(ANCHOR_ARGS);
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " output=" + anchorOut);
        }

        String prefix = clean(data.consumeAsciiString(12));
        String infix = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String suffix = clean(data.consumeAsciiString(12));
        String arg = data.consumeString(16);
        if (arg.length() == 0) {
            arg = "Dummy";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }

        String formatName;
        switch (data.consumeInt(0, 1)) {
            case 0:
                formatName = "lower";
                break;
            default:
                formatName = "upper";
                break;
        }

        String pattern = prefix + "''" + infix + " {0," + formatName + "} '" + quoted + "'" + suffix;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " before=" + before + " after=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, REGISTRY);
        String out2 = emf2.format(new Object[] { arg });
        String before2 = emf2.toPattern();
        if (before == null ? before2 != null : !before.equals(before2)) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern + " p1=" + before + " p2=" + before2);
        }
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (c < 32 || c > 126) {
                b.append('x');
            } else {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            b.append('x');
        }
        return b.toString();
    }

    private static Map makeRegistry() {
        HashMap map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }
}

class LowerCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new LowerCaseFormat(locale);
    }
}

class UpperCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new UpperCaseFormat(locale);
    }
}

class LowerCaseFormat extends Format {
    private final Locale locale;

    LowerCaseFormat(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        pos.setIndex(source == null ? 0 : source.length());
        return source == null ? null : source.toLowerCase(locale);
    }
}

class UpperCaseFormat extends Format {
    private final Locale locale;

    UpperCaseFormat(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        pos.setIndex(source == null ? 0 : source.length());
        return source == null ? null : source.toUpperCase(locale);
    }
}