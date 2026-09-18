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

        String a = data.consumeString(24);
        String b = data.consumeAsciiString(24);
        String c = data.consumeRemainingAsString();

        String idx0 = String.valueOf(Math.abs(data.consumeInt(-2, 2)));
        String idx1 = String.valueOf(Math.floorMod(data.consumeInt(), 10));
        boolean ctorAlso = data.consumeBoolean();

        String[] patterns = new String[] {
            "{0,x,''}",
            "{0,x,'''}",
            "{0,x,''''}",
            "{0,x,'''''}",
            "{0,x,''a}",
            "{0,x,a''}",
            "{0,x,'a''b'}",
            "{0,x,''" + b + "}",
            "{0,x," + b + "''}",
            "{0,x,''" + b + "''}",
            "{0,x,'" + b + "''}",
            "{0,x,''" + b + "'}",
            "{0,x,'" + b + "'}",
            "{0,x,''" + b + "''" + c + "}",
            "{0,x," + a + "''" + b + "}",
            "{0,x," + a + "'" + b + "}",
            "{0,x," + a + "{" + idx0 + "}''}",
            "{0,x,''{" + idx0 + "}}",
            "{0,x,''{" + idx0 + "}" + b + "}",
            "{0,x,'{" + idx0 + "}" + b + "'}",
            "{0,x,{" + idx0 + "}}",
            "{0,x,{" + idx0 + "}" + b + "}",
            "{0,x,''''{" + idx0 + "}}",
            "{0,x,''''}",
            "{0,x,''''" + b + "}",
            "{0,x,''''" + b + "''}",
            "{0,x,''''" + b + "'" + c + "}",
            "{0,x," + b + "}",
            "{0,x," + b + "," + c + "}",
            "{0,x,'" + c + "}",
            "{0,x,''" + c + "}",
            "{0,x,''}" + a,
            a + "{0,x,''}",
            a + "{0,x,'''}" + b,
            a + "{0,x,''''}" + b,
            a + "{0,x,''" + b + "}" + c,
            "{" + idx1 + ",x,''}",
            "{" + idx1 + ",x,'''}",
            "{" + idx1 + ",x,''''}",
            "{" + idx1 + ",x,'" + b + "''}",
            "{" + idx1 + ",x,''" + b + "''}",
            "{" + idx1 + ",x,''" + b + "'}",
            "{" + idx1 + ",x,''{" + idx0 + "}}",
            "{" + idx1 + ",x,{" + idx0 + "}}",
            "{" + idx1 + ",x," + a + "''" + b + "}",
            "{" + idx1 + ",x," + a + "'" + b + "}"
        };

        if (ctorAlso) {
            try {
                new ExtendedMessageFormat(patterns[data.consumeInt(0, patterns.length - 1)], locale, registry);
            } catch (IllegalArgumentException ignored) {
            }
        }

        for (int i = 0; i < patterns.length; i++) {
            ExtendedMessageFormat emf = new ExtendedMessageFormat("", locale, registry);
            try {
                emf.applyPattern(patterns[i]);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}