package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String left = sanitize(data.consumeAsciiString(32));
        String right = sanitize(data.consumeAsciiString(32));
        String quoted = sanitize(data.consumeAsciiString(32));

        if (left.length() == 0) {
            left = "it";
        }
        if (right.length() == 0) {
            right = "works";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }

        String pattern = left + "''" + right + " {0} '" + quoted + "'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String p1 = emf.toPattern();
        String formatted1 = emf.format(new Object[] { "dummy" });

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        String formatted2 = emf2.format(new Object[] { "dummy" });

        if (!p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
        }
        if (!formatted1.equals(formatted2)) {
            throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + formatted1 + " rhs=" + formatted2);
        }
    }

    private static String sanitize(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}