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
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorBefore = anchor.toPattern();
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();
        if (!anchorPattern.equals(anchorBefore) || !anchorPattern.equals(anchorAfter)
                || !"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException(
                "[oracle:anchor-output] metamorphic violation: known valid seed must preserve pattern and expected output input="
                    + anchorPattern + " before=" + anchorBefore + " after=" + anchorAfter + " out=" + anchorOut);
        }

        String pattern = buildPattern(data);
        String arg = sanitizeArg(data.consumeString(24));

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
        String p1 = emf1.toPattern();
        String out1 = emf1.format(new Object[] { arg });

        /*
         * Contract/oracle: toPattern() exposes the parsed pattern. Reconstructing an
         * ExtendedMessageFormat from its own toPattern() with the same registry must
         * preserve the represented formatting behavior for the same argument. A patch
         * that merely skips/deletes quoted-string bookkeeping could avoid the crash but
         * silently misparse quotes; this round-trip would then disagree.
         */
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { arg });

        if (!eq(p1, p2) || !eq(out1, out2)) {
            throw new RuntimeException(
                "[oracle:pattern-roundtrip] metamorphic violation: reconstructing from toPattern must preserve behavior input="
                    + pattern + " p1=" + p1 + " p2=" + p2 + " out1=" + out1 + " out2=" + out2);
        }
    }

    private static String buildPattern(FuzzedDataProvider data) {
        String a = literal(data.consumeAsciiString(10));
        String b = literal(data.consumeAsciiString(10));
        String c = literal(data.consumeAsciiString(10));
        String d = literal(data.consumeAsciiString(10));

        if (a.length() == 0) {
            a = "it";
        }
        if (b.length() == 0) {
            b = "s";
        }
        if (c.length() == 0) {
            c = "test";
        }
        if (d.length() == 0) {
            d = "end";
        }

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        switch (data.consumeInt(0, 5)) {
            case 0:
                return a + "''" + b + " a {0," + fmt + "} '" + c + "'!";
            case 1:
                return a + "''" + b + " {0," + fmt + "} '" + c + "'";
            case 2:
                return "'" + a + "' " + b + "'' " + c + " {0," + fmt + "}!";
            case 3:
                return a + " {0," + fmt + "} '" + b + "' " + c + "''" + d;
            case 4:
                return a + "''s a {0," + fmt + "} '" + b + "'!" + d;
            default:
                return "'" + a + "' " + b + " {0," + fmt + "} '" + c + "' " + d;
        }
    }

    private static String literal(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else if (Character.isISOControl(ch)) {
                sb.append(' ');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String sanitizeArg(String s) {
        if (s == null || s.length() == 0) {
            return "Dummy";
        }
        return s;
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
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
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
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
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}