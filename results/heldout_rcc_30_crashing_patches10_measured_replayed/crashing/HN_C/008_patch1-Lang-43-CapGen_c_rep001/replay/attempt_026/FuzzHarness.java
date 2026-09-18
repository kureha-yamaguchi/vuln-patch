package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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
            java.util.Locale.ITALY
        };
        java.util.Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeRemainingAsString();

        String[] patterns = new String[] {
            s1,
            s2,
            s3,
            "'" + s1,
            s1 + "'",
            "'" + s1 + "'",
            "''" + s1,
            s1 + "''",
            s1 + "'" + s2,
            s1 + "{" + s2 + "}",
            "{" + s1 + "}",
            "{0}",
            "{0,'" + s1 + "'}",
            "prefix '" + s1 + "' suffix",
            "prefix ''" + s1 + "'' suffix",
            s1 + " {" + data.consumeInt() + "}",
            s1 + " {" + data.consumeInt() + ",number}",
            s1 + " {" + data.consumeInt() + ",date}",
            s1 + " {" + data.consumeInt() + ",time}",
            s1 + " {" + data.consumeInt() + ",choice," + s2 + "}",
            s1 + " '" + s2 + "' " + s3,
            "'" + s1 + "''" + s2 + "'",
            "{" + data.consumeInt() + "," + s2 + "," + s3 + "}",
            ""
        };

        java.util.Map registry = data.consumeBoolean() ? null : java.util.Collections.emptyMap();

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];

            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern);
            emf1.toPattern();
            emf1.format(new Object[] { s1, s2, s3, Integer.valueOf(data.consumeInt()) }, new StringBuffer(), new java.text.FieldPosition(0));

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, locale);
            emf2.toPattern();
            emf2.format(new Object[] { s3, s2, s1, Integer.valueOf(data.consumeInt()) }, new StringBuffer(), new java.text.FieldPosition(0));

            ExtendedMessageFormat emf3 = new ExtendedMessageFormat(pattern, locale, registry);
            emf3.toPattern();
            emf3.format(new Object[] { s2, s1, s3, Integer.valueOf(data.consumeInt()) }, new StringBuffer(), new java.text.FieldPosition(0));

            emf1.applyPattern(pattern);
            emf2.applyPattern(pattern);
            emf3.applyPattern(pattern);

            emf1.equals(emf2);
            emf2.equals(emf3);
            emf1.hashCode();
            emf2.hashCode();
            emf3.hashCode();
        }
    }
}