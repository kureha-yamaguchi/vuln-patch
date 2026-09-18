package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact trigger shape from the regression test, but using a non-null registry so
        // ExtendedMessageFormat takes its custom-pattern parsing path and reaches appendQuotedString.
        // On the buggy version, the escaped quote ("''") causes appendQuotedString to return
        // without advancing ParsePosition, leading to unbounded growth and OutOfMemoryError.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String left = literal(data.consumeAsciiString(16));
        String mid = literal(data.consumeAsciiString(16));
        String right = literal(data.consumeAsciiString(16));
        String arg = data.consumeRemainingAsString();

        // EXPLORE: preserve the root-cause property:
        // - non-null registry, which forces ExtendedMessageFormat through its real parser
        // - an escaped quote token ("''"), which hits the patched branch
        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = left + "''" + mid + " {0}";
                break;
            case 1:
                pattern = left + " {0} " + mid + "''" + right;
                break;
            case 2:
                pattern = left + "''s " + mid + " {0} " + right;
                break;
            case 3:
                pattern = "'" + left + "' " + mid + "''" + right + " {0}";
                break;
            case 4:
                pattern = left + " {0} '' " + mid + " " + right;
                break;
            default:
                pattern = left + "''" + mid + " '{quoted}' " + right + " {0}";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        String expected = new MessageFormat(pattern).format(new Object[] { arg });

        // Oracle 1: with an empty registry and a valid standard MessageFormat pattern, this class
        // should behave like MessageFormat. A bogus "fix" that merely skips quote handling or
        // returns the wrong parsed text would violate this without throwing.
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                "[oracle:msgfmt-equiv] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        // Oracle 2: formatting is read-only; toPattern() should not change across format().
        if (!before.equals(after)) {
            throw new RuntimeException(
                "[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }
    }

    private static String literal(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }
}