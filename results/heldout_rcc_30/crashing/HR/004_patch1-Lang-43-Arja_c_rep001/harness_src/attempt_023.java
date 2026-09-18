package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static final Map NON_NULL_REGISTRY = new HashMap();
    private static final String ANCHOR_PATTERN = "it''s a {0} 'test'!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        trigger(ANCHOR_PATTERN);

        String left = sanitize(data.consumeAsciiString(24));
        String right = sanitize(data.consumeAsciiString(24));
        String quoted = sanitize(data.consumeAsciiString(24));
        boolean putQuoteNearStart = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (putQuoteNearStart) {
            pattern.append(left);
            pattern.append("''");
            pattern.append(right);
        } else {
            pattern.append(left);
            pattern.append(' ');
            pattern.append(right);
            pattern.append("''");
        }
        pattern.append(" {0} ");
        pattern.append('\'').append(quoted).append('\'');

        trigger(pattern.toString());
    }

    private static void trigger(String pattern) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, NON_NULL_REGISTRY);

        String formatted = emf.format(new Object[] { "DUMMY" });
        String reparsedPattern = emf.toPattern();
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(reparsedPattern, NON_NULL_REGISTRY);
        String formattedAgain = reparsed.format(new Object[] { "DUMMY" });

        if (!formatted.equals(formattedAgain)) {
            throw new RuntimeException(
                    "[oracle:roundtrip] metamorphic violation: pattern=" + pattern
                            + " lhs=" + formatted + " rhs=" + formattedAgain);
        }
    }

    private static String sanitize(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length() && out.length() < 24; i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ') {
                out.append(c);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }
}