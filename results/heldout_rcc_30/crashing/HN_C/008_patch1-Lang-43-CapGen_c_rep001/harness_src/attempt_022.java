package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Locale;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        String s3 = data.consumeString(32);
        String rest = data.consumeRemainingAsString();
        int idx = data.consumeInt(0, 3);

        String pattern;
        switch (data.consumeInt(0, 19)) {
            case 0:
                pattern = "'";
                break;
            case 1:
                pattern = "''";
                break;
            case 2:
                pattern = "'''";
                break;
            case 3:
                pattern = "'" + s1;
                break;
            case 4:
                pattern = s1 + "'";
                break;
            case 5:
                pattern = "'" + s1 + "'";
                break;
            case 6:
                pattern = "'" + s1 + "''" + s2;
                break;
            case 7:
                pattern = "{0}'";
                break;
            case 8:
                pattern = "{0}'" + s1;
                break;
            case 9:
                pattern = "'" + s1 + "{0}";
                break;
            case 10:
                pattern = "{0,'" + s2;
                break;
            case 11:
                pattern = "pre'" + s1 + "'post";
                break;
            case 12:
                pattern = "pre'" + s1;
                break;
            case 13:
                pattern = "'{" + idx + "}";
                break;
            case 14:
                pattern = "{'" + idx + "'}";
                break;
            case 15:
                pattern = "{0}" + "'" + rest;
                break;
            case 16:
                pattern = rest + "'";
                break;
            case 17:
                pattern = "'" + rest;
                break;
            case 18:
                pattern = s1 + "'" + s2 + "'" + s3;
                break;
            default:
                pattern = s1 + "'" + s2 + rest;
                break;
        }

        Locale locale;
        switch (data.consumeInt(0, 4)) {
            case 0:
                locale = Locale.ROOT;
                break;
            case 1:
                locale = Locale.US;
                break;
            case 2:
                locale = Locale.UK;
                break;
            case 3:
                locale = Locale.JAPAN;
                break;
            default:
                locale = Locale.GERMANY;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat("");
        emf.applyPattern(pattern);
        emf.toPattern();
        emf.format(new Object[] { s1, Integer.valueOf(data.consumeInt()), s2, s3 });

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, locale);
        emf2.toPattern();
        emf2.format(new Object[] { s1, Integer.valueOf(data.consumeInt()), s2, rest });
    }
}