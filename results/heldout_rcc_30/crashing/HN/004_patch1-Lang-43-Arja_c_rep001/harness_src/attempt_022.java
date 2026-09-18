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
        // ANCHOR: exact failing test input. On the buggy version this must reach
        // ExtendedMessageFormat.appendQuotedString and trigger the verified OOM.
        checkPattern("it''s a {0,lower} 'test'!", "DUMMY", "it's a dummy test!");

        // EXPLORE: same root-cause property as the patch:
        // when escapingOn is true and the current position starts with a QUOTE,
        // the buggy code returns without advancing ParsePosition. Patterns with
        // doubled quotes ("''") in literal text drive that branch.
        String a = clean(data.consumeAsciiString(12));
        String b = clean(data.consumeAsciiString(12));
        String c = clean(data.consumeAsciiString(12));
        String d = clean(data.consumeAsciiString(12));
        String arg = cleanNonEmpty(data.consumeAsciiString(12));
        boolean lower = data.consumeBoolean();

        String formatName = lower ? "lower" : "upper";
        String transformed = lower ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        switch (data.consumeInt(0, 3)) {
            case 0:
                checkPattern(a + "''" + b + " {0," + formatName + "} '" + c + "'",
                        arg,
                        a + "'" + b + " " + transformed + " " + c);
                break;
            case 1:
                checkPattern("'" + a + "' " + b + "''" + c + " {0," + formatName + "}",
                        arg,
                        a + " " + b + "'" + c + " " + transformed);
                break;
            case 2:
                checkPattern(a + " {0," + formatName + "} " + b + "''" + c + " '" + d + "'",
                        arg,
                        a + " " + transformed + " " + b + "'" + c + " " + d);
                break;
            default:
                checkPattern(a + "''" + b + " {0," + formatName + "} '" + c + "' " + d,
                        arg,
                        a + "'" + b + " " + transformed + " " + c + " " + d);
                break;
        }
    }

    private static void checkPattern(String pattern, String arg, String expected) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        // Oracle 1: for valid-by-construction patterns, toPattern must preserve the
        // pattern text; a "fix" that skips the escaped-quote branch or drops state
        // can silently alter the parsed pattern.
        String reparsed = emf.toPattern();
        if (!pattern.equals(reparsed)) {
            throw new RuntimeException("[oracle:pattern-roundtrip] metamorphic violation: constructor/toPattern changed valid pattern input="
                    + pattern + " lhs=" + pattern + " rhs=" + reparsed);
        }

        // Oracle 2: the valid constructed pattern's formatted output is known from
        // the input itself: literals are preserved, '' becomes a single quote, the
        // custom factory transforms argument case, and quoted text loses delimiters.
        String out = emf.format(new Object[] { arg });
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:format-output] metamorphic violation: valid pattern formatted unexpectedly input="
                    + pattern + " lhs=" + out + " rhs=" + expected);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == ' ' || ch == '_' || ch == '-') {
                sb.append(ch);
            }
        }
        return sb.toString().trim();
    }

    private static String cleanNonEmpty(String s) {
        String out = clean(s);
        return out.length() == 0 ? "Dummy" : out;
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new SimpleCaseFormatFactory(true));
        registry.put("upper", new SimpleCaseFormatFactory(false));
        return registry;
    }

    private static final class SimpleCaseFormatFactory implements FormatFactory {
        private final boolean lower;

        private SimpleCaseFormatFactory(boolean lower) {
            this.lower = lower;
        }

        public Format getFormat(String name, String arguments, Locale locale) {
            return new SimpleCaseFormat(lower);
        }
    }

    private static final class SimpleCaseFormat extends Format {
        private static final long serialVersionUID = 1L;
        private final boolean lower;

        private SimpleCaseFormat(boolean lower) {
            this.lower = lower;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(Locale.ROOT) : s.toUpperCase(Locale.ROOT));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}