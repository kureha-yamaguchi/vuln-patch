package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Map REGISTRY = new HashMap();

    static {
        REGISTRY.put("lower", new LowerCaseFormatFactory());
        REGISTRY.put("upper", new UpperCaseFormatFactory());
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression test input. On the buggy version this reaches
        // ExtendedMessageFormat.appendQuotedString and triggers the verified OOME.
        exercise("it''s a {0,lower} 'test'!");

        // EXPLORE: same root-cause property as the patch:
        // when escapingOn is true and the current character is a quote from an
        // escaped quote sequence ("''"), appendQuotedString must advance ParsePosition.
        // Build only valid patterns so fixed code accepts them.
        String prefix = atom(data.consumeAsciiString(16));
        String mid = atom(data.consumeAsciiString(16));
        String suffix = atom(data.consumeAsciiString(16));

        int repeats = data.consumeInt(1, 4);
        StringBuilder pattern = new StringBuilder();
        pattern.append(prefix);

        for (int i = 0; i < repeats; i++) {
            pattern.append("''");
            pattern.append((i & 1) == 0 ? mid : suffix);
            if (data.consumeBoolean()) {
                pattern.append(' ');
            }
        }

        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append('\'').append(atom(data.consumeAsciiString(12))).append('\'');
        }

        if (data.consumeBoolean()) {
            pattern.append(' ');
        }
        pattern.append("{0,lower}");

        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append(atom(data.consumeAsciiString(12)));
        }

        exercise(pattern.toString());
    }

    private static void exercise(String pattern) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        // Oracle: constructing from a valid pattern and then reading it back via
        // toPattern() must preserve the pattern, and re-parsing that returned
        // pattern must also preserve it. A "fix" that merely skips quote handling
        // or drops bookkeeping for custom formats would violate this observable API.
        String roundTrip = emf.toPattern();
        if (!pattern.equals(roundTrip)) {
            throw new RuntimeException(
                    "[oracle:pattern-roundtrip] metamorphic violation: toPattern must preserve valid input pattern input="
                            + pattern + " lhs=" + pattern + " rhs=" + roundTrip);
        }

        String roundTrip2 = new ExtendedMessageFormat(roundTrip, REGISTRY).toPattern();
        if (!roundTrip.equals(roundTrip2)) {
            throw new RuntimeException(
                    "[oracle:pattern-idempotent] metamorphic violation: reparsing toPattern must be stable input="
                            + pattern + " lhs=" + roundTrip + " rhs=" + roundTrip2);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '{' && ch != '}' && ch != '\'' && ch != ',') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            return "x";
        }
        return out.toString();
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return PassthroughFormat.INSTANCE;
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return PassthroughFormat.INSTANCE;
        }
    }

    private static final class PassthroughFormat extends Format {
        private static final PassthroughFormat INSTANCE = new PassthroughFormat();

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(obj == null ? "null" : String.valueOf(obj));
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}