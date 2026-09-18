package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        // Exact trigger from the failing test. On the buggy version, construction calls
        // ExtendedMessageFormat.applyPattern(), which reaches appendQuotedString() on the
        // doubled quote in "it''s" and loops until OutOfMemoryError.
        exercise(ANCHOR_PATTERN, registry);

        // Explore the root-cause property from the diff: any pattern containing a quote
        // reaches appendQuotedString(..., escapingOn=true) from applyPattern(). In the
        // buggy version, the method returns without advancing ParsePosition when the
        // current character is QUOTE, so repeated scanning can loop forever.
        String left = clean(data.consumeAsciiString(24));
        String mid = clean(data.consumeAsciiString(24));
        String right = clean(data.consumeAsciiString(24));
        String literal = clean(data.consumeAsciiString(24));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String[] patterns = new String[] {
            left + "''" + right,
            left + "''" + mid + " {0," + fmt + "} " + right,
            left + " {0," + fmt + "} '" + literal + "' " + right,
            "'" + literal + "'",
            "''",
            left + " 'x' " + right,
            left + "''s a {0," + fmt + "} 'test'!",
            "{0," + fmt + "} '" + literal + "'",
            left + " {0," + fmt + "} '' " + right
        };

        for (int i = 0; i < patterns.length; i++) {
            exercise(patterns[i], registry);
        }
    }

    private static void exercise(String pattern, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String before = emf.toPattern();
        String formatted = emf.format(new Object[] { "DuMmY" });
        String after = emf.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);
        String before2 = emf2.toPattern();
        String formatted2 = emf2.format(new Object[] { "DuMmY" });
        String after2 = emf2.toPattern();

        // Contract/post-condition:
        // format() is read-only with respect to the compiled message pattern, so toPattern()
        // must stay stable across formatting; and two independently constructed formatters
        // from the same pattern/registry must behave identically.
        if (!eq(before, after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern
                    + " before=" + before + " after=" + after);
        }
        if (!eq(before2, after2)) {
            throw new RuntimeException("[oracle:topattern-stable-fresh] metamorphic violation: pattern=" + pattern
                    + " before=" + before2 + " after=" + after2);
        }
        if (!eq(before, before2) || !eq(after, after2)) {
            throw new RuntimeException("[oracle:constructor-determinism] metamorphic violation: pattern=" + pattern
                    + " before1=" + before + " before2=" + before2
                    + " after1=" + after + " after2=" + after2);
        }
        if (!eq(formatted, formatted2)) {
            throw new RuntimeException("[oracle:format-determinism] metamorphic violation: pattern=" + pattern
                    + " out1=" + formatted + " out2=" + formatted2);
        }
    }

    private static Map makeRegistry() {
        HashMap registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 32 || c > 126 || c == '{' || c == '}' || c == ',') {
                sb.append('x');
            } else {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            return "x";
        }
        return sb.toString();
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