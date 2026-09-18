package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n1 = data.consumeInt();
        int n2 = data.consumeInt(-8, 8);
        boolean b1 = data.consumeBoolean();
        boolean b2 = data.consumeBoolean();
        String raw = data.consumeString(128);
        String ascii = data.consumeAsciiString(128);
        String tail = data.consumeRemainingAsString();

        Locale[] locales = new Locale[] {
            Locale.ROOT,
            Locale.US,
            Locale.UK,
            Locale.FRANCE,
            Locale.GERMANY,
            Locale.JAPAN,
            Locale.CHINA
        };
        Locale locale = locales[Math.abs(n1) % locales.length];

        Map registry = b1 ? null : new HashMap();

        String[] patterns = new String[] {
            raw,
            ascii,
            tail,
            "",
            "'",
            "''",
            "'''",
            raw + ascii,
            ascii + raw,
            raw + tail,
            "'" + raw,
            raw + "'",
            "'" + raw + "'",
            "''" + raw + "''",
            "'" + ascii + "''" + tail,
            raw + "''" + tail,
            "{" + Math.abs(n2) + "}",
            "{0}",
            "{1}",
            "{0,number}",
            "{0,date}",
            "{0,time}",
            raw + "{0}" + tail,
            ascii + "{1,number}" + raw,
            "'" + raw + "{0}" + tail,
            raw + "'" + ascii,
            "'" + raw + "''" + ascii + "'",
            "prefix '" + raw + "' suffix",
            "{0," + ascii + "}",
            "{0," + raw + "}",
            "{" + ascii + "}",
            raw + "{" + ascii + "}" + tail
        };

        Object[] args = new Object[] {
            raw,
            ascii,
            tail,
            Integer.valueOf(n1),
            Integer.valueOf(n2),
            Boolean.valueOf(b2)
        };

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];

            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern);
            emf1.format(args);
            emf1.toPattern();
            emf1.applyPattern(patterns[(i + 1) % patterns.length]);
            emf1.format(args);
            emf1.toPattern();

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, locale);
            emf2.format(args);
            emf2.toPattern();
            emf2.applyPattern(patterns[(i + 2) % patterns.length]);
            emf2.format(args);
            emf2.toPattern();

            ExtendedMessageFormat emf3 = new ExtendedMessageFormat(pattern, registry);
            emf3.format(args);
            emf3.toPattern();
            emf3.applyPattern(patterns[(i + 3) % patterns.length]);
            emf3.format(args);
            emf3.toPattern();

            ExtendedMessageFormat emf4 = new ExtendedMessageFormat(pattern, locale, registry);
            emf4.format(args);
            emf4.toPattern();
            emf4.applyPattern(patterns[(i + 4) % patterns.length]);
            emf4.format(args);
            emf4.toPattern();
        }
    }
}