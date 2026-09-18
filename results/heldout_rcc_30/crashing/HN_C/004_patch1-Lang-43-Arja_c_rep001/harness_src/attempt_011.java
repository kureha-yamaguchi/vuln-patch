package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeString(64);
        String s4 = data.consumeAsciiString(64);

        int n1 = data.consumeInt();
        int n2 = data.consumeInt(-3, 3);
        boolean b1 = data.consumeBoolean();
        boolean b2 = data.consumeBoolean();

        Locale locale = chooseLocale(data);
        Map registry = b1 ? null : new HashMap();

        String[] patterns = new String[] {
            s1,
            "'" + s1,
            s1 + "'",
            "''",
            "'''",
            "'" + s2 + "'",
            "'" + s2 + "''" + s3 + "'",
            s2 + "'" + s3,
            s2 + "''" + s3,
            "{0}",
            "{0," + s2 + "}",
            "{0," + s2 + "," + s3 + "}",
            "'" + "{0}" + "'",
            "{0} '" + s2,
            s2 + "{0}" + s3,
            "{" + Math.abs(n1 % 10) + "}",
            "{" + Math.abs(n1 % 10) + "," + s2 + "}",
            "{" + Math.abs(n1 % 10) + "," + s2 + "," + s3 + "}",
            buildPattern(s1, s2, s3, s4, n1, n2, b2)
        };

        Object[] formatArgs = new Object[] {
            s1,
            s2,
            s3,
            Integer.valueOf(n1),
            Long.valueOf(n1),
            Double.valueOf(n2),
            Boolean.valueOf(b2),
            null
        };

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];

            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern);
            emf1.toPattern();
            emf1.format(formatArgs, new StringBuffer(), new FieldPosition(0));

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, locale);
            emf2.toPattern();
            emf2.format(formatArgs, new StringBuffer(), new FieldPosition(Math.abs(n2)));

            ExtendedMessageFormat emf3 = new ExtendedMessageFormat(pattern, locale, registry);
            emf3.toPattern();
            emf3.format(formatArgs, new StringBuffer(), new FieldPosition(Math.abs(n1 % 5)));

            String nextPattern = patterns[(i + 1) % patterns.length];
            emf3.applyPattern(nextPattern);
            emf3.toPattern();
            emf3.format(formatArgs, new StringBuffer(), new FieldPosition(0));
        }
    }

    private static Locale chooseLocale(FuzzedDataProvider data) {
        Locale[] locales = new Locale[] {
            Locale.ROOT,
            Locale.US,
            Locale.UK,
            Locale.FRANCE,
            Locale.GERMANY,
            Locale.JAPAN,
            new Locale("tr"),
            new Locale("ar"),
            new Locale("hi", "IN")
        };
        return locales[data.consumeInt(0, locales.length - 1)];
    }

    private static String buildPattern(String a, String b, String c, String d, int n1, int n2, boolean flip) {
        StringBuilder sb = new StringBuilder();
        if (flip) {
            sb.append('\'');
        }
        sb.append(a);
        if ((n1 & 1) == 0) {
            sb.append("''");
        } else {
            sb.append('\'');
        }
        sb.append('{').append(Math.abs(n1 % 10));
        if ((n1 & 2) != 0) {
            sb.append(',').append(b);
        }
        if ((n1 & 4) != 0) {
            sb.append(',').append(c);
        }
        sb.append('}');
        if ((n2 & 1) != 0) {
            sb.append('\'');
        }
        sb.append(d);
        return sb.toString();
    }
}