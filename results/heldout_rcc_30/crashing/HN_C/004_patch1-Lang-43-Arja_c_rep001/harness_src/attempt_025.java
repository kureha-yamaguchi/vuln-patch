package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Locale;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale[] locales = Locale.getAvailableLocales();
        Locale locale = locales.length == 0
                ? Locale.ROOT
                : locales[data.consumeInt(0, locales.length - 1)];

        String a = data.consumeString(32);
        String b = data.consumeAsciiString(32);
        String c = data.consumeRemainingAsString();

        String escapedA = a.replace("'", "''");
        String escapedB = b.replace("'", "''");
        String escapedC = c.replace("'", "''");

        String pattern;
        switch (data.consumeInt(0, 9)) {
            case 0:
                pattern = a;
                break;
            case 1:
                pattern = "'" + a + "'";
                break;
            case 2:
                pattern = "'" + a + "''";
                break;
            case 3:
                pattern = "'" + escapedA + "'";
                break;
            case 4:
                pattern = b + "'" + c + "''";
                break;
            case 5:
                pattern = "{0}";
                break;
            case 6:
                pattern = "{0,'" + a + "''}";
                break;
            case 7:
                pattern = "{0," + b + "}";
                break;
            case 8:
                pattern = "{0," + "'" + escapedB + "''" + "}";
                break;
            default:
                pattern = "'" + escapedA + "'" + b + "{0," + escapedC + "}";
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, locale, Collections.EMPTY_MAP);
        emf1.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat("", locale, Collections.EMPTY_MAP);
        emf2.applyPattern(pattern);
        emf2.toPattern();

        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(pattern, Collections.EMPTY_MAP);
        emf3.toPattern();
    }
}