package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Locale;

public class FuzzHarness {
    private static String sanitizeNoQuote(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\'') {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('a');
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale[] locales = Locale.getAvailableLocales();
        Locale locale = locales.length == 0 ? Locale.ROOT : locales[data.consumeInt(0, locales.length - 1)];

        String core1 = sanitizeNoQuote(data.consumeString(32));
        String core2 = sanitizeNoQuote(data.consumeAsciiString(32));
        String core3 = sanitizeNoQuote(data.consumeRemainingAsString());

        String[] patterns = new String[] {
            "'" + core1 + "''",
            "x'" + core1 + "''",
            "'" + core1 + core2 + "''",
            "'" + core1 + "''" + core2,
            "'" + core1 + core2 + core3 + "''",

            "{0,'" + core1 + "''",
            "{0,'" + core1 + core2 + "''",
            "{0,'" + core1 + "''" + core2,
            "{0,choice,'" + core1 + "''",
            "{0,date,'" + core1 + "''",
            "{0,time,'" + core1 + "''",
            "{0,number,'" + core1 + "''",

            "a{0,'" + core1 + "''",
            "{0," + core1 + ",'" + core2 + "''",
            "{0," + core1 + ",'" + core2 + core3 + "''",

            "''" + core1 + "''",
            "'" + core1 + "''}",
            "{'" + core1 + "''",
            core1 + "{0,'" + core2 + "''" + core3
        };

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];

            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern);
            emf1.toPattern();

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, locale);
            emf2.toPattern();

            ExtendedMessageFormat emf3 = new ExtendedMessageFormat(pattern, locale, Collections.EMPTY_MAP);
            emf3.toPattern();

            ExtendedMessageFormat emf4 = new ExtendedMessageFormat("", locale, Collections.EMPTY_MAP);
            emf4.applyPattern(pattern);
            emf4.toPattern();
        }
    }
}