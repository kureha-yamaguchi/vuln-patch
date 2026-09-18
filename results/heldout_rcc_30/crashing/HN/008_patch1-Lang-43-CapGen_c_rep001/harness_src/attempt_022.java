package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
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
            pos.setIndex(source.length());
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
            pos.setIndex(source.length());
            return source;
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String noBracesNoQuotes(String s) {
        return s.replace("'", "").replace("{", "").replace("}", "");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorResult = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorResult)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: known regression output changed input="
                    + anchorPattern + " lhs=" + anchorResult + " rhs=it's a dummy test!");
        }

        String prefix = noBracesNoQuotes(data.consumeAsciiString(12));
        String middle = noBracesNoQuotes(data.consumeAsciiString(12));
        String quoted = noBracesNoQuotes(data.consumeAsciiString(12));
        String suffix = noBracesNoQuotes(data.consumeAsciiString(12));
        String arg = data.consumeString(16);

        int which = data.consumeInt(0, 3);
        String pattern;
        if (which == 0) {
            pattern = prefix + "''" + middle + " {0,lower} '" + quoted + "'!" + suffix;
        } else if (which == 1) {
            pattern = prefix + "''s " + middle + "{0,lower} '" + quoted + "'" + suffix;
        } else if (which == 2) {
            pattern = prefix + "''" + middle + " and {0,upper} '" + quoted + "' " + suffix;
        } else {
            pattern = prefix + "''" + middle + " {0,lower} '" + quoted + "' " + suffix;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String formatted1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }

        String formatted2 = emf.format(new Object[] { arg });
        if (!formatted1.equals(formatted2)) {
            throw new RuntimeException("[oracle:format-idempotent] metamorphic violation: repeated formatting with same argument must be stable input="
                    + pattern + " lhs=" + formatted1 + " rhs=" + formatted2);
        }
    }
}