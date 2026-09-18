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

        // ANCHOR: exact failing test input. On the buggy version, applyPattern() reaches
        // appendQuotedString() with pos at the first quote of the doubled-quote sequence in "it''s".
        // The buggy code returns without advancing pos, so the outer loop keeps reprocessing the
        // same quote and grows buffers until OutOfMemoryError. The fixed code advances first.
        ExtendedMessageFormat emf = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String formatted = emf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(formatted)) {
            throw new RuntimeException("[oracle:seed-output] metamorphic violation: expected=it's a dummy test! actual=" + formatted);
        }

        // POST-CONDITION: toPattern() is the object's externally visible pattern representation.
        // Formatting should not mutate it; a throw-deleting or branch-skipping patch that damages
        // parser bookkeeping can violate this while avoiding the crash.
        String before = emf.toPattern();
        emf.format(new Object[] { "DUMMY" });
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: before=" + before + " after=" + after);
        }

        // EXPLORE: build more patterns that exercise the same changed condition: a doubled quote
        // outside a format element while a custom format is present, so parsing goes through the
        // same public entry point and appendQuotedString(..., escapingOn=true).
        String left = clean(data.consumeAsciiString(12));
        String right = clean(data.consumeAsciiString(12));
        String lit = cleanNonEmpty(data.consumeAsciiString(8));
        String arg = cleanNonEmpty(data.consumeAsciiString(12));

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = left + "''" + right + " {0,lower}";
                break;
            case 1:
                pattern = left + "''" + right + " {0,lower} '" + lit + "'";
                break;
            case 2:
                pattern = "''" + left + " {0,upper}";
                break;
            case 3:
                pattern = left + " {0,lower} ''" + right;
                break;
            default:
                pattern = "'" + lit + "' " + left + "''" + right + " {0,lower}";
                break;
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);
        String p1 = emf2.toPattern();
        String out1 = emf2.format(new Object[] { arg });

        // Round-trip metamorphic check: rebuilding from toPattern() must preserve formatting
        // behavior for the same argument on any correct implementation.
        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf3.toPattern();
        String out2 = emf3.format(new Object[] { arg });
        if (!p1.equals(p2) || !out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: input=" + pattern + " p1=" + p1 + " p2=" + p2 + " out1=" + out1 + " out2=" + out2);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String cleanNonEmpty(String s) {
        String out = clean(s);
        return out.length() == 0 ? "x" : out;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }
}