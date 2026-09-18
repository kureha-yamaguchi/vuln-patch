package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        java.util.Locale[] locales = new java.util.Locale[] {
            java.util.Locale.ROOT,
            java.util.Locale.US,
            java.util.Locale.UK,
            java.util.Locale.ENGLISH,
            java.util.Locale.FRANCE,
            java.util.Locale.GERMANY,
            java.util.Locale.JAPAN,
            java.util.Locale.CHINA,
            java.util.Locale.CANADA,
            java.util.Locale.getDefault()
        };
        java.util.Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        String x = data.consumeString(data.consumeInt(0, 8));
        String y = data.consumeAsciiString(data.consumeInt(0, 8));
        String z = data.consumeRemainingAsString();

        java.util.Map registry = data.consumeBoolean()
                ? java.util.Collections.EMPTY_MAP
                : new java.util.HashMap();

        String[] quoteTails = new String[] {
            "",
            "'",
            "''",
            "'''",
            "a",
            "a'",
            "a''",
            x,
            x + "'",
            x + "''",
            y,
            y + "'",
            z,
            z + "'"
        };

        String[] prefixes = new String[] {
            "'",
            "x'",
            x + "'",
            "{0,'",
            "{0,number,'",
            "{0,date,'",
            "{0,time,'",
            "{0,choice,'",
            "prefix '",
            "prefix {0,'",
            "prefix {0,number,'",
            "{0," + x + "'",
            "{0," + y + "'"
        };

        for (int i = 0; i < prefixes.length; i++) {
            for (int j = 0; j < quoteTails.length; j++) {
                String pattern = prefixes[i] + quoteTails[j];

                new ExtendedMessageFormat(pattern);
                new ExtendedMessageFormat(pattern, locale);
                new ExtendedMessageFormat(pattern, registry);
                new ExtendedMessageFormat(pattern, locale, registry);

                ExtendedMessageFormat emf1 = new ExtendedMessageFormat("");
                emf1.applyPattern(pattern);
                emf1.toPattern();

                ExtendedMessageFormat emf2 = new ExtendedMessageFormat("", locale);
                emf2.applyPattern(pattern);
                emf2.toPattern();

                ExtendedMessageFormat emf3 = new ExtendedMessageFormat("", registry);
                emf3.applyPattern(pattern);
                emf3.toPattern();

                ExtendedMessageFormat emf4 = new ExtendedMessageFormat("", locale, registry);
                emf4.applyPattern(pattern);
                emf4.toPattern();
            }
        }

        String[] directPatterns = new String[] {
            "'",
            "x'",
            x + "'",
            y + "'",
            z + "'",
            "{0,'",
            "{0,''",
            "{0,number,'",
            "{0,date,'",
            "{0,time,'",
            "{0,choice,'",
            "{0," + x + "'",
            "{0," + y + "'",
            "prefix '",
            "prefix {0,'",
            "prefix {0,number,'",
            "'" + x,
            "'" + y,
            "'" + z,
            "{0,'" + x,
            "{0,'" + y,
            "{0,'" + z,
            "{0,number,'" + x,
            "{0,date,'" + y,
            "{0,time,'" + z
        };

        for (int i = 0; i < directPatterns.length; i++) {
            String pattern = directPatterns[i];
            ExtendedMessageFormat emf = new ExtendedMessageFormat("");
            emf.applyPattern(pattern);
        }
    }
}