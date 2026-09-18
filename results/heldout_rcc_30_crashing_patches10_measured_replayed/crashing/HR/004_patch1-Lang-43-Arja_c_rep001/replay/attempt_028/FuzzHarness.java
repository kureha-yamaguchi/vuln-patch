package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchor = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchor, registry);
        String anchorOut = anchorEmf.format(new Object[] { "dummy" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: input=" + anchor + " output=" + anchorOut);
        }

        String left = sanitize(data.consumeAsciiString(12));
        String right = sanitize(data.consumeAsciiString(12));
        String quoted = sanitize(data.consumeAsciiString(12));
        String arg = sanitize(data.consumeAsciiString(12));
        String suffix = sanitize(data.consumeAsciiString(12));

        if (left.length() == 0) {
            left = "it";
        }
        if (right.length() == 0) {
            right = "s";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }
        if (arg.length() == 0) {
            arg = "dummy";
        }

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = left + "''" + right + " {0}";
                break;
            case 1:
                pattern = left + "''" + right + " {0} '" + quoted + "'";
                break;
            case 2:
                pattern = "'" + quoted + "' " + left + "''" + right + " {0}";
                break;
            default:
                pattern = left + "''" + right + " {0} '" + quoted + "'" + suffix;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { arg });

        String expected;
        switch (patternCase(pattern, left, right, quoted, suffix)) {
            case 0:
                expected = left + "'" + right + " " + arg;
                break;
            case 1:
                expected = left + "'" + right + " " + arg + " " + quoted;
                break;
            case 2:
                expected = quoted + " " + left + "'" + right + " " + arg;
                break;
            default:
                expected = left + "'" + right + " " + arg + " " + quoted + suffix;
                break;
        }

        /* Contract: MessageFormat formatting must honor doubled quotes as a literal
         * apostrophe and single-quoted text as literal text. A patch that merely avoids
         * the crash by skipping quote handling would violate this observable output.
         */
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:format] metamorphic violation: input=" + pattern + " output=" + out + " expected=" + expected);
        }

        String p1 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        if (!p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-idempotence] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
        }
    }

    private static int patternCase(String pattern, String left, String right, String quoted, String suffix) {
        String c0 = left + "''" + right + " {0}";
        if (pattern.equals(c0)) {
            return 0;
        }
        String c1 = left + "''" + right + " {0} '" + quoted + "'";
        if (pattern.equals(c1)) {
            return 1;
        }
        String c2 = "'" + quoted + "' " + left + "''" + right + " {0}";
        if (pattern.equals(c2)) {
            return 2;
        }
        return 3;
    }

    private static String sanitize(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}