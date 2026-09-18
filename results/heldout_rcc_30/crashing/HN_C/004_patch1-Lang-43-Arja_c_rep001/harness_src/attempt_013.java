package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Locale;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale[] locales = new Locale[] {
            Locale.ROOT,
            Locale.US,
            Locale.UK,
            Locale.FRANCE,
            Locale.GERMANY,
            Locale.JAPAN,
            Locale.CHINA,
            Locale.CANADA,
            Locale.ITALY
        };
        Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeRemainingAsString();

        String[] patterns = new String[] {
            s1,
            s2,
            s3,
            "'" + s1,
            s1 + "'",
            "'" + s1 + "'",
            "''" + s1,
            s1 + "''",
            "'" + s1 + "''" + s2,
            s1 + "{" + s2 + "}",
            s1 + "{0}",
            s1 + "{0," + s2 + "}",
            s1 + "{0," + s2 + "," + s3 + "}",
            s1 + "'" + s2 + "'" + s3,
            s1 + "''" + s2 + "''" + s3,
            "{" + s1 + "}",
            "'" + "{0}" + "'",
            "'" + s2 + "{0}" + s3,
            s1 + "{" + data.consumeInt() + "}",
            s1 + "{" + data.consumeInt(-10, 10) + "," + s2 + "}",
            s1 + "{" + data.consumeInt(-10, 10) + "," + s2 + "," + s3 + "}",
            ""
        };

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat("", locale, Collections.EMPTY_MAP);
        for (int i = 0; i < patterns.length; i++) {
            emf1.applyPattern(patterns[i]);
            emf1.toPattern();
            emf1.toString();
            emf1.hashCode();
            emf1.equals(emf1);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(patterns[data.consumeInt(0, patterns.length - 1)], locale, Collections.EMPTY_MAP);
        emf2.applyPattern(patterns[data.consumeInt(0, patterns.length - 1)]);
        emf2.toPattern();

        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(patterns[data.consumeInt(0, patterns.length - 1)], Collections.EMPTY_MAP);
        emf3.applyPattern(patterns[data.consumeInt(0, patterns.length - 1)]);
        emf3.toPattern();

        ExtendedMessageFormat emf4 = new ExtendedMessageFormat(patterns[data.consumeInt(0, patterns.length - 1)], locale);
        emf4.applyPattern(patterns[data.consumeInt(0, patterns.length - 1)]);
        emf4.toPattern();

        ExtendedMessageFormat emf5 = new ExtendedMessageFormat(patterns[data.consumeInt(0, patterns.length - 1)]);
        emf5.applyPattern(patterns[data.consumeInt(0, patterns.length - 1)]);
        emf5.toPattern();
    }
}