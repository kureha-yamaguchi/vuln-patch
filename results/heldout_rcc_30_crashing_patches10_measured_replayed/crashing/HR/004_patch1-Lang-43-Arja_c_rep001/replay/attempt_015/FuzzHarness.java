package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchor = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchorFmt = new ExtendedMessageFormat(anchor, registry);
        String anchorOut = anchorFmt.format(new Object[] { "dummy" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: pattern=" + anchor + " output=" + anchorOut);
        }

        String left = sanitize(data.consumeAsciiString(16));
        String middle = sanitize(data.consumeAsciiString(16));
        String right = sanitize(data.consumeAsciiString(16));
        int shape = data.consumeInt(0, 3);

        String pattern;
        switch (shape) {
            case 0:
                pattern = left + "''" + middle + " {0} '" + right + "'";
                break;
            case 1:
                pattern = "'" + left + "' " + middle + "''" + right + " {0}";
                break;
            case 2:
                pattern = left + " {0} '' " + middle + " '" + right + "'";
                break;
            default:
                pattern = left + "''" + middle + right;
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
        String out1 = emf1.format(new Object[] { "dummy" });
        String canon1 = emf1.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(canon1, registry);
        String out2 = emf2.format(new Object[] { "dummy" });
        String canon2 = emf2.toPattern();

        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: pattern=" + pattern + " canon=" + canon1 + " lhs=" + out1 + " rhs=" + out2);
        }
        if (!canon1.equals(canon2)) {
            throw new RuntimeException("[oracle:topattern-idempotent] metamorphic violation: pattern=" + pattern + " first=" + canon1 + " second=" + canon2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        return s.replace('\'', 'x').replace('{', 'x').replace('}', 'y');
    }
}