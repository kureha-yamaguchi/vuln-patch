package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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

        String s1 = data.consumeString(data.consumeInt(0, 64));
        String s2 = data.consumeAsciiString(data.consumeInt(0, 64));
        String s3 = data.consumeRemainingAsString();

        java.util.Map registry = data.consumeBoolean()
                ? java.util.Collections.EMPTY_MAP
                : new java.util.HashMap();

        String[] patterns = new String[] {
            s1,
            s2,
            s3,
            "",
            "'",
            "''",
            "'''",
            "'" + s1,
            s1 + "'",
            "'" + s1 + "'",
            "'" + s2,
            s2 + "'",
            "'" + s2 + "'",
            "{0}",
            "{0} " + s1,
            s1 + " {0}",
            "'{0}'",
            "''{0}''",
            "'" + "{0}",
            "{0" + "'",
            "{0,'" + s1,
            "{0," + s1 + "}",
            "{" + s1 + "}",
            s1 + "'" + s2,
            s1 + "''" + s2,
            s1 + "'''" + s2,
            "'" + s1 + "''" + s2 + "'",
            s1 + "{" + s2 + "}",
            s1 + "}" + s2,
            s1 + "{" + s2,
            "'" + s3,
            s3 + "'",
            "'" + s3 + "'",
            s3 + "''",
            "''" + s3,
            "prefix '" + s1 + "' suffix",
            "prefix ''" + s2 + "'' suffix",
            "{0,number,'" + s1 + "'}",
            "{0,date,'" + s2 + "'}",
            "{0,time,'" + s3 + "'}"
        };

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];

            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern);
            emf1.toPattern();
            emf1.applyPattern(pattern);
            emf1.toPattern();

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, locale);
            emf2.toPattern();
            if (i + 1 < patterns.length) {
                emf2.applyPattern(patterns[i + 1]);
                emf2.toPattern();
            }

            ExtendedMessageFormat emf3 = new ExtendedMessageFormat(pattern, registry);
            emf3.toPattern();
            emf3.getFormats();
            emf3.getFormatsByArgumentIndex();
            if (i > 0) {
                emf3.applyPattern(patterns[i - 1]);
                emf3.toPattern();
            }

            ExtendedMessageFormat emf4 = new ExtendedMessageFormat(pattern, locale, registry);
            emf4.toPattern();
            emf4.getFormats();
            emf4.getFormatsByArgumentIndex();
            emf4.equals(emf1);
            emf4.hashCode();
            if (i + 2 < patterns.length) {
                emf4.applyPattern(patterns[i + 2]);
                emf4.toPattern();
            }
        }

        if (data.consumeBoolean()) {
            String dynamic = data.consumeAsciiString(data.consumeInt(0, 128));
            ExtendedMessageFormat emf = new ExtendedMessageFormat(dynamic, locale, registry);
            emf.applyPattern(dynamic + "'" + data.consumeString(data.consumeInt(0, 32)));
            emf.toPattern();
        }
    }
}