package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchor, registry);

        String anchorPattern = anchorEmf.toPattern();
        ExtendedMessageFormat anchorRoundTrip = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorPattern2 = anchorRoundTrip.toPattern();
        if (!anchorPattern.equals(anchorPattern2)) {
            throw new RuntimeException("[oracle:anchor-toPattern-idempotence] metamorphic violation: input=" + anchor
                    + " lhs=" + anchorPattern + " rhs=" + anchorPattern2);
        }

        String left = safeLiteral(data.consumeAsciiString(16));
        String right = safeLiteral(data.consumeAsciiString(16));
        String tail = safeLiteral(data.consumeAsciiString(16));

        if (left.length() == 0) {
            left = "it";
        }
        if (right.length() == 0) {
            right = "s";
        }

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = left + "''" + right + " {0}";
                break;
            case 1:
                pattern = left + "''" + right + " {0,lower}";
                break;
            case 2:
                pattern = left + "''" + right + " {0} '" + tail + "'";
                break;
            default:
                pattern = left + "''" + right + " {0,lower} '" + tail + "'!";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        /* Contract: toPattern() returns the pattern representation of the format.
         * Reconstructing a new ExtendedMessageFormat from that representation and
         * asking for toPattern() again must be stable for any correct implementation.
         * A patch that merely skips quote handling to avoid the crash can violate this.
         */
        String p1 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        if (!p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-idempotence] metamorphic violation: input=" + pattern
                    + " lhs=" + p1 + " rhs=" + p2);
        }
    }

    private static String safeLiteral(String s) {
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