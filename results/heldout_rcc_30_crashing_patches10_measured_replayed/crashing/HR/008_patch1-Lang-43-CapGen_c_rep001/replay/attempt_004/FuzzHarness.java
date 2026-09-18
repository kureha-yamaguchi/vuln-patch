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

        // Exact upstream regression trigger. On the buggy version this constructor call
        // reaches applyPattern() -> appendQuotedString(..., true) at the first quote in
        // "it''s...". Because the buggy code does not advance ParsePosition before the
        // early-return path, it reprocesses the same quote forever and eventually throws
        // OutOfMemoryError. Let that unchecked error escape to Jazzer.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        // Also exercise close variants with the same root-cause property: a quote seen by
        // applyPattern() while registry != null.
        String word1 = safeAtom(data.consumeAsciiString(12));
        String word2 = safeAtom(data.consumeAsciiString(12));
        String quoted = safeAtom(data.consumeAsciiString(12));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = word1 + "''" + word2;
                break;
            case 1:
                pattern = word1 + "''" + word2 + " {0," + fmt + "}";
                break;
            case 2:
                pattern = word1 + " {0," + fmt + "} '" + quoted + "'";
                break;
            case 3:
                pattern = "'" + quoted + "'";
                break;
            case 4:
                pattern = "''";
                break;
            default:
                pattern = word1 + "''s a {0," + fmt + "} 'test'!";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        // Post-condition: formatting must not mutate the pattern representation.
        String before = emf.toPattern();
        emf.format(new Object[] { "DuMmY" });
        String after = emf.toPattern();
        if (before != null ? !before.equals(after) : after != null) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern
                    + " before=" + before + " after=" + after);
        }
    }

    private static String safeAtom(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < 32 || ch > 126 || ch == '\'' || ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        return sb.length() == 0 ? "x" : sb.toString();
    }

    static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}