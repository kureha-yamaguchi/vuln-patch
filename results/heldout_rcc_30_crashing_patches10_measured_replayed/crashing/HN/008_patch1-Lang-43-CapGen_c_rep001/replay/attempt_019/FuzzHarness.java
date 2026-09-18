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
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, REGISTRY);

        /*
         * Contract asserted: this exact valid regression pattern must parse and format as in the
         * original test. If a patch merely suppresses the buggy quote-handling path, it can avoid
         * the crash yet still produce the wrong parsed pattern or wrong formatted output.
         */
        String anchorFormatted = anchor.format(new Object[] { "DUMMY" });
        if (!anchorPattern.equals(anchor.toPattern())) {
            throw new RuntimeException(
                "[oracle:anchor-topattern] metamorphic violation: valid pattern must round-trip through toPattern input="
                    + anchorPattern + " lhs=" + anchor.toPattern() + " rhs=" + anchorPattern);
        }
        if (!"it's a dummy test!".equals(anchorFormatted)) {
            throw new RuntimeException(
                "[oracle:anchor-format] metamorphic violation: regression pattern must format as expected input="
                    + anchorPattern + " lhs=" + anchorFormatted + " rhs=it's a dummy test!");
        }

        String pre = clean(data.consumeAsciiString(10));
        String mid = clean(data.consumeAsciiString(10));
        String post = clean(data.consumeAsciiString(10));
        String quoted = clean(data.consumeAsciiString(10));
        if (quoted.length() == 0) {
            quoted = "test";
        }
        String arg = data.consumeAsciiString(20);
        if (arg.length() == 0) {
            arg = "Dummy";
        }
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        /*
         * Targeted exploration:
         * The diff shows the buggy/fixed difference is whether ParsePosition advances before
         * handling an escaped quote. So we always generate patterns containing a doubled quote
         * outside a format element: ...''...{0,lower|upper}...'literal'...
         * These are valid-by-construction and drive the real constructor/applyPattern path.
         */
        String pattern = pre + "''" + mid + " {0," + fmt + "} '" + quoted + "'" + post;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        String actual = emf.format(new Object[] { arg });
        String expectedArg = "lower".equals(fmt)
            ? arg.toLowerCase(Locale.getDefault())
            : arg.toUpperCase(Locale.getDefault());
        String expected = pre + "'" + mid + " " + expectedArg + " " + quoted + post;

        if (!pattern.equals(emf.toPattern())) {
            throw new RuntimeException(
                "[oracle:topattern] metamorphic violation: valid pattern must round-trip through toPattern input="
                    + pattern + " lhs=" + emf.toPattern() + " rhs=" + pattern);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                "[oracle:format] metamorphic violation: doubled quote and quoted literal semantics must be preserved input="
                    + pattern + " arg=" + arg + " lhs=" + actual + " rhs=" + expected);
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

    private static Map makeRegistry() {
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