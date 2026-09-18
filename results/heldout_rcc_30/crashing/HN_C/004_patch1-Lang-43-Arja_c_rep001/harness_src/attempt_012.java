package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.util.Locale;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String frag1 = data.consumeString(16);
        String frag2 = data.consumeAsciiString(16);
        String frag3 = data.consumeString(8);
        boolean flip = data.consumeBoolean();
        int num = data.consumeInt(-2, 3);

        if (frag1 == null) {
            frag1 = "";
        }
        if (frag2 == null) {
            frag2 = "";
        }
        if (frag3 == null) {
            frag3 = "";
        }

        String core = flip ? frag2 : frag1;
        String alt = flip ? frag1 : frag2;

        String[] patterns = new String[] {
            "'a''",
            "'" + core + "''",
            "'" + core + alt + "''",
            "'" + core + "''" + frag3,
            frag3 + "'" + core + "''",
            "'" + frag3 + core + "''",
            "{0} '" + core + "''",
            "{0}" + "'" + core + "''",
            "'" + core + "''" + "{0}",
            "x'" + core + "''",
            "'" + "x" + "''",
            "'" + core + "''y",
            "'" + core + "''" + "'" ,
            "'" + core + "''" + "{" + Math.abs(num) + "}",
            "'" + core + "''" + "," + alt,
            "'" + core + "''" + " " + alt,
            "'" + core + "''" + "''",
            "'" + core + "''''",
            "'" + core + "'" + "'" ,
            "'" + core + "''" + frag1 + frag2 + frag3
        };

        Object[] args = new Object[] { core, alt, frag3, Integer.valueOf(num) };

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];

            ExtendedMessageFormat emf = new ExtendedMessageFormat("");
            emf.applyPattern(pattern);
            emf.toPattern();
            emf.format(args, new StringBuffer(), new FieldPosition(0));

            ExtendedMessageFormat emfLocale = new ExtendedMessageFormat(pattern, Locale.US);
            emfLocale.toPattern();
            emfLocale.format(args, new StringBuffer(), new FieldPosition(0));
        }
    }
}