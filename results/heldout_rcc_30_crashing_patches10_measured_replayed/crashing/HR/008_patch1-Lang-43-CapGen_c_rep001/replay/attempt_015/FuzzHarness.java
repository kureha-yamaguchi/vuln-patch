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
            String s = String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(Locale.ENGLISH));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(Locale.ENGLISH));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        // Anchor: exact regression test input. On the buggy version this reaches
        // ExtendedMessageFormat.applyPattern -> appendQuotedString and triggers OOME.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: expected=it's a dummy test! actual=" + anchorOut);
        }

        // Explore the same root cause with varied valid patterns that contain a doubled quote.
        // The changed line advances past the opening quote before scanning; so we deliberately
        // build inputs beginning a quoted-string parse at a quote that is immediately followed
        // by another quote somewhere in the pattern.
        String p1 = sanitize(data.consumeAsciiString(12));
        String p2 = sanitize(data.consumeAsciiString(12));
        String p3 = sanitize(data.consumeAsciiString(12));
        String arg = data.consumeString(12);
        boolean useLower = data.consumeBoolean();
        boolean addQuotedTail = data.consumeBoolean();
        boolean addSecondEscapedQuote = data.consumeBoolean();

        if (p1.length() == 0) p1 = "a";
        if (p2.length() == 0) p2 = "b";
        if (p3.length() == 0) p3 = "c";

        StringBuilder pattern = new StringBuilder();
        pattern.append(p1);
        pattern.append("''");
        pattern.append(p2);
        pattern.append(" {0,");
        pattern.append(useLower ? "lower" : "upper");
        pattern.append("}");
        pattern.append(' ');
        if (addQuotedTail) {
            pattern.append('\'').append(p3).append('\'');
        } else {
            pattern.append(p3);
        }
        if (addSecondEscapedQuote) {
            pattern.append(' ');
            pattern.append("q''q");
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern.toString(), registry);
        String out1 = emf.format(new Object[] { arg });

        // Post-condition / metamorphic check:
        // reparsing the formatter's own toPattern with the same registry must preserve behavior.
        String reparsedPattern = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reparsedPattern, registry);
        String out2 = emf2.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: pattern=" + pattern
                    + " toPattern=" + reparsedPattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String sanitize(String s) {
        return s.replace("{", "").replace("}", "").replace(",", "").replace("'", "");
    }
}