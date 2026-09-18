package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern);
        String anchorBefore = anchor.toPattern();
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();
        if (!"it's a DUMMY test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: exact regression pattern formatted incorrectly input="
                    + anchorPattern + " lhs=" + anchorOut + " rhs=it's a DUMMY test!");
        }
        if (anchorBefore != null ? !anchorBefore.equals(anchorAfter) : anchorAfter != null) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                    + anchorPattern + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String prefix = nonEmptyLiteral(data.consumeAsciiString(24), "a");
        String middle = nonEmptyLiteral(data.consumeAsciiString(24), "b");
        String quoted = nonEmptyLiteral(data.consumeAsciiString(16), "test");
        String suffix = cleanArg(data.consumeAsciiString(24), "z");
        String arg = cleanArg(data.consumeString(24), "X");
        int shape = data.consumeInt(0, 3);

        String pattern1;
        String pattern2;

        /*
         * Contract used for these oracles:
         * In MessageFormat patterns, ordinary literal text does not need quoting; quoting a literal
         * segment that contains no special pattern characters must preserve the formatted output.
         * Also, formatting is read-only with respect to the pattern representation, so toPattern()
         * should be stable before and after format().
         *
         * The bug fixed by the patch is specifically on doubled-quote handling ("''"). These patterns
         * are valid by construction and all contain an escaped quote so execution reaches the changed
         * code on construction.
         */
        if (shape == 0) {
            pattern1 = prefix + "''" + middle + " {0} '" + quoted + "' " + suffix;
            pattern2 = prefix + "''" + middle + " {0} " + quoted + " " + suffix;
        } else if (shape == 1) {
            pattern1 = "'" + quoted + "' " + prefix + "''" + middle + " {0}";
            pattern2 = quoted + " " + prefix + "''" + middle + " {0}";
        } else if (shape == 2) {
            pattern1 = prefix + " {0} " + middle + "''" + suffix + " '" + quoted + "'";
            pattern2 = prefix + " {0} " + middle + "''" + suffix + " " + quoted;
        } else {
            pattern1 = prefix + "''" + middle + " '" + quoted + "' {0} " + suffix;
            pattern2 = prefix + "''" + middle + " " + quoted + " {0} " + suffix;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern1);
        String before1 = emf1.toPattern();
        String out1 = emf1.format(new Object[] { arg });
        String after1 = emf1.toPattern();
        if (before1 != null ? !before1.equals(after1) : after1 != null) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                    + pattern1 + " lhs=" + before1 + " rhs=" + after1);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern2);
        String out2 = emf2.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:quoted-vs-unquoted] metamorphic violation: equivalent quoted and unquoted literal patterns differ input="
                    + pattern1 + " lhs=" + out1 + " rhs=" + out2);
        }

        String extraPrefix = nonEmptyLiteral(data.consumeAsciiString(12), "m");
        String extraQuoted = nonEmptyLiteral(data.consumeAsciiString(10), "n");
        String extraArg = cleanArg(data.consumeString(12), "Y");
        String pattern3 = extraPrefix + "''" + " {0} '" + extraQuoted + "'";
        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(pattern3);
        String before3 = emf3.toPattern();
        String out3a = emf3.format(new Object[] { extraArg });
        String after3 = emf3.toPattern();
        if (before3 != null ? !before3.equals(after3) : after3 != null) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                    + pattern3 + " lhs=" + before3 + " rhs=" + after3);
        }
        String out3b = emf3.format(new Object[] { extraArg });
        if (!out3a.equals(out3b)) {
            throw new RuntimeException("[oracle:idempotent-format] metamorphic violation: repeated format on same instance should be stable input="
                    + pattern3 + " lhs=" + out3a + " rhs=" + out3b);
        }
    }

    private static String nonEmptyLiteral(String s, String fallback) {
        String cleaned = cleanLiteral(s);
        return cleaned.length() == 0 ? fallback : cleaned;
    }

    private static String cleanLiteral(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char c = s.charAt(i);
            if (c < 32 || c == '\'' || c == '{' || c == '}') {
                continue;
            }
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-' || c == '.' || c == '!') {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private static String cleanArg(String s, String fallback) {
        if (s == null) {
            return fallback;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char c = s.charAt(i);
            if (c >= 32) {
                sb.append(c);
            }
        }
        String out = sb.toString();
        return out.length() == 0 ? fallback : out;
    }
}