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
        registry.put("lower", new SimpleFormatFactory(false));
        registry.put("upper", new SimpleFormatFactory(true));

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });

        /*
         * Contract asserted: for a valid pattern, toPattern() should preserve the pattern and
         * formatting should honor doubled quotes as a literal single quote plus the registered
         * custom format. A throw-deleting patch in quote handling could avoid the crash but
         * silently change parsing/formatting; these checks catch that.
         */
        if (!anchorPattern.equals(anchor.toPattern())) {
            throw new RuntimeException(
                "[oracle:anchor-topattern] metamorphic violation: valid pattern must round-trip through toPattern input="
                    + anchorPattern + " lhs=" + anchor.toPattern() + " rhs=" + anchorPattern);
        }
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException(
                "[oracle:anchor-format] metamorphic violation: anchor regression pattern must format as expected input="
                    + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        String pre = stripSpecial(data.consumeAsciiString(8));
        String mid = stripSpecial(data.consumeAsciiString(8));
        String quoted = stripSpecial(data.consumeAsciiString(8));
        String post = stripSpecial(data.consumeAsciiString(8));
        String arg = data.consumeAsciiString(16);
        if (arg.length() == 0) {
            arg = "X";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern = pre + "''" + mid + " {0," + fmt + "} '" + quoted + "'" + post;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });

        String transformed = "lower".equals(fmt)
            ? arg.toLowerCase(Locale.getDefault())
            : arg.toUpperCase(Locale.getDefault());
        String expected = pre + "'" + mid + " " + transformed + " " + quoted + post;

        if (!pattern.equals(emf.toPattern())) {
            throw new RuntimeException(
                "[oracle:topattern] metamorphic violation: valid pattern must round-trip through toPattern input="
                    + pattern + " lhs=" + emf.toPattern() + " rhs=" + pattern);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                "[oracle:format] metamorphic violation: constructed valid pattern must preserve quote semantics and registered format input="
                    + pattern + " arg=" + arg + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String stripSpecial(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final class SimpleFormatFactory implements FormatFactory {
        private final boolean upper;

        SimpleFormatFactory(boolean upper) {
            this.upper = upper;
        }

        public Format getFormat(String name, String args, Locale locale) {
            return new Format() {
                public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                    String s = String.valueOf(obj);
                    Locale useLocale = locale == null ? Locale.getDefault() : locale;
                    toAppendTo.append(upper ? s.toUpperCase(useLocale) : s.toLowerCase(useLocale));
                    return toAppendTo;
                }

                public Object parseObject(String source, ParsePosition pos) {
                    int start = pos.getIndex();
                    pos.setIndex(source.length());
                    return source.substring(start);
                }
            };
        }
    }
}