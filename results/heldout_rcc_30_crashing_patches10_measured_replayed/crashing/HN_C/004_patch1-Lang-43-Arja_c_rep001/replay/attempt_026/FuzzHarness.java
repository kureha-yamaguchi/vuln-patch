package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Locale;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale[] locales = Locale.getAvailableLocales();
        Locale locale = locales.length == 0 ? Locale.ROOT : locales[data.consumeInt(0, locales.length - 1)];

        String s1 = data.consumeString(24);
        String s2 = data.consumeAsciiString(24);
        String s3 = data.consumeString(24);
        String s4 = data.consumeRemainingAsString();

        String q1 = s1.replace("'", "''");
        String q2 = s2.replace("'", "''");
        String q3 = s3.replace("'", "''");
        String q4 = s4.replace("'", "''");

        String[] patterns = new String[] {
            s1,
            "'" + s1,
            "'" + q1 + "'",
            "{0}",
            "{0," + s1 + "}",
            "{0,'" + q1 + "'}",
            "{0,'" + q1 + "''" + q2 + "'}",
            "{0," + s1 + "'" + q1 + "''" + q2 + "'}",
            "{0," + s1 + "'" + q1 + "''" + q2 + "'" + s2 + "}",
            "{0," + "'" + q1 + "''" + q2 + "'" + "}",
            "{0,choice,0#'" + q1 + "''" + q2 + "'}",
            "{0,date,'" + q1 + "''" + q2 + "'}",
            "{0,time,'" + q1 + "''" + q2 + "'}",
            "{0,number,'" + q1 + "''" + q2 + "'}",
            "{0," + s1 + "," + s2 + "}",
            "{0," + s1 + ",'" + q1 + "''" + q2 + "'}",
            "{0," + s1 + ",'" + q1 + "''" + q2 + "'" + s2 + "}",
            "{0," + s1 + "'" + q1 + "''" + q2 + "'",
            "{0," + s1 + "'" + q1 + "''" + q2,
            "{0,'" + q1 + "''" + q2,
            "{0," + q1 + "'" + q2 + "''" + q3 + "'" + q4 + "}",
            "{0," + q1 + "{1,'" + q2 + "''" + q3 + "'}" + "}",
            s1 + "{0,'" + q2 + "''" + q3 + "'}" + s4,
            "{0," + s1 + "}" + "{1,'" + q2 + "''" + q3 + "'}",
            "{{0,'" + q1 + "''" + q2 + "'}}"
        };

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];

            ExtendedMessageFormat emfA = new ExtendedMessageFormat(pattern, locale, Collections.EMPTY_MAP);
            emfA.toPattern();

            ExtendedMessageFormat emfB = new ExtendedMessageFormat("", locale, Collections.EMPTY_MAP);
            emfB.applyPattern(pattern);
            emfB.toPattern();

            ExtendedMessageFormat emfC = new ExtendedMessageFormat(pattern, Collections.EMPTY_MAP);
            emfC.toPattern();
        }
    }
}