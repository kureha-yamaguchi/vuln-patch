package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchorPattern = "it''s a {0} 'test'!";
        checkPattern(anchorPattern, registry, "DUMMY");

        String a = atom(data.consumeAsciiString(8));
        String b = atom(data.consumeAsciiString(8));
        String c = atom(data.consumeAsciiString(8));
        String d = atom(data.consumeAsciiString(8));
        String arg = nonEmpty(data.consumeString(16));

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = a + "''" + b + " {0} '" + c + "' " + d;
                break;
            case 1:
                pattern = a + "''s " + b + " {0} '" + c + "'!";
                break;
            case 2:
                pattern = "'" + a + "' " + b + "''" + c + " {0}";
                break;
            case 3:
                pattern = a + " {0} " + b + "''" + c + " '" + d + "'";
                break;
            case 4:
                pattern = a + "''" + b + " x {0} 'y" + c + "'";
                break;
            default:
                pattern = a + "''" + b + " {0} '" + c + "' '" + d + "'";
                break;
        }

        checkPattern(pattern, registry, arg);
    }

    private static void checkPattern(String pattern, Map registry, String arg) {
        MessageFormat mf = new MessageFormat(pattern);
        String expected = mf.format(new Object[] { arg });

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Contract: formatting should not mutate the parsed pattern's textual form.
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: formatting changed toPattern input="
                    + pattern + " before=" + before + " after=" + after);
        }

        // Contract: with no custom format elements used, ExtendedMessageFormat must behave like MessageFormat.
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:mf-equiv] metamorphic violation: ExtendedMessageFormat differs from MessageFormat input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}') {
                sb.append('X');
            } else if (Character.isISOControl(ch)) {
                sb.append('Y');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('Z');
        }
        return sb.toString();
    }

    private static String nonEmpty(String s) {
        if (s == null || s.length() == 0) {
            return "DUMMY";
        }
        return s;
    }
}