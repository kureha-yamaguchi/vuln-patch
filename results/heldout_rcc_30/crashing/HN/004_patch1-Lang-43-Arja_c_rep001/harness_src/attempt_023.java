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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test setup and input.
        // On the buggy version, constructing ExtendedMessageFormat with this pattern
        // reaches appendQuotedString with escapingOn=true at a doubled quote and
        // loops without advancing ParsePosition, eventually throwing OOME.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", REGISTRY);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: valid anchor pattern formatted unexpectedly lhs="
                    + anchorOut + " rhs=it's a dummy test!");
        }
        String anchorPattern = anchor.toPattern();
        if (!"it''s a {0,lower} 'test'!".equals(anchorPattern)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern changed valid anchor pattern lhs="
                    + anchorPattern + " rhs=it''s a {0,lower} 'test'!");
        }

        // EXPLORE: generate more valid patterns with the same triggering property:
        // literal text containing doubled quotes before/around a format element.
        String a = atom(data.consumeAsciiString(8));
        String b = atom(data.consumeAsciiString(8));
        String c = atom(data.consumeAsciiString(8));
        String arg = atomNonEmpty(data.consumeAsciiString(8));
        boolean useLower = data.consumeBoolean();
        String formatName = useLower ? "lower" : "upper";
        String expectedArg = useLower ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        String pattern;
        String expected;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = a + "''" + b + " {0," + formatName + "} '" + c + "'!";
                expected = a + "'" + b + " " + expectedArg + " " + c + "!";
                break;
            case 1:
                pattern = a + " '' " + b + " {0," + formatName + "} '" + c + "'";
                expected = a + " ' " + b + " " + expectedArg + " " + c;
                break;
            case 2:
                pattern = a + "{0," + formatName + "}''" + b + " '" + c + "'";
                expected = a + expectedArg + "'" + b + " " + c;
                break;
            default:
                pattern = a + "''" + b + "{0," + formatName + "}'" + c + "'";
                expected = a + "'" + b + expectedArg + c;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String out = emf.format(new Object[] { arg });

        // Oracle: for these valid-by-construction patterns, formatting must preserve
        // literals, decode '' to a single quote, apply the registered format, and
        // include quoted text without the quote delimiters. A "fix" that merely
        // skips the branch or loses parser state would break this observable result.
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:format-output] metamorphic violation: valid pattern formatted unexpectedly input="
                    + pattern + " lhs=" + out + " rhs=" + expected);
        }

        String roundTrip = emf.toPattern();
        if (!pattern.equals(roundTrip)) {
            throw new RuntimeException("[oracle:pattern-roundtrip] metamorphic violation: constructor/toPattern changed valid pattern input="
                    + pattern + " lhs=" + roundTrip + " rhs=" + pattern);
        }
    }

    private static String atom(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            return "x";
        }
        return sb.toString();
    }

    private static String atomNonEmpty(String s) {
        String out = atom(s);
        return out.length() == 0 ? "Dummy" : out;
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
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

    private static final class LowerCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}