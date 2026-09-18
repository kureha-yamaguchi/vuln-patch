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
        Map registry = makeRegistry();

        // ANCHOR: exact trigger from the failing test. On the buggy version this must
        // escape as the observed OutOfMemoryError from appendQuotedString.
        String anchor = "it''s a {0,lower} 'test'!";
        runAndCheck(anchor, registry, "DUMMY", "it's a dummy test!");

        // EXPLORE: same root-cause property as the patch: a quote-handling path where
        // appendQuotedString is entered with pos at a quote and escaping enabled, so the
        // buggy code fails to advance past the opening quote before scanning.
        String fuzzArg = nonEmptyLiteral(data.consumeString(24));
        String pattern = buildQuoteHeavyPattern(data);
        runAndCheck(pattern, registry, fuzzArg, null);
    }

    private static void runAndCheck(String pattern, Map registry, String arg, String expectedOrNull) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        /*
         * Observable contract used as oracle:
         * - toPattern() exposes the parsed pattern.
         * - Reconstructing an ExtendedMessageFormat from its own toPattern() with the same
         *   registry must preserve formatting behavior for the same argument.
         * A patch that merely skips/deletes the quoted-string bookkeeping could avoid the
         * crash but silently mis-parse quotes; this round-trip would then disagree.
         */
        String p1 = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { arg });

        if (!eq(p1, p2) || !eq(out1, out2)) {
            throw new RuntimeException(
                "[oracle:pattern-roundtrip] metamorphic violation: reconstructing from toPattern must preserve behavior input="
                    + pattern + " p1=" + p1 + " p2=" + p2 + " out1=" + out1 + " out2=" + out2);
        }

        if (expectedOrNull != null && !expectedOrNull.equals(out1)) {
            throw new RuntimeException(
                "[oracle:anchor-output] metamorphic violation: known regression input formatted unexpectedly input="
                    + pattern + " expected=" + expectedOrNull + " actual=" + out1);
        }
    }

    private static String buildQuoteHeavyPattern(FuzzedDataProvider data) {
        String a = nonEmptyLiteral(data.consumeAsciiString(8));
        String b = nonEmptyLiteral(data.consumeAsciiString(8));
        String c = nonEmptyLiteral(data.consumeAsciiString(8));
        String d = nonEmptyLiteral(data.consumeAsciiString(8));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        switch (data.consumeInt(0, 5)) {
            case 0:
                return a + "''" + b + " {0," + fmt + "} '" + c + "'";
            case 1:
                return "'" + a + "' " + b + "''" + c + " {0," + fmt + "}!";
            case 2:
                return a + " {0," + fmt + "} '" + b + "' " + c + "''" + d;
            case 3:
                return a + "''s " + b + " {0," + fmt + "} '" + c + "'!";
            case 4:
                return "'" + a + "' " + b + " {0," + fmt + "} '" + c + "' " + d;
            default:
                return a + "''" + b + " {0," + fmt + "} '" + c + "' " + d + "''";
        }
    }

    private static String nonEmptyLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
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
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
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