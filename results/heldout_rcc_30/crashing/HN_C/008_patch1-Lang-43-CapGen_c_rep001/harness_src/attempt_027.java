package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String a = data.consumeString(32);
        String b = data.consumeAsciiString(32);
        String c = data.consumeRemainingAsString();

        String[] atoms = new String[] {
            "",
            "'",
            "''",
            "'''",
            "{",
            "}",
            ",",
            "0",
            "-1",
            "text",
            a,
            b,
            c
        };

        java.util.Locale[] locales = new java.util.Locale[] {
            java.util.Locale.ROOT,
            java.util.Locale.US,
            java.util.Locale.ENGLISH,
            java.util.Locale.FRANCE,
            java.util.Locale.GERMANY,
            java.util.Locale.JAPAN
        };
        java.util.Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        String[] patterns = new String[] {
            a,
            b,
            c,
            "'",
            "''",
            "'''",
            "''''",
            "'" + a,
            a + "'",
            "'" + a + "'",
            "''" + a,
            a + "''",
            "'" + a + "''",
            "''" + a + "'",
            "'" + a + "''" + b,
            "'" + a + "'" + b,
            a + "'" + b,
            a + "''" + b,
            "{" + a,
            a + "}",
            "{" + a + "}",
            "{0}",
            "{0,number}",
            "{0,date}",
            "{0,time}",
            "{0,choice," + a + "}",
            "{0," + a + "}",
            "{0," + a + "," + b + "}",
            "{0,'" + a + "'}",
            "{0,''" + a + "''}",
            "{0," + a + ",'" + b + "'}",
            "prefix '" + a,
            "prefix '" + a + "'",
            "prefix ''" + a,
            "prefix ''" + a + "''",
            "prefix {" + a + "} suffix",
            "prefix {0," + a + "} suffix",
            "prefix {0," + a + "," + b + "} suffix",
            "'{'",
            "'}'",
            "'{0}'",
            "'{0",
            "{0,'",
            "{0,''",
            "{0,choice,'",
            "{0,choice,''",
            "{0,choice," + a + "'",
            "{0,choice,'" + a,
            "{0,choice,''" + a,
            "{0,choice," + a + "''",
            a + "{0}" + b,
            a + "'{0}" + b,
            a + "''{0}" + b,
            a + "{0,'" + b,
            a + "{0,''" + b,
            a + "{0," + b + ",'" + c,
            a + "{0," + b + ",''" + c,
            "'" + "{0," + a + "," + b + "}",
            "{0," + a + "," + b + "}'",
            "'{" + a + "}",
            "'{" + a,
            "{" + a + "}'",
            "{0," + a + "," + b + c + "}",
            a + "'" + b + "'" + c,
            a + "''" + b + "''" + c,
            a + "'" + b + "''" + c,
            a + "''" + b + "'" + c
        };

        for (int i = 0; i < patterns.length; i++) {
            String p = patterns[i];

            new ExtendedMessageFormat(p);
            new ExtendedMessageFormat(p, locale);
            new ExtendedMessageFormat(p, locale, null);
            new ExtendedMessageFormat(p, locale, java.util.Collections.EMPTY_MAP);

            ExtendedMessageFormat emf = new ExtendedMessageFormat("");
            emf.applyPattern(p);
            emf.toPattern();
            emf.format(new Object[] { a, b, c, Integer.valueOf(data.consumeInt()) }, new StringBuffer(), new java.text.FieldPosition(0));

            for (int x = 0; x < atoms.length; x++) {
                String q1 = atoms[x] + p;
                ExtendedMessageFormat e1 = new ExtendedMessageFormat("");
                e1.applyPattern(q1);
                e1.toPattern();

                String q2 = p + atoms[x];
                ExtendedMessageFormat e2 = new ExtendedMessageFormat("");
                e2.applyPattern(q2);
                e2.toPattern();

                for (int y = 0; y < atoms.length; y++) {
                    String q3 = atoms[x] + p + atoms[y];
                    ExtendedMessageFormat e3 = new ExtendedMessageFormat("");
                    e3.applyPattern(q3);
                    e3.toPattern();
                }
            }
        }
    }
}