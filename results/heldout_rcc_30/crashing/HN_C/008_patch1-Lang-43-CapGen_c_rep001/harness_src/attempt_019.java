package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        java.util.Locale[] locales = new java.util.Locale[] {
            java.util.Locale.ROOT,
            java.util.Locale.US,
            java.util.Locale.UK,
            java.util.Locale.FRANCE,
            java.util.Locale.JAPAN
        };
        java.util.Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        java.util.Map registry = new java.util.HashMap();

        String s1 = data.consumeString(16);
        String s2 = data.consumeAsciiString(16);
        String s3 = data.consumeRemainingAsString();

        String[] seeds = new String[] {
            "{0,x,''}",
            "{0,x,'''}",
            "{0,x,''''}",
            "{0,x,'''''}",
            "{0,x,''''''}",
            "{0,x,''a}",
            "{0,x,a''}",
            "{0,x,'a''b'}",
            "{0,x,''a''}",
            "{0,x,''" + s1 + "}",
            "{0,x,''" + s1 + "''}",
            "{0,x,''" + s1 + "''' }",
            "{0,x,''" + s2 + "}",
            "{0,x,''" + s2 + "''}",
            "{0,x,''" + s3 + "}",
            "{0,x,''" + s1 + s2 + "}",
            "{0,x," + s1 + "''}",
            "{0,x," + s2 + "''}",
            "{0,x," + s1 + "'" + s2 + "}",
            "{0,x," + s1 + "''" + s2 + "}",
            "{0,x,''{" + data.consumeInt(0, 3) + "}}",
            "{0,x,''{" + data.consumeInt(0, 3) + "}" + s1 + "}",
            "{0,x,'{" + data.consumeInt(0, 3) + "}" + s2 + "'}",
            "{0,x,''}",
            "{0,x,''" + "}",
            "{0,x,''" + s1 + "}",
            "{0,x,''" + s1,
            "{0,x,''" + s1 + "''",
            "{0,x,''" + s1 + "'",
            "{0,x,'" + s1 + "''}",
            "{0,x,'" + s1 + "}",
            "{0,x,''" + s1 + "," + s2 + "}",
            "{0,x,''" + s1 + "}" + s2,
            "prefix{0,x,''}suffix",
            "prefix{0,x,''" + s1 + "}suffix",
            "prefix{0,x," + s1 + "''}suffix",
            "{" + data.consumeInt(0, 9) + ",x,''}",
            "{" + data.consumeInt(0, 9) + ",x,'''}",
            "{" + data.consumeInt(0, 9) + ",x,''''}",
            "{" + data.consumeInt(0, 9) + ",x,''" + s1 + "}",
            "{" + data.consumeInt(0, 9) + ",x," + s1 + "''}",
            s1 + "{0,x,''}" + s2,
            s1 + "{0,x,''" + s2 + "}" + s3,
            s1 + "{0,x," + s2 + "''}" + s3
        };

        ExtendedMessageFormat emf = new ExtendedMessageFormat("", locale, registry);

        for (int i = 0; i < seeds.length; i++) {
            emf.applyPattern(seeds[i]);
            emf.toPattern();
        }

        new ExtendedMessageFormat(seeds[data.consumeInt(0, seeds.length - 1)], locale, registry);
    }
}