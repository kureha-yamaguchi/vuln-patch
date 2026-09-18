package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, Locale.US, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a DUMMY test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " expected=it's a DUMMY test! actual=" + anchorOut);
        }

        String prefix = clean(data.consumeAsciiString(16));
        String middle = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeAsciiString(16));
        String quoted = clean(data.consumeAsciiString(12));
        String arg = cleanArg(data.consumeString(16));
        int escapedQuoteRuns = data.consumeInt(1, 4);
        boolean leadingArg = data.consumeBoolean();
        boolean trailingQuoted = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (leadingArg) {
            pattern.append("{0} ");
        }
        pattern.append(prefix);
        for (int i = 0; i < escapedQuoteRuns; i++) {
            pattern.append("''");
            if (i + 1 < escapedQuoteRuns) {
                pattern.append(clean(data.consumeAsciiString(6)));
            }
        }
        pattern.append(middle);
        pattern.append(" {0}");
        if (trailingQuoted) {
            pattern.append(" '");
            pattern.append(quoted);
            pattern.append('\'');
        }
        pattern.append(suffix);

        String p = pattern.toString();
        ExtendedMessageFormat emf = new ExtendedMessageFormat(p, Locale.US, registry);
        String before = emf.toPattern();
        String emfOut = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        /*
         * Oracle: formatting is read-only with respect to the parsed pattern, so toPattern() must stay stable.
         */
        if (!eq(before, after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + p + " before=" + before + " after=" + after);
        }

        /*
         * Oracle: with an empty registry and only standard {0} placeholders, ExtendedMessageFormat must
         * behave like MessageFormat on valid-by-construction patterns.
         */
        MessageFormat mf = new MessageFormat(p, Locale.US);
        String mfOut = mf.format(new Object[] { arg });
        if (!eq(emfOut, mfOut)) {
            throw new RuntimeException("[oracle:messageformat-equivalence] metamorphic violation: input=" + p + " arg=" + arg + " lhs=" + emfOut + " rhs=" + mfOut);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                out.append('x');
            } else if (Character.isISOControl(ch)) {
                out.append('y');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "Dummy";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isISOControl(ch)) {
                out.append('a');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append("Dummy");
        }
        return out.toString();
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}