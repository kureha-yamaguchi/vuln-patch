package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int anyInt = data.consumeInt();
        int smallInt = data.consumeInt(-3, 3);
        boolean useCtorPattern = data.consumeBoolean();
        boolean reapply = data.consumeBoolean();

        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        String s3 = data.consumeRemainingAsString();

        String idx = String.valueOf(Math.abs(smallInt));
        String anyIdx = String.valueOf(Math.abs(anyInt % 10));

        String[] patterns = new String[] {
            "",
            "'",
            "''",
            s1,
            s2,
            s3,
            "'" + s1,
            s1 + "'",
            "'" + s1 + "'",
            s1 + "''" + s2,
            "{" + idx + "}",
            "{0}",
            "{0," + s2 + "}",
            "{0," + s2 + "," + s3 + "}",
            "{0,'" + s2,
            "{0," + s2 + ",'" + s3 + "}",
            s1 + "{" + anyIdx + "}",
            s1 + "'" + s2,
            s1 + "{" + idx + "," + s2 + "}" + s3,
            s1 + "{" + idx + "," + s2 + "," + s3 + "}",
            s1 + "'" + s2 + "'" + s3,
            "'" + s1 + "{" + idx + "}" + s2,
            s1 + "{" + idx + "," + s2 + "}" + "'" + s3,
            s1 + "'" + s2 + "{" + idx + "}" + s3
        };

        java.util.Locale[] locales = new java.util.Locale[] {
            java.util.Locale.ROOT,
            java.util.Locale.US,
            java.util.Locale.UK,
            java.util.Locale.FRANCE,
            java.util.Locale.JAPAN
        };
        java.util.Locale locale = locales[Math.abs(anyInt) % locales.length];

        java.util.Map registry = new java.util.HashMap();

        String ctorPattern = patterns[Math.abs(anyInt) % patterns.length];
        String applyPattern = patterns[Math.abs(anyInt / 7) % patterns.length];

        ExtendedMessageFormat emf;
        if (useCtorPattern) {
            emf = new ExtendedMessageFormat(ctorPattern, locale, registry);
        } else {
            emf = new ExtendedMessageFormat("", locale, registry);
        }

        if (reapply) {
            emf.applyPattern(applyPattern);
        } else {
            emf.applyPattern(ctorPattern);
        }

        emf.toPattern();

        Object[] args = new Object[] {
            s1,
            s2,
            s3,
            Integer.valueOf(anyInt),
            Long.valueOf(anyInt),
            new java.util.Date((long) anyInt)
        };
        emf.format(args, new StringBuffer(), new java.text.FieldPosition(0));
        emf.parseObject(s1 + s2 + s3, new java.text.ParsePosition(Math.abs(smallInt)));
    }
}