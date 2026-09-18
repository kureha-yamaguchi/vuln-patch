package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Locale;

public class FuzzHarness {
    private static String mapBytes(byte[] in) {
        char[] alphabet = new char[] {
            '\'', '\'', '{', '}', ',', '0', '1', '2', 'a', 'b', ' ', ':'
        };
        StringBuffer sb = new StringBuffer(in.length);
        for (int i = 0; i < in.length; i++) {
            sb.append(alphabet[(in[i] & 0xFF) % alphabet.length]);
        }
        return sb.toString();
    }

    private static void testPattern(String pattern, Locale locale) {
        ExtendedMessageFormat emf;

        emf = new ExtendedMessageFormat("");
        emf.applyPattern(pattern);
        emf.toPattern();

        emf = new ExtendedMessageFormat("", locale);
        emf.applyPattern(pattern);
        emf.toPattern();

        emf = new ExtendedMessageFormat("", locale, Collections.EMPTY_MAP);
        emf.applyPattern(pattern);
        emf.toPattern();

        new ExtendedMessageFormat(pattern);
        new ExtendedMessageFormat(pattern, locale);
        new ExtendedMessageFormat(pattern, locale, Collections.EMPTY_MAP).toPattern();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale[] locales = new Locale[] {
            Locale.ROOT, Locale.US, Locale.UK, Locale.FRANCE, Locale.GERMANY, Locale.JAPAN
        };
        Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        String s1 = data.consumeString(16);
        String s2 = data.consumeAsciiString(16);
        String s3 = mapBytes(data.consumeBytes(24));
        String rest = mapBytes(data.consumeRemainingAsBytes());

        String[] seeds = new String[] {
            "",
            "'",
            "''",
            "'''",
            "''''",
            s1,
            s2,
            s3,
            rest,
            "'" + s1,
            s1 + "'",
            "''" + s1,
            s1 + "''",
            "'" + s3,
            s3 + "'",
            "''" + s3,
            s3 + "''",
            "'" + s3 + "'",
            "'" + s3 + "''",
            "''" + s3 + "'",
            s1 + s3 + s2,
            s3 + rest
        };

        String[] patterns = new String[] {
            seeds[0],
            seeds[1],
            seeds[2],
            seeds[3],
            seeds[4],

            "{0}",
            "{0,number}",
            "{0,date}",
            "{0,time}",

            "{0,'" + s3 + "}",
            "{0,''}",
            "{0,'''}",
            "{0,''''}",
            "{0,'''''}",
            "{0,'" + s1 + "''}",
            "{0,''" + s1 + "}",
            "{0," + s3 + "}",
            "{0," + s3 + "," + s1 + "}",
            "{0,number,'" + s3 + "}",
            "{0,number,''}",
            "{0,number,'''}",
            "{0,number,''''}",
            "{0,number,'''''}",
            "{0,number," + s3 + "}",
            "{0,number,'" + s3 + "'}",
            "{0,number,'" + s3 + "''}",
            "{0,number,''" + s3 + "}",
            "{0,date,'" + s3 + "}",
            "{0,time,'" + s3 + "}",

            "a{0,'" + s3 + "}b",
            "a{0,''}b",
            "a{0,'''}b",
            "a{0,''''}b",
            "a{0,'''''}b",
            "a{0,number,'" + s3 + "}b",
            "a{0,number,''}b",
            "a{0,number,'''}b",
            "a{0,number,''''}b",
            "a{0,number,'''''}b",

            "{0," + seeds[data.consumeInt(0, seeds.length - 1)] + "}",
            "{0," + seeds[data.consumeInt(0, seeds.length - 1)] + "," + seeds[data.consumeInt(0, seeds.length - 1)] + "}",
            "{0,number," + seeds[data.consumeInt(0, seeds.length - 1)] + "}",
            "{0,date," + seeds[data.consumeInt(0, seeds.length - 1)] + "}",
            "{0,time," + seeds[data.consumeInt(0, seeds.length - 1)] + "}",

            seeds[data.consumeInt(0, seeds.length - 1)] + "{0,'" + s3 + "}",
            seeds[data.consumeInt(0, seeds.length - 1)] + "{0,number,'" + s3 + "}",
            seeds[data.consumeInt(0, seeds.length - 1)] + "{0,''}",
            seeds[data.consumeInt(0, seeds.length - 1)] + "{0,number,''}",
            seeds[data.consumeInt(0, seeds.length - 1)] + "{0,''''}",
            seeds[data.consumeInt(0, seeds.length - 1)] + "{0,number,''''}",

            "{0,'" + rest + "}",
            "{0,number,'" + rest + "}",
            "{0,''" + rest + "}",
            "{0,number,''" + rest + "}",
            "{0,'" + rest + "''}",
            "{0,number,'" + rest + "''}"
        };

        for (int i = 0; i < patterns.length; i++) {
            testPattern(patterns[i], locale);
        }
    }
}