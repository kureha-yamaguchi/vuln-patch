package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Map REGISTRY = createRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        String prefix = clean(data.consumeAsciiString(12));
        String infix = clean(data.consumeAsciiString(12));
        String quoted = cleanNonEmpty(data.consumeAsciiString(12));
        String suffix = clean(data.consumeAsciiString(12));
        String arg = data.consumeAsciiString(20);
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String formatName = data.consumeBoolean() ? "lower" : "upper";

        String pattern = prefix + "''" + infix + "{0," + formatName + "}" + " '" + quoted + "'" + suffix;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        String expected = prefix
                + "'"
                + infix
                + ("lower".equals(formatName) ? arg.toLowerCase(Locale.getDefault()) : arg.toUpperCase(Locale.getDefault()))
                + " "
                + quoted
                + suffix;

        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        /*
         * Contract asserted:
         * For these patterns, the quotes are balanced and the custom format name is registered,
         * so construction and formatting are valid by construction. A correct implementation must
         * preserve the parsed pattern in toPattern() and must render doubled quote '' as a single
         * literal quote while preserving the quoted literal section.
         */
        if (!pattern.equals(before) || !pattern.equals(after)) {
            throw new RuntimeException(
                    "[oracle:topattern] metamorphic violation: valid pattern must round-trip through toPattern input="
                            + pattern + " lhsBefore=" + before + " lhsAfter=" + after + " rhs=" + pattern);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                    "[oracle:format] metamorphic violation: valid constructed pattern must format to the known literal/argument composition input="
                            + pattern + " arg=" + arg + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static void anchor() {
        String pattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String actual = emf.format(new Object[] { "DUMMY" });
        String expected = "it's a dummy test!";
        if (!pattern.equals(emf.toPattern())) {
            throw new RuntimeException(
                    "[oracle:anchor-topattern] metamorphic violation: anchor pattern must be preserved input="
                            + pattern + " lhs=" + emf.toPattern() + " rhs=" + pattern);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                    "[oracle:anchor-format] metamorphic violation: anchor formatting must match the regression test input="
                            + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String cleanNonEmpty(String s) {
        String out = clean(s);
        return out.length() == 0 ? "test" : out;
    }

    private static Map createRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false, locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true, locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean upper;
        private final Locale locale;

        private CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(upper ? s.toUpperCase(locale) : s.toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}