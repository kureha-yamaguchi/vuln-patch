package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String a = data.consumeString(24);
        String b = data.consumeAsciiString(24);
        String c = data.consumeString(24);
        String d = data.consumeRemainingAsString();

        int x = data.consumeInt();
        int y = data.consumeInt(0, 9);
        boolean useNullRegistry = data.consumeBoolean();
        boolean useAltLocale = data.consumeBoolean();

        Locale locale = useAltLocale ? Locale.US : Locale.ROOT;
        Map registry = useNullRegistry ? null : Collections.EMPTY_MAP;

        String[] atoms = new String[] {
            "",
            "'",
            "''",
            "'''",
            "{",
            "}",
            ",",
            "x",
            a,
            b,
            c,
            d
        };

        String[] patterns = new String[] {
            "'",
            "''",
            "'''",
            "''''",
            "a'",
            "'a",
            "'a'",
            "'a''",
            "''a'",
            "{0}",
            "{0,'x'}",
            "{0,''}",
            "{0,}",
            "{0,'}",
            "{0,'''}",
            "{0,''''}",
            "{0,'''''x}",
            "{0,''x''}",
            "{0,'" + a + "'}",
            "{0,''" + a + "}",
            "{0,'" + a + "''}",
            "{0,''" + a + "''}",
            "{0,''" + a + "''" + b + "}",
            "{0,'" + a + "'" + b + "}",
            "{0," + a + "}",
            "{0," + a + "'}",
            "{0,'" + a + "}",
            "{0,''" + a + "'}",
            "{0,'" + a + "''" + b + "}",
            "{0,''" + a + "''" + b + "''}",
            "{0,''" + a + "''" + b + "''" + c + "}",
            "{0,''" + a + "''," + b + "}",
            "{0," + a + ",''}",
            "{0," + a + ",'}",
            "{0," + a + ",'''}",
            "{0," + a + ",''" + b + "}",
            "{0," + a + ",'" + b + "}",
            "{0," + a + ",'" + b + "''}",
            "{0," + a + ",''" + b + "''}",
            "{0," + a + ",''" + b + "''" + c + "}",
            "prefix '{0}",
            "prefix ''{0}",
            "prefix {0,''}",
            "prefix {0,'x}",
            "prefix {0,''x}",
            "prefix {0,''x''}",
            "'" + a,
            a + "'",
            "'" + a + "'",
            "'" + a + "''",
            "''" + a + "'",
            "'" + a + "''" + b,
            "'" + a + "''" + b + "'",
            "{'" + a + "'}",
            "{''" + a + "}",
            "{''" + a + "''}",
            "{0,choice,0#''}",
            "{0,choice,0#'" + a + "}",
            "{0,choice,0#''" + a + "}",
            "{0,choice,0#''" + a + "''}",
            "{0,choice,0#'" + a + "''" + b + "}",
            "{0,date,'}",
            "{0,time,''}",
            "{0,number,'''}",
            atoms[Math.floorMod(x, atoms.length)] + atoms[Math.floorMod(x + 1, atoms.length)] + atoms[Math.floorMod(x + 2, atoms.length)],
            "{0," + atoms[Math.floorMod(x + 3, atoms.length)] + atoms[Math.floorMod(x + 4, atoms.length)] + "}",
            "{0,'" + atoms[Math.floorMod(x + 5, atoms.length)] + atoms[Math.floorMod(x + 6, atoms.length)] + "}",
            "{0,''" + atoms[Math.floorMod(x + 7, atoms.length)] + atoms[Math.floorMod(x + 8, atoms.length)] + "}",
            "{0,''" + atoms[Math.floorMod(x + 1, atoms.length)] + "''" + atoms[Math.floorMod(x + 2, atoms.length)] + "}",
            "{0," + atoms[Math.floorMod(x + 3, atoms.length)] + ",''" + atoms[Math.floorMod(x + 4, atoms.length)] + "''}",
            "{0," + atoms[Math.floorMod(x + 5, atoms.length)] + ",'" + atoms[Math.floorMod(x + 6, atoms.length)] + "}",
            "{0," + atoms[Math.floorMod(x + 7, atoms.length)] + ",''" + atoms[Math.floorMod(x + 8, atoms.length)] + "''" + atoms[Math.floorMod(x + 9, atoms.length)] + "}"
        };

        for (int i = 0; i < patterns.length; i++) {
            String p = patterns[(i + y) % patterns.length];

            new ExtendedMessageFormat(p);
            new ExtendedMessageFormat(p, locale);
            new ExtendedMessageFormat(p, locale, registry);

            ExtendedMessageFormat emf = new ExtendedMessageFormat("", locale, registry);
            emf.applyPattern(p);
            emf.toPattern();
            emf.hashCode();
            emf.equals(new ExtendedMessageFormat(p, locale, registry));
        }
    }
}