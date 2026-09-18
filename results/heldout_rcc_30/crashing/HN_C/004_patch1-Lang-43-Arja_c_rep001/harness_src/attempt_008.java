package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String a = data.consumeString(64);
        String b = data.consumeAsciiString(64);
        boolean flip = data.consumeBoolean();
        int n = data.consumeInt();
        int idx = data.consumeInt(0, 9);

        String pattern;
        switch (idx) {
            case 0:
                pattern = a;
                break;
            case 1:
                pattern = "'" + a;
                break;
            case 2:
                pattern = a + "'";
                break;
            case 3:
                pattern = "'" + a + "'";
                break;
            case 4:
                pattern = a + "''" + b;
                break;
            case 5:
                pattern = "{" + Math.abs(n % 4) + "}";
                break;
            case 6:
                pattern = "{" + Math.abs(n % 4) + "," + b + "," + a + "}";
                break;
            case 7:
                pattern = "prefix '" + a + "' suffix";
                break;
            case 8:
                pattern = "{" + Math.abs(n % 4) + "," + b + ",'" + a + "'}";
                break;
            default:
                pattern = "'" + a + "''" + b + "'" + data.consumeRemainingAsString();
                break;
        }

        if (flip) {
            int pos = data.consumeInt(0, pattern.length());
            String left = pattern.substring(0, pos);
            String right = pattern.substring(pos);
            pattern = left + "'" + right;
        }

        Locale[] locales = Locale.getAvailableLocales();
        Locale locale = locales.length == 0
                ? Locale.getDefault()
                : locales[data.consumeInt(0, locales.length - 1)];

        Map registry = new HashMap();

        switch (data.consumeInt(0, 5)) {
            case 0:
                new ExtendedMessageFormat(pattern);
                break;
            case 1:
                new ExtendedMessageFormat(pattern, locale);
                break;
            case 2:
                new ExtendedMessageFormat(pattern, registry);
                break;
            case 3:
                new ExtendedMessageFormat(pattern, locale, registry);
                break;
            case 4:
                new ExtendedMessageFormat("", registry).applyPattern(pattern);
                break;
            default:
                new ExtendedMessageFormat("", locale, registry).applyPattern(pattern);
                break;
        }
    }
}