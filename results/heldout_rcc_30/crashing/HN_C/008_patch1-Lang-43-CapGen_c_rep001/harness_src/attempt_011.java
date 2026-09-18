package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        java.util.Locale[] locales = new java.util.Locale[] {
            java.util.Locale.ROOT,
            java.util.Locale.US,
            java.util.Locale.UK,
            java.util.Locale.ENGLISH,
            java.util.Locale.FRANCE,
            java.util.Locale.GERMANY,
            java.util.Locale.JAPAN,
            java.util.Locale.CHINA,
            java.util.Locale.CANADA,
            java.util.Locale.getDefault()
        };
        java.util.Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        String a = data.consumeString(data.consumeInt(0, 24));
        String b = data.consumeAsciiString(data.consumeInt(0, 24));
        String c = data.consumeRemainingAsString();

        java.util.Map registry = data.consumeBoolean()
                ? java.util.Collections.EMPTY_MAP
                : new java.util.HashMap();

        String[] atoms = new String[] {
            "",
            "'",
            "''",
            "'''",
            "''''",
            "'''''",
            a,
            b,
            c,
            a + "'",
            "'" + a,
            "'" + a + "'",
            a + "''",
            "''" + a,
            "'" + b + "''",
            "''" + b + "'",
            c + "'",
            "'" + c
        };

        String[] patterns = new String[] {
            "'",
            "''",
            "'''",
            "''''",
            "'''''",
            "'a",
            "'a'",
            "'a''",
            "'a'''",
            "''a",
            "''a'",
            "'''a",
            "'''a'",
            "''''a",
            "a'",
            "a''",
            "a'''",
            "a''''",
            "{0}",
            "'{0}",
            "'{0}'",
            "'{0}''",
            "''{0}",
            "''{0}'",
            "'''{0}",
            "{0}'",
            "{0}''",
            "{0}'''",
            "{0,number}",
            "{0,number,'",
            "{0,number,''",
            "{0,number,'''",
            "{0,number,'a",
            "{0,number,'a'}",
            "{0,number,'a''",
            "{0,date,'",
            "{0,time,''",
            "prefix '",
            "prefix ''",
            "prefix '''",
            "prefix 'suffix",
            "prefix ''suffix",
            "prefix '''suffix",
            "prefix '" + a,
            "prefix ''" + a,
            "prefix '''" + a,
            "'" + a,
            "'" + a + "'",
            "'" + a + "''",
            "'" + a + "'''",
            "''" + a,
            "'''" + a,
            "'" + b,
            "'" + b + "''",
            "'" + c,
            "'" + c + "''",
            a + "'",
            a + "''",
            a + "'''",
            b + "'",
            b + "''",
            c + "'",
            c + "''",
            "'" + a + b,
            "'" + a + b + "'",
            "'" + a + b + "''",
            "'" + a + "''" + b,
            "'" + a + "''" + b + "'",
            "'" + a + "''" + b + "''",
            a + "'" + b,
            a + "''" + b,
            a + "'''" + b,
            "'" + a + "'{" + b,
            "'" + a + "''{" + b,
            "'" + a + "''",
            "'" + a + "''''",
            "'" + a + "''''''",
            "'" + b + "''''",
            "'" + c + "''''"
        };

        for (int i = 0; i < patterns.length; i++) {
            String p = patterns[i];
            new ExtendedMessageFormat(p);
            new ExtendedMessageFormat(p, locale);
            new ExtendedMessageFormat(p, registry);
            new ExtendedMessageFormat(p, locale, registry);
        }

        for (int i = 0; i < atoms.length; i++) {
            for (int j = 0; j < atoms.length; j++) {
                String x = atoms[i];
                String y = atoms[j];

                String[] generated = new String[] {
                    "'" + x,
                    "'" + x + "'",
                    "'" + x + "''",
                    "'" + x + "'''",
                    "'" + x + "''''",
                    "'" + x + y,
                    "'" + x + "''" + y,
                    "'" + x + "'''" + y,
                    x + "'",
                    x + "''",
                    x + "'''",
                    x + "'" + y,
                    x + "''" + y,
                    x + "'''" + y,
                    "{" + x,
                    "{0," + x,
                    "{0," + x + "}",
                    "{0,'" + x,
                    "{0,'" + x + "'",
                    "{0,'" + x + "''",
                    "{0,'" + x + "'''}",
                    "{0,number,'" + x,
                    "{0,number,'" + x + "'",
                    "{0,number,'" + x + "''",
                    "{0,number,'" + x + "'''}",
                    "prefix '" + x,
                    "prefix '" + x + "'",
                    "prefix '" + x + "''",
                    "prefix '" + x + "'''" + y
                };

                for (int k = 0; k < generated.length; k++) {
                    String p = generated[k];

                    ExtendedMessageFormat emf = new ExtendedMessageFormat("");
                    emf.applyPattern(p);
                    emf.toPattern();

                    ExtendedMessageFormat emfLocale = new ExtendedMessageFormat("", locale);
                    emfLocale.applyPattern(p);
                    emfLocale.toPattern();

                    ExtendedMessageFormat emfReg = new ExtendedMessageFormat("", registry);
                    emfReg.applyPattern(p);
                    emfReg.toPattern();

                    ExtendedMessageFormat emfBoth = new ExtendedMessageFormat("", locale, registry);
                    emfBoth.applyPattern(p);
                    emfBoth.toPattern();
                }
            }
        }
    }
}