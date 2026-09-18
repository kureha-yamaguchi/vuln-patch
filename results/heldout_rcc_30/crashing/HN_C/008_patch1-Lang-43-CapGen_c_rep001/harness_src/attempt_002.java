package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(24);
        String s2 = data.consumeString(24);
        String s3 = data.consumeAsciiString(24);
        String tail = data.consumeRemainingAsString();

        String[] atoms = new String[] {
            "",
            "'",
            "''",
            "'''",
            "''''",
            "a",
            "a'",
            "'a",
            "'a'",
            "a''",
            "''a",
            "{0}",
            "{0,number}",
            "{0,date}",
            "{0,time}",
            s1,
            s2,
            s3,
            tail
        };

        String[] patterns = new String[] {
            "'",
            "''",
            "'''",
            "a''",
            "'a''",
            "'a''b",
            "'a''b''",
            "'a" + "''",
            "'" + s1 + "''",
            "'" + s1 + "''" + s2,
            "'" + s1 + "''" + s2 + "''",
            "{0}'",
            "{0}''",
            "{0}'a''",
            "{0}'" + s1 + "''",
            "{0}" + "'" + s1 + "''" + s2,
            "x'" + s1 + "''",
            "x'" + s1 + "''" + s2,
            "'" + s1,
            "'" + s1 + "'",
            "'" + s1 + "''" + tail,
            s1 + "'" + s2 + "''",
            s1 + "''",
            s1 + "'" + s2,
            tail
        };

        Locale[] locales = new Locale[] {
            Locale.getDefault(),
            Locale.ROOT,
            Locale.US,
            Locale.UK,
            Locale.FRANCE,
            Locale.GERMANY,
            Locale.JAPAN,
            Locale.CHINA
        };
        Locale locale = locales[data.consumeInt(0, locales.length - 1)];
        Map registry = new HashMap();

        for (int i = 0; i < patterns.length; i++) {
            String p = patterns[i];

            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(p);
            emf1.toPattern();

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p, locale);
            emf2.toPattern();

            ExtendedMessageFormat emf3 = new ExtendedMessageFormat(p, locale, registry);
            emf3.toPattern();

            emf1.applyPattern(p);
            emf2.applyPattern(p);
            emf3.applyPattern(p);

            for (int j = 0; j < atoms.length; j++) {
                String a = atoms[j];

                String[] variants = new String[] {
                    p + a,
                    a + p,
                    "'" + p,
                    p + "'",
                    p + "''",
                    "'" + p + "''",
                    "{0}" + p,
                    p + "{0}",
                    "{0}" + "'" + p + "''",
                    "x'" + p + "''",
                    "'" + a + "''",
                    "'" + a + "''" + p
                };

                for (int k = 0; k < variants.length; k++) {
                    String v = variants[k];

                    if (data.consumeBoolean()) {
                        new ExtendedMessageFormat(v);
                    } else if (data.consumeBoolean()) {
                        new ExtendedMessageFormat(v, locale);
                    } else {
                        new ExtendedMessageFormat(v, locale, registry);
                    }

                    ExtendedMessageFormat emf = new ExtendedMessageFormat("");
                    emf.applyPattern(v);
                    emf.toPattern();
                }
            }
        }
    }
}