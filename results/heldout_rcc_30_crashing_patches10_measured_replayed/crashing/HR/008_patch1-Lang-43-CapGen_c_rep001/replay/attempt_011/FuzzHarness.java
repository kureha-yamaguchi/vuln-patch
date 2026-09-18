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

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, Locale.getDefault(), registry);
        String anchorFormatted = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorFormatted)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " output=" + anchorFormatted);
        }
        String anchorBefore = anchor.toPattern();
        String anchorAfter = anchor.toPattern();
        if (anchorBefore == null ? anchorAfter != null : !anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: before=" + anchorBefore + " after=" + anchorAfter);
        }

        String a = safeLiteral(data.consumeAsciiString(8));
        String b = safeLiteral(data.consumeAsciiString(8));
        String c = safeLiteral(data.consumeAsciiString(8));
        String d = safeLiteral(data.consumeAsciiString(8));
        String arg = data.consumeString(12);
        if (arg.length() == 0) {
            arg = "Dummy";
        }
        String quoted = safeLiteral(data.consumeAsciiString(8));
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String formatName = data.consumeBoolean() ? "lower" : "upper";

        String pattern = a + "''" + b + " {0," + formatName + "} '" + quoted + "'" + c + "''" + d;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " before=" + before + " after=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, Locale.getDefault(), registry);
        String out2 = emf2.format(new Object[] { arg });
        String before2 = emf2.toPattern();

        if (before == null ? before2 != null : !before.equals(before2)) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern + " lhs=" + before + " rhs=" + before2);
        }
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String safeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else if (ch < 32 || ch > 126) {
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
}

class LowerCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
    }
}

class UpperCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
    }
}

class LowerCaseFormat extends Format {
    private final Locale locale;

    LowerCaseFormat(Locale locale) {
        this.locale = locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            pos.setIndex(0);
            return null;
        }
        pos.setIndex(source.length());
        return source.toLowerCase(locale);
    }
}

class UpperCaseFormat extends Format {
    private final Locale locale;

    UpperCaseFormat(Locale locale) {
        this.locale = locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            pos.setIndex(0);
            return null;
        }
        pos.setIndex(source.length());
        return source.toUpperCase(locale);
    }
}