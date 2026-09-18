package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
            Locale.CANADA
        };

        int localeIdx = data.consumeInt(0, locales.length - 1);
        boolean useNullRegistry = data.consumeBoolean();
        int pick1 = data.consumeInt(0, 13);
        int pick2 = data.consumeInt(0, 13);
        int pick3 = data.consumeInt(0, 13);
        int pick4 = data.consumeInt(0, 13);
        int pick5 = data.consumeInt(0, 13);

        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeRemainingAsString();

        String[] patterns = new String[] {
            s1,
            s2,
            s1 + s2,
            s2 + s3,
            "'" + s1,
            s1 + "'",
            "'" + s1 + "'",
            s1 + "''" + s2,
            "'" + s1 + "''" + s2 + "'",
            "{" + s1 + "}",
            s1 + "{0}" + s2,
            s1 + "{0," + s2 + "}" + s3,
            s1 + "'" + s2 + "'" + s3,
            s1 + "{0,'" + s2 + "'}" + s3
        };

        Locale locale = locales[localeIdx];
        Map registry = useNullRegistry ? null : new HashMap();

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(patterns[pick1], locale, registry);
        emf1.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(patterns[pick2], locale);
        emf2.toPattern();
        emf2.applyPattern(patterns[pick3]);
        emf2.toPattern();

        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(patterns[pick4], registry);
        emf3.toPattern();
        emf3.applyPattern(patterns[pick5]);
        emf3.toPattern();

        new ExtendedMessageFormat(patterns[pick1]);
    }
}