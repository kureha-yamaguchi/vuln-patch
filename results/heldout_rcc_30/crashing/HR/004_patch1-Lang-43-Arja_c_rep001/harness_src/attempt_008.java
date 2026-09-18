package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("force-custom-path", "non-empty");

        exercise("it''s a {0} 'test'!", "dummy", "it's a dummy test!", registry);

        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitize(data.consumeAsciiString(16));

        if (a.length() == 0) {
            a = "a";
        }
        if (b.length() == 0) {
            b = "b";
        }
        if (c.length() == 0) {
            c = "c";
        }

        String arg = sanitize(data.consumeAsciiString(12));
        if (arg.length() == 0) {
            arg = "x";
        }

        int variant = data.consumeInt(0, 7);
        String pattern;
        String expected;

        switch (variant) {
            case 0:
                pattern = "''" + a + "{0}" + b;
                expected = "'" + a + arg + b;
                break;
            case 1:
                pattern = a + "''" + b + "{0}" + c;
                expected = a + "'" + b + arg + c;
                break;
            case 2:
                pattern = a + "{0}" + "''" + b;
                expected = a + arg + "'" + b;
                break;
            case 3:
                pattern = a + "''" + "{0}" + "''" + b;
                expected = a + "'" + arg + "'" + b;
                break;
            case 4:
                pattern = a + " {0} '' " + b;
                expected = a + " " + arg + " ' " + b;
                break;
            case 5:
                pattern = a + "''" + b + " {0} 'x' " + c;
                expected = a + "'" + b + " " + arg + " x " + c;
                break;
            case 6:
                pattern = "'" + a + "' " + "''" + " {0} " + b;
                expected = a + " ' " + arg + " " + b;
                break;
            default:
                pattern = a + "''" + b + "''" + c + " {0}";
                expected = a + "'" + b + "'" + c + " " + arg;
                break;
        }

        exercise(pattern, arg, expected, registry);
    }

    private static void exercise(String pattern, String arg, String expected, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });
        assertEquals(expected, actual, "format");

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(emf.toPattern(), registry);
        String actual2 = emf2.format(new Object[] { arg });
        assertEquals(actual, actual2, "roundtrip");
    }

    private static void assertEquals(String expected, String actual, String oracleId) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new RuntimeException(
                    "[oracle:" + oracleId + "] metamorphic violation: expected=" + expected + " actual=" + actual);
        }
    }

    private static String sanitize(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length() && sb.length() < 16; i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == ' ') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}