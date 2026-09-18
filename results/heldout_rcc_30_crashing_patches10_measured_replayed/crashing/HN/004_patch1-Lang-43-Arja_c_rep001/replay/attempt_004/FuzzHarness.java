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
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, REGISTRY);

        String anchorBefore = anchor.toPattern();
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();

        /* Contract asserted:
           the exact regression input from the real test must format to "it's a dummy test!",
           and format() must not mutate the stored pattern. A patch that merely avoids the
           crashing branch by skipping quote bookkeeping can return a wrong string or corrupt
           toPattern() while no exception fires. */
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted incorrectly input=" + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }
        if (!anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: format changed toPattern input=" + anchorPattern + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String p1 = clean(data.consumeAsciiString(12), 'A');
        String p2 = clean(data.consumeAsciiString(12), 'B');
        String p3 = clean(data.consumeAsciiString(12), 'C');
        String arg = clean(data.consumeAsciiString(12), 'X');

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = p1 + "''" + p2 + " {0,lower} '" + p3 + "'!";
                break;
            case 1:
                pattern = "'" + p1 + "' " + p2 + "''" + p3 + " {0,lower}!";
                break;
            case 2:
                pattern = p1 + " {0,lower} " + p2 + "''" + p3 + "!";
                break;
            default:
                pattern = p1 + "''" + p2 + " {0,upper} '" + p3 + "'!";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String before = emf.toPattern();
        String out = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern] metamorphic violation: format changed toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, REGISTRY);
        String out2 = emf2.format(new Object[] { arg });

        /* Contract asserted:
           toPattern() returns a pattern representation equivalent to the current formatter.
           Reconstructing a new ExtendedMessageFormat from toPattern() must preserve format output
           for the same arguments. A throw-deleting patch in quote parsing can silently emit a
           non-equivalent pattern. */
        if (!out.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: rebuilding from toPattern changed formatting input=" + pattern + " lhs=" + out + " rhs=" + out2);
        }
    }

    private static String clean(String s, char fallback) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                b.append(ch);
            }
        }
        if (b.length() == 0) {
            b.append(fallback);
        }
        return b.toString();
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
        private static final UpperCaseFormat INSTANCE = new UpperCaseFormat();

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