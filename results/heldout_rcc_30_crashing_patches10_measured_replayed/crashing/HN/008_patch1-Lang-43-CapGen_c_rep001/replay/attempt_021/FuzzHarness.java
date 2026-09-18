package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(this.locale));
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
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(this.locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String sanitizeLiteral(String s) {
        return s.replace("{", "(").replace("}", ")").replace("'", "");
    }

    private static String sanitizeQuotedBody(String s) {
        return s.replace("'", "''").replace("{", "(").replace("}", ")");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: known regression test output changed input="
                    + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        String prefix = sanitizeLiteral(data.consumeAsciiString(16));
        String infix = sanitizeLiteral(data.consumeAsciiString(16));
        String quoted = sanitizeQuotedBody(data.consumeAsciiString(16));
        String suffix = sanitizeLiteral(data.consumeAsciiString(16));
        String arg = data.consumeString(24);

        int variant = data.consumeInt(0, 3);
        String pattern;
        if (variant == 0) {
            pattern = prefix + "''" + infix + " {0,lower} '" + quoted + "'" + suffix;
        } else if (variant == 1) {
            pattern = "'" + quoted + "' " + prefix + "''" + infix + " {0,lower}" + suffix;
        } else if (variant == 2) {
            pattern = prefix + " {0,lower} " + infix + "''" + suffix + " '" + quoted + "'";
        } else {
            pattern = prefix + "''" + infix + " '{"
                    + quoted.replace("{", "").replace("}", "") + "}' {0,lower} " + suffix;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });

        /*
         * Contract/oracle: with exactly one custom {0,lower} element, formatting must be equivalent
         * to formatting the same pattern with plain MessageFormat after replacing {0,lower} by {0}
         * and lowercasing the argument up front. A patch that merely avoids the buggy quote-parsing
         * loop but loses/skips quote handling would change the observable formatted result.
         */
        String referencePattern = pattern.replace("{0,lower}", "{0}");
        String expected = new MessageFormat(referencePattern).format(new Object[] {
                String.valueOf(arg).toLowerCase()
        });
        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:lower-eq] metamorphic violation: ExtendedMessageFormat with lower must match MessageFormat on pre-lowercased argument input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }
}