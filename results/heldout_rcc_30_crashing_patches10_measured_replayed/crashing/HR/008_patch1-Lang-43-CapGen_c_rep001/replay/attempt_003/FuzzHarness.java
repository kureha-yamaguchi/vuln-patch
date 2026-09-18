package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // Exact trigger from the upstream failing test. With registry != null,
        // ExtendedMessageFormat.applyPattern() uses appendQuotedString(..., true).
        // On the buggy version, a pattern position at QUOTE returns without next(pos),
        // so the parser stays on the same quote and grows buffers until OOME.
        new ExtendedMessageFormat(ANCHOR_PATTERN, registry);

        String a = atom(data.consumeAsciiString(16));
        String b = atom(data.consumeAsciiString(16));
        String c = atom(data.consumeAsciiString(16));

        String[] patterns = new String[] {
            a + "''" + b,
            a + "''" + b + " {0}",
            a + " {0} '' " + b,
            a + " 'x' " + b,
            a + " '" + c + "' " + b,
            "''",
            "'x'",
            a + "''s a {0} 'test'!"
        };

        String pattern = patterns[data.consumeInt(0, patterns.length - 1)];
        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);

        String before1 = emf1.toPattern();
        String out1 = emf1.format(new Object[] { "DuMmY" });
        String after1 = emf1.toPattern();

        String before2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { "DuMmY" });
        String after2 = emf2.toPattern();

        // Contract: formatting must not mutate the formatter's pattern representation,
        // and two independently constructed formatters from the same pattern must behave identically.
        if (!eq(before1, after1)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern
                    + " before=" + before1 + " after=" + after1);
        }
        if (!eq(before2, after2)) {
            throw new RuntimeException("[oracle:topattern-stable2] metamorphic violation: pattern=" + pattern
                    + " before=" + before2 + " after=" + after2);
        }
        if (!eq(before1, before2) || !eq(after1, after2)) {
            throw new RuntimeException("[oracle:constructor-determinism] metamorphic violation: pattern=" + pattern
                    + " before1=" + before1 + " before2=" + before2
                    + " after1=" + after1 + " after2=" + after2);
        }
        if (!eq(out1, out2)) {
            throw new RuntimeException("[oracle:format-determinism] metamorphic violation: pattern=" + pattern
                    + " out1=" + out1 + " out2=" + out2);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < 32 || ch > 126 || ch == '\'' || ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            return "x";
        }
        return sb.toString();
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}