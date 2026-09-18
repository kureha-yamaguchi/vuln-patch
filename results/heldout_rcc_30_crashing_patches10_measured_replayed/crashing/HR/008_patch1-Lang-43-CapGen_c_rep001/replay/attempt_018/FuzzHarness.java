package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact behavioural trigger shape from the bug, but using only standard MessageFormat syntax
        // so the fixed version is obligated to accept it.
        exercise("it''s a {0} 'test'!", registry, "dummy");

        String a = sanitize(data.consumeAsciiString(20));
        String b = sanitize(data.consumeAsciiString(20));
        String c = sanitize(data.consumeAsciiString(20));

        int mode = data.consumeInt(0, 5);
        String pattern;
        switch (mode) {
            case 0:
                pattern = a + "''" + b + " {0} " + c;
                break;
            case 1:
                pattern = a + " {0} ''" + b + c;
                break;
            case 2:
                pattern = a + "''" + b + " '{0}' " + c;
                break;
            case 3:
                pattern = a + " '" + b + "' {0} ''" + c;
                break;
            case 4:
                pattern = "it''s " + a + " {0} '" + b + "' " + c;
                break;
            default:
                pattern = a + "''" + b + " {0} '" + c + "'";
                break;
        }

        exercise(pattern, registry, "dummy");
    }

    private static void exercise(String pattern, Map registry, Object arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        // Contract/oracle: with no custom formats in the pattern, ExtendedMessageFormat must behave
        // the same as MessageFormat on the same valid pattern.
        String actual = emf.format(new Object[] { arg });
        String expected = new MessageFormat(pattern).format(new Object[] { arg });
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:mf-eq] metamorphic violation: pattern=" + pattern + " expected=" + expected + " actual=" + actual);
        }

        // Observable state check: formatting should not mutate the formatter's pattern representation.
        String p1 = emf.toPattern();
        String p2 = emf.toPattern();
        if (p1 == null ? p2 != null : !p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: pattern=" + pattern + " first=" + p1 + " second=" + p2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'' || ch < 0x20) {
                out.append('x');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }
}