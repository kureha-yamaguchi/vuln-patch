package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.Locale;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runPattern("it''s a {0} 'test'!", "DUMMY");

        String a = clean(data.consumeAsciiString(20));
        String b = clean(data.consumeAsciiString(20));
        String c = clean(data.consumeAsciiString(20));
        String q = clean(data.consumeAsciiString(12));
        String arg = cleanArg(data.consumeString(20));
        int escapedQuoteRuns = data.consumeInt(1, 4);
        boolean leadingArg = data.consumeBoolean();
        boolean trailingQuoted = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (leadingArg) {
            pattern.append("{0} ");
        }
        pattern.append(a);
        for (int i = 0; i < escapedQuoteRuns; i++) {
            pattern.append("''");
            if (i + 1 < escapedQuoteRuns) {
                pattern.append(clean(data.consumeAsciiString(8)));
            }
        }
        pattern.append(b);
        pattern.append(" {0}");
        if (trailingQuoted) {
            pattern.append(" '");
            pattern.append(q);
            pattern.append('\'');
        }
        pattern.append(c);

        runPattern(pattern.toString(), arg);
    }

    private static void runPattern(String pattern, String arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.US);
        String before = emf.toPattern();
        String emfOut = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        /*
         * Oracle: format is read-only with respect to the stored pattern, so toPattern() must be stable
         * before and after formatting. A fix that only bypasses quote processing could corrupt bookkeeping.
         */
        if (!eq(before, after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format changed toPattern input=" + pattern + " before=" + before + " after=" + after);
        }

        /*
         * Oracle: for patterns using only standard MessageFormat syntax, ExtendedMessageFormat must behave
         * like MessageFormat. We generate only valid-by-construction patterns with one argument and quotes.
         */
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        String mfOut = mf.format(new Object[] { arg });
        if (!eq(emfOut, mfOut)) {
            throw new RuntimeException("[oracle:messageformat-equivalence] metamorphic violation: input=" + pattern + " arg=" + arg + " lhs=" + emfOut + " rhs=" + mfOut);
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