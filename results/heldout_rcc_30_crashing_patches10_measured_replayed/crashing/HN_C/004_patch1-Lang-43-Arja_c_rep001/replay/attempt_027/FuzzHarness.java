package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Locale;

public class FuzzHarness {
    private static String mapToPatternAlphabet(byte[] bytes) {
        char[] alphabet = new char[] {
            '{', '}', '\'', ',', '0', '1', '2', 'a', 'b', 'c', ' ', '#'
        };
        StringBuffer sb = new StringBuffer(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            sb.append(alphabet[(bytes[i] & 0xFF) % alphabet.length]);
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale[] locales = Locale.getAvailableLocales();
        Locale locale = locales.length == 0 ? Locale.ROOT : locales[data.consumeInt(0, locales.length - 1)];

        byte[] rawBytes = data.consumeBytes(data.consumeInt(0, 64));
        String raw = mapToPatternAlphabet(rawBytes);
        String s1 = data.consumeString(16);
        String s2 = data.consumeAsciiString(16);
        String tail = data.consumeRemainingAsString();

        String q1 = s1.replace("'", "''");
        String q2 = s2.replace("'", "''");
        String qRaw = raw.replace("'", "''");
        String qTail = tail.replace("'", "''");

        String[] patterns = new String[] {
            raw,
            "'",
            "''",
            "'''",
            raw + "'",
            "'" + raw,
            "'" + raw + "'",
            "'" + raw + "''",
            "{",
            "}",
            "{}",
            "{0}",
            "{0,}",
            "{0,'}",
            "{0,''}",
            "{0,'''}}",
            "{0," + raw,
            "{0," + raw + "}",
            "{0,'" + raw,
            "{0,'" + raw + "'",
            "{0,'" + raw + "'}",
            "{0,''" + raw + "}",
            "{0," + raw + "''}",
            "{0," + raw + "'}",
            "{0," + raw + "''' }",
            "{0," + qRaw + "}",
            "{0," + qRaw + "'" + "}",
            "{0," + qRaw + "''" + "}",
            "{0," + qRaw + "'''" + "}",
            "{0," + qRaw + "'" + q1 + "}",
            "{0," + qRaw + "''" + q1 + "}",
            "{0,'" + q1 + "''" + q2 + "'}",
            "{0," + s1 + ",'" + q1 + "''" + q2 + "'}",
            "{0," + raw + ",a}",
            "{0," + raw + ",'" + q1 + "'}",
            "{0," + raw + ",'" + q1 + "''" + q2 + "'}",
            "{0," + raw + "'" + q1 + "''" + q2 + "'}",
            "{0," + raw + "'" + q1 + "''" + q2,
            "{0," + raw + "{1}}",
            "{0," + raw + "{1,'" + q1 + "'}}",
            "{0," + raw + "'{1}" + "}",
            "{0," + raw + "''{1}" + "}",
            s1 + "{0," + raw + "}" + tail,
            s1 + "{0,'" + q1 + "''" + q2 + "'}" + tail,
            "{0," + qTail + "'" + "}",
            "{0," + qTail + "''" + "}",
            "{0,'" + qTail + "}",
            "{0,'" + qTail + "'}",
            "{0,'" + qTail + "''}",
            "{{0," + raw + "}}"
        };

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];

            ExtendedMessageFormat emf1 = new ExtendedMessageFormat("");
            emf1.applyPattern(pattern);
            emf1.toPattern();

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, locale);
            emf2.toPattern();

            ExtendedMessageFormat emf3 = new ExtendedMessageFormat(pattern, locale, Collections.EMPTY_MAP);
            emf3.toPattern();

            ExtendedMessageFormat emf4 = new ExtendedMessageFormat(pattern, Collections.EMPTY_MAP);
            emf4.toPattern();
        }
    }
}