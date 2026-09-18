package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeRemainingAsString();

        String[] patterns = new String[] {
            "",
            "'",
            "''",
            "'''",
            "{0}",
            "'{0}'",
            "'" + s1,
            s1 + "'",
            "'" + s1 + "'",
            s1,
            s2,
            s3,
            s1 + s2,
            s2 + s3,
            s1 + "'" + s2,
            s1 + "''" + s2,
            s1 + "'{" + s2,
            s1 + "{0,'" + s2,
            "prefix '" + s1 + "' suffix",
            "{" + Math.abs(data.consumeInt()) + "}",
            "{" + Math.abs(data.consumeInt()) + "," + s2 + "}",
            "'" + s1 + "''" + s2 + "'",
            s1 + "'{" + Math.abs(data.consumeInt()) + "}" + s2,
            "'" + s1 + s2 + s3,
            s1 + "'" + s2 + "'" + s3
        };

        Locale[] locales = new Locale[] {
            Locale.getDefault(),
            Locale.ROOT,
            Locale.US,
            Locale.UK,
            Locale.FRANCE,
            new Locale(s2.length() > 0 ? s2.substring(0, Math.min(2, s2.length())) : "",
                    s1.length() > 0 ? s1.substring(0, Math.min(2, s1.length())) : "")
        };

        Map registry = data.consumeBoolean() ? null : new HashMap();

        for (Locale locale : locales) {
            for (String pattern : patterns) {
                ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern);
                emf1.toPattern();
                emf1.applyPattern(pattern);
                emf1.toPattern();

                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, locale);
                emf2.toPattern();
                emf2.applyPattern(pattern);
                emf2.toPattern();

                ExtendedMessageFormat emf3 = new ExtendedMessageFormat(pattern, registry);
                emf3.toPattern();
                emf3.applyPattern(pattern);
                emf3.toPattern();

                ExtendedMessageFormat emf4 = new ExtendedMessageFormat(pattern, locale, registry);
                emf4.toPattern();
                emf4.applyPattern(pattern);
                emf4.toPattern();

                if (data.consumeBoolean()) {
                    String alt = pattern + "'" + data.consumeAsciiString(16);
                    emf4.applyPattern(alt);
                    emf4.toPattern();
                }
            }
        }
    }
}