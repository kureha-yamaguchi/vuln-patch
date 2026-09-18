package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        try {
            new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        } catch (IllegalArgumentException e) {
        }

        new ExtendedMessageFormat("it''s a {0} 'test'!", registry);

        String prefix = sanitizeLiteral(data.consumeAsciiString(24));
        String middle = sanitizeLiteral(data.consumeAsciiString(24));
        String suffix = sanitizeLiteral(data.consumeAsciiString(24));
        String arg = data.consumeAsciiString(16);

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = prefix + "''" + middle + "{0}" + suffix;
                break;
            case 1:
                pattern = prefix + "{0}" + middle + "''" + suffix;
                break;
            case 2:
                pattern = "'" + prefix + "''" + middle + "'" + suffix + "{0}";
                break;
            case 3:
                pattern = prefix + "''" + middle + " 'x' " + suffix + "{0}";
                break;
            case 4:
                pattern = prefix + "''" + middle + "{0,number}" + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + "{0,date}" + suffix;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String once = emf.format(new Object[] { arg });

        String toPattern = emf.toPattern();
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(toPattern, registry);
        String twice = reparsed.format(new Object[] { arg });
        if (!once.equals(twice)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " lhs=" + once + " rhs=" + twice);
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('a');
        }
        return sb.toString();
    }
}