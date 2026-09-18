package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a DUMMY test!".equals(anchorOut)) {
            throw new RuntimeException(
                    "[oracle:anchor-output] metamorphic violation: exact escaped-quote pattern must format correctly input="
                            + anchorPattern + " lhs=" + anchorOut + " rhs=it's a DUMMY test!");
        }

        String pre = atom(data.consumeAsciiString(16));
        String mid = atom(data.consumeAsciiString(16));
        String quoted = atom(data.consumeAsciiString(16));
        String post = atom(data.consumeAsciiString(16));
        String arg = atom(data.consumeString(16));

        int escapedQuoteRuns = data.consumeInt(1, 4);
        int formatIndex = data.consumeInt(0, 3);

        StringBuilder pattern = new StringBuilder();
        pattern.append(pre);

        for (int i = 0; i < escapedQuoteRuns; i++) {
            pattern.append("''");
            pattern.append((i & 1) == 0 ? mid : post);
            if (data.consumeBoolean()) {
                pattern.append(' ');
            }
        }

        if (data.consumeBoolean()) {
            pattern.append('\'').append(quoted).append('\'');
            if (data.consumeBoolean()) {
                pattern.append(' ');
            }
        }

        pattern.append('{').append(formatIndex).append('}');

        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append('\'').append(post).append('\'');
        } else {
            pattern.append(post);
        }

        String p = pattern.toString();
        ExtendedMessageFormat emf = new ExtendedMessageFormat(p, data.consumeBoolean() ? Collections.EMPTY_MAP : null);
        String before = emf.toPattern();
        String formatted = emf.format(new Object[] { arg, "B", "C", "D" });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException(
                    "[oracle:topattern-stable] metamorphic violation: formatting must not change toPattern input="
                            + p + " lhs=" + before + " rhs=" + after);
        }

        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(before, data.consumeBoolean() ? Collections.EMPTY_MAP : null);
        String reparsedPattern = reparsed.toPattern();
        if (!before.equals(reparsedPattern)) {
            throw new RuntimeException(
                    "[oracle:topattern-idempotent] metamorphic violation: reparsing toPattern must be stable input="
                            + p + " lhs=" + before + " rhs=" + reparsedPattern);
        }

        if (formatted.length() == 0) {
            throw new RuntimeException(
                    "[oracle:nonempty-format] metamorphic violation: valid nonempty pattern with one argument should not format to empty string input="
                            + p + " lhs=" + formatted + " rhs=non-empty");
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '{' && ch != '}' && ch != '\'') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            return "x";
        }
        return out.toString();
    }
}