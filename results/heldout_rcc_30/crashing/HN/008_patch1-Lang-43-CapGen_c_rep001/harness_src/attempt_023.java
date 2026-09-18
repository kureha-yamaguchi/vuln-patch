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
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorFormatted = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorFormatted)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression-test pattern must format as expected input="
                    + anchorPattern + " lhs=" + anchorFormatted + " rhs=it's a dummy test!");
        }

        String a = sanitizeLiteral(data.consumeAsciiString(12));
        String b = sanitizeLiteral(data.consumeAsciiString(12));
        String c = sanitizeLiteral(data.consumeAsciiString(12));
        String quoted = sanitizeQuoted(data.consumeAsciiString(12));
        String arg = data.consumeString(20);

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = a + "''" + b + " {0,lower} '" + quoted + "'!" + c;
                break;
            case 1:
                pattern = a + "''" + b + " {0,lower} '" + quoted + "'";
                break;
            case 2:
                pattern = a + "''" + b + " x {0,lower} '" + quoted + "' y " + c;
                break;
            case 3:
                pattern = a + "''s a {0,lower} '" + quoted + "'!" + c;
                break;
            default:
                pattern = a + "''" + b + " {0,upper} '" + quoted + "'!" + c;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        /*
         * Contract/oracle: formatting is read-only with respect to the parsed pattern state;
         * toPattern is a public reader and must not change across repeated format calls.
         * A patch that only suppresses the buggy quote-handling path by skipping/losing parser
         * bookkeeping can leave observable pattern state inconsistent.
         */
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }

        String referencePattern = pattern.replace("{0,lower}", "{0}").replace("{0,upper}", "{0}");
        MessageFormat mf = new MessageFormat(referencePattern);
        String transformedArg;
        if (pattern.indexOf("{0,upper}") >= 0) {
            transformedArg = String.valueOf(arg).toUpperCase();
        } else {
            transformedArg = String.valueOf(arg).toLowerCase();
        }
        String expected = mf.format(new Object[] { transformedArg });
        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:format-equivalence] metamorphic violation: custom lower/upper format should match MessageFormat on pre-transformed argument input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String sanitizeLiteral(String s) {
        return s.replace("'", "").replace("{", "").replace("}", "");
    }

    private static String sanitizeQuoted(String s) {
        return s.replace("'", "''").replace("{", "").replace("}", "");
    }

    public static class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    public static class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    public static class LowerCaseFormat extends Format {
        private final Locale locale;

        public LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            toAppendTo.append(String.valueOf(obj).toLowerCase(this.locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    public static class UpperCaseFormat extends Format {
        private final Locale locale;

        public UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            toAppendTo.append(String.valueOf(obj).toUpperCase(this.locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}