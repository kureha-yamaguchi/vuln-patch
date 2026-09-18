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
    private static final Map REGISTRY = new HashMap();

    static {
        REGISTRY.put("lower", new LowerCaseFormatFactory());
        REGISTRY.put("upper", new UpperCaseFormatFactory());
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, REGISTRY);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted incorrectly input=" + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }
        String anchorBefore = anchor.toPattern();
        String anchorAfter = anchor.toPattern();
        if (!anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern changed across read-only observation input=" + anchorPattern + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String prefix = sanitize(data.consumeAsciiString(12), 'A');
        String middle = sanitize(data.consumeAsciiString(12), 'B');
        String quoted = sanitize(data.consumeAsciiString(12), 'C');
        String suffix = sanitize(data.consumeAsciiString(12), 'D');
        String arg = sanitize(data.consumeAsciiString(12), 'X');

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = prefix + "''" + middle + " {0,lower} '" + quoted + "'" + suffix;
                break;
            case 1:
                pattern = "'" + quoted + "' " + prefix + "''" + middle + " {0,lower} " + suffix;
                break;
            case 2:
                pattern = prefix + " {0,lower} " + middle + "''" + suffix + " '" + quoted + "'";
                break;
            default:
                pattern = prefix + "''" + middle + " '{0}' " + suffix + " {0,lower}";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        String referencePattern = pattern.replace("{0,lower}", "{0}");
        MessageFormat mf = new MessageFormat(referencePattern);
        String expected = mf.format(new Object[] { arg.toLowerCase(Locale.ROOT) });

        /* Contract/oracle:
           For a registered "lower" format, formatting "{0,lower}" is equivalent to formatting "{0}"
           with the argument lowercased beforehand. This uses real library formatting on both sides.
           Also, format() is read-only with respect to the stored pattern, so toPattern() must stay stable.
           A patch that merely skips/changes the quote-handling bookkeeping can avoid the crash but
           silently mis-parse doubled quotes or mutate the reconstructed pattern. */
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern] metamorphic violation: format changed toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
        }
        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:lower-eq] metamorphic violation: custom lower format disagrees with equivalent real formatting input=" + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String sanitize(String s, char fallback) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append(fallback);
        }
        return out.toString();
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return LowerCaseFormat.INSTANCE;
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return UpperCaseFormat.INSTANCE;
        }
    }

    private static final class LowerCaseFormat extends Format {
        private static final LowerCaseFormat INSTANCE = new LowerCaseFormat();

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            return toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private static final UpperCaseFormat INSTANCE = new UpperCaseFormat();

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            return toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}