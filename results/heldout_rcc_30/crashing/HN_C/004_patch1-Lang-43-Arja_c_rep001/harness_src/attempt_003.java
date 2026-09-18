package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        String a = data.consumeAsciiString(24);
        String b = data.consumeAsciiString(24);
        String c = data.consumeAsciiString(24);
        int n = data.consumeInt(-2, 3);
        boolean useLocale = data.consumeBoolean();
        boolean useRegistry = data.consumeBoolean();

        Locale locale = useLocale ? Locale.US : Locale.ROOT;
        Map registry = useRegistry ? Collections.EMPTY_MAP : null;

        String[] patterns = new String[] {
            "'" + a + "''",
            "x'" + a + "''",
            "'" + a + b + "''",
            "{" + n + "}'" + a + "''",
            "{" + n + "}x'" + a + "''",
            "'" + a + "''" + b,
            "x'" + a + "''" + b,
            "'" + a + "''" + b + c,
            "'" + "''",
            "x'" + "''",
            "pre'" + a + "''post",
            "{0,number,'" + a + "''}",
            "{0,date,'" + a + "''}",
            "{0,time,'" + a + "''}",
            "{0,choice,0#'" + a + "''|1#" + b + "}",
            a + "'",
            a + "''",
            "'" + a,
            "{" + n + "," + b + ",'"+ a + "''}",
            "{" + n + "," + b + "," + "'" + a + "''" + "}",
            "'" + a + "''" + "'" + b,
            "'" + a + "''" + "'" + b + "''"
        };

        for (int i = 0; i < patterns.length; i++) {
            String p = patterns[i];

            if (registry == null) {
                new ExtendedMessageFormat(p);
                new ExtendedMessageFormat(p, locale);
                ExtendedMessageFormat emf = new ExtendedMessageFormat("");
                emf.applyPattern(p);
                emf.toPattern();
            } else {
                new ExtendedMessageFormat(p, registry);
                new ExtendedMessageFormat(p, locale, registry);
                ExtendedMessageFormat emf = new ExtendedMessageFormat("", locale, registry);
                emf.applyPattern(p);
                emf.toPattern();
            }
        }
    }
}