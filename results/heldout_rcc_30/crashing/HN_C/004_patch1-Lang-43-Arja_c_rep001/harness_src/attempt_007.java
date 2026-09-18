package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Locale;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale locale;
        switch (data.consumeInt(0, 4)) {
            case 0:
                locale = Locale.ROOT;
                break;
            case 1:
                locale = Locale.US;
                break;
            case 2:
                locale = Locale.JAPAN;
                break;
            case 3:
                locale = Locale.GERMANY;
                break;
            default:
                locale = Locale.getDefault();
                break;
        }

        String a = data.consumeString(16);
        String b = data.consumeAsciiString(16);
        String c = data.consumeRemainingAsString();

        String s1 = a.length() == 0 ? "a" : a;
        String s2 = b.length() == 0 ? "b" : b;
        String s3 = c.length() == 0 ? "c" : c;

        String[] patterns = new String[] {
            "'a''",
            "'" + s1 + "''",
            "'" + s2 + "''",
            "'" + s1 + s2 + "''",
            "x'" + s1 + "''",
            "'" + s1 + "''x",
            "x'" + s1 + "''y",
            "{0}'" + s1 + "''",
            "'" + s1 + "''{0}",
            "{0}x'" + s1 + "''",
            "x{0}'" + s1 + "''y",
            "'" + s1 + "''" + s2,
            s2 + "'" + s1 + "''",
            "'" + s1 + s2 + s3 + "''",
            "prefix'" + s1 + "''suffix",
            "{" + data.consumeInt(0, 3) + "}'" + s1 + "''",
            "'" + s1 + "''{" + data.consumeInt(0, 3) + "}",
            "'" + s1 + "'''" ,
            "''" + "'" + s1 + "''",
            "'" + "'" + s1 + "''",
            "'" + s1 + "''''",
            "'" + s1 + "''" + "'" + s2,
            "{" + data.consumeInt(0, 3) + ",number}'" + s1 + "''",
            "'" + s1 + "'' " + s2,
            s3 + "'" + s1 + "''" + s2
        };

        Object[] args = new Object[] { s1, s2, s3, Integer.valueOf(1) };

        for (int i = 0; i < patterns.length; i++) {
            String p = patterns[i];

            ExtendedMessageFormat emf1 = new ExtendedMessageFormat("");
            emf1.applyPattern(p);
            emf1.toPattern();
            emf1.format(args);

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p);
            emf2.toPattern();
            emf2.format(args);

            ExtendedMessageFormat emf3 = new ExtendedMessageFormat("", locale);
            emf3.applyPattern(p);
            emf3.toPattern();
            emf3.format(args);

            ExtendedMessageFormat emf4 = new ExtendedMessageFormat(p, locale);
            emf4.toPattern();
            emf4.format(args);
        }
    }
}