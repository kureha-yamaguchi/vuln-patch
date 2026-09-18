package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.ParsePosition;
import java.util.Locale;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeRemainingAsString();

        Locale[] locales = new Locale[] {
            Locale.ROOT,
            Locale.US,
            Locale.UK,
            Locale.FRANCE,
            Locale.GERMANY,
            Locale.JAPAN,
            Locale.CHINA
        };
        Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        String[] patterns = new String[] {
            s1,
            s2,
            s3,
            "",
            "'",
            "''",
            "'''",
            "''''",
            "{0}",
            "'{0}'",
            "''{0}''",
            s1 + "'" + s2,
            "'" + s1,
            s1 + "'",
            "'" + s1 + "'",
            "prefix '" + s1 + "' suffix",
            "{" + Math.abs(data.consumeInt()) + "}",
            "{" + Math.abs(data.consumeInt()) + "," + s2 + "}",
            s1 + "{" + Math.abs(data.consumeInt()) + "}" + s2,
            s1 + "''" + s2,
            s1 + "'" + s2 + "'" + s3,
            s1 + "{0," + s2 + "}",
            s1 + "{0," + s2 + "," + s3 + "}",
            "'" + s1 + "''" + s2 + "'",
            "a'b",
            "a''b",
            "a'''b",
            "{0} '" + s1,
            "'" + s1 + " {0} " + s2
        };

        Object[] args = new Object[] {
            null,
            s1,
            s2,
            s3,
            Integer.valueOf(data.consumeInt()),
            Byte.valueOf(data.consumeByte()),
            Boolean.valueOf(data.consumeBoolean())
        };

        for (String pattern : patterns) {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, locale);

            emf.toPattern();
            emf.hashCode();
            emf.equals(emf);

            String formatted = emf.format(args);

            ParsePosition pp = new ParsePosition(Math.max(0, Math.min(formatted.length(), Math.abs(data.consumeInt()))));
            emf.parseObject(formatted, pp);

            if (data.consumeBoolean()) {
                String alt = pattern + data.consumeAsciiString(16);
                emf.applyPattern(alt);
                emf.toPattern();
                emf.format(args);
            }

            if (data.consumeBoolean()) {
                String alt = "'" + pattern;
                emf.applyPattern(alt);
                emf.toPattern();
            }

            if (data.consumeBoolean()) {
                String alt = pattern + "'";
                emf.applyPattern(alt);
                emf.toPattern();
            }

            if (data.consumeBoolean()) {
                String alt = pattern.replace("{", "'{").replace("}", "}'");
                emf.applyPattern(alt);
                emf.toPattern();
            }
        }
    }
}