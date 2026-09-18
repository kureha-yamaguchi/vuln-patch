package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        ExtendedMessageFormat emf = new ExtendedMessageFormat("", Locale.getDefault(), registry);

        String a = data.consumeString(64);
        String b = data.consumeAsciiString(64);
        String c = data.consumeRemainingAsString();

        String quote = "'";
        String pattern;
        switch (data.consumeInt(0, 11)) {
            case 0:
                pattern = a;
                break;
            case 1:
                pattern = quote + a;
                break;
            case 2:
                pattern = a + quote;
                break;
            case 3:
                pattern = "{0," + a + "}";
                break;
            case 4:
                pattern = "{0," + quote + a + "}";
                break;
            case 5:
                pattern = "{0," + a + quote + "}";
                break;
            case 6:
                pattern = "{0," + quote + a + quote + "}";
                break;
            case 7:
                pattern = "x{0," + quote + a + quote + "}y";
                break;
            case 8:
                pattern = "{0," + b + quote + c + "}";
                break;
            case 9:
                pattern = "{0," + quote + b + "''" + c + "}";
                break;
            case 10:
                pattern = "{0," + "''" + a + quote + b + "}";
                break;
            default:
                pattern = a + "{0," + quote + b + c + "}" + quote;
                break;
        }

        if (data.consumeBoolean()) {
            emf.applyPattern(pattern);
        } else {
            Locale locale;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    locale = Locale.ROOT;
                    break;
                case 1:
                    locale = Locale.US;
                    break;
                case 2:
                    locale = Locale.getDefault();
                    break;
                default:
                    locale = new Locale(b.length() > 0 ? b.substring(0, 1) : "", c.length() > 1 ? c.substring(0, 2) : "");
                    break;
            }
            new ExtendedMessageFormat(pattern, locale, registry);
        }
    }
}