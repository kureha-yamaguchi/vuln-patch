package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeString(32);
        String rest = data.consumeRemainingAsString();

        int argIndex = data.consumeInt(-3, 3);
        Locale[] locales = new Locale[] {
            Locale.ROOT, Locale.US, Locale.UK, Locale.FRANCE, Locale.GERMANY, Locale.JAPAN
        };
        Locale locale = locales[Math.abs(argIndex) % locales.length];

        Map registry = data.consumeBoolean() ? new HashMap() : Collections.EMPTY_MAP;

        String[] patterns = new String[] {
            s1,
            "'" + s1,
            "'" + s1 + "'",
            "''" + s1,
            s1 + "''" + s2,
            "prefix '" + s1 + "' suffix",
            "prefix '' suffix " + s2,
            "{" + argIndex + "}",
            "{" + argIndex + "," + s2 + "}",
            "{" + argIndex + ",'" + s1 + "'}",
            "{" + argIndex + "," + s2 + ",'" + s1 + "'}",
            "{" + argIndex + "," + s2 + "," + "'" + s1 + "'" + "}",
            "{" + argIndex + "," + "'" + s1,
            "{" + argIndex + "," + s2 + "," + "'" + s1,
            s3 + "{0}" + rest,
            "'" + s3 + "{0}" + rest,
            "{" + argIndex + ",choice,'" + s1 + "'#x}",
            "{" + argIndex + ",choice,0#'" + s1 + "'|1#" + s2 + "}",
            "a{" + argIndex + "," + s2 + "," + s1 + "}b",
            "a'{" + s1 + "}'b",
            "{0," + rest + "}",
            "{0,'" + rest + "'}",
            rest + "'"
        };

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];

            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
            emf1.toPattern();
            emf1.hashCode();

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, locale, registry);
            emf2.toPattern();
            emf2.hashCode();
            emf2.equals(emf1);

            String nextPattern = patterns[(i + 1) % patterns.length];
            emf1.applyPattern(nextPattern);
            emf1.toPattern();
            emf1.equals(emf2);

            emf2.applyPattern(patterns[(i + 2) % patterns.length]);
            emf2.toPattern();
        }
    }
}