package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        String s3 = data.consumeString(32);

        int n1 = data.consumeInt();
        int n2 = data.consumeInt(0, 3);
        int n3 = data.consumeInt(0, 4);
        boolean b1 = data.consumeBoolean();
        boolean b2 = data.consumeBoolean();

        String tail = data.consumeRemainingAsString();

        String[] patterns = new String[] {
            "",
            "'",
            "''",
            "'''",
            s1,
            s2,
            s3,
            tail,
            s1 + s2,
            s1 + "'" + s2,
            "'" + s1,
            s1 + "'",
            "'" + s1 + "'",
            "''" + s1,
            s1 + "''",
            s1 + "''" + s2,
            "'" + s1 + "''" + s2 + "'",
            "{0}",
            "{1}",
            "{" + n2 + "}",
            "{" + n2 + "," + s2 + "}",
            "{0," + s2 + "}",
            "{0,'" + s2 + "'}",
            "'{0}'",
            "{'" + s2 + "'}",
            s1 + "{0}" + s2,
            s1 + "'{0}" + s2,
            s1 + "{0'" + s2,
            s1 + "{0," + s2 + "}" + tail,
            s1 + "'" + tail,
            "'" + tail + "'",
            s1 + "{" + n3 + "," + tail + "}",
            tail + "''" + s1,
            tail + "'" + s2 + "'",
            tail + "{0}",
            tail + "{0," + s1 + "}",
            "prefix '" + s1 + "' suffix",
            "prefix ''" + s1 + "'' suffix",
            "{" + Math.abs(n1 % 10) + "," + s1 + "}"
        };

        Locale[] locales = new Locale[] {
            Locale.getDefault(),
            Locale.ROOT,
            Locale.US,
            Locale.UK,
            Locale.CHINA
        };

        String pattern1 = patterns[Math.floorMod(n1, patterns.length)];
        String pattern2 = patterns[Math.floorMod(n1 ^ 0x5a5a5a5a, patterns.length)];
        Locale locale = locales[Math.floorMod(n1 + n2, locales.length)];
        Map registry = b1 ? null : Collections.EMPTY_MAP;

        switch (Math.floorMod(n1, 7)) {
            case 0: {
                new ExtendedMessageFormat(pattern1);
                break;
            }
            case 1: {
                new ExtendedMessageFormat(pattern1, locale);
                break;
            }
            case 2: {
                new ExtendedMessageFormat(pattern1, locale, registry);
                break;
            }
            case 3: {
                ExtendedMessageFormat emf = new ExtendedMessageFormat("");
                emf.applyPattern(pattern1);
                break;
            }
            case 4: {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern1, locale, registry);
                emf.applyPattern(pattern2);
                emf.toPattern();
                break;
            }
            case 5: {
                ExtendedMessageFormat a = new ExtendedMessageFormat(pattern1, locale, registry);
                ExtendedMessageFormat b = new ExtendedMessageFormat(pattern2, b2 ? locale : locales[Math.floorMod(n3, locales.length)], registry);
                a.equals(b);
                a.hashCode();
                a.toPattern();
                b.toPattern();
                break;
            }
            default: {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern1, locale, registry);
                emf.applyPattern(pattern2 + "'" + pattern1);
                emf.toPattern();
                break;
            }
        }
    }
}