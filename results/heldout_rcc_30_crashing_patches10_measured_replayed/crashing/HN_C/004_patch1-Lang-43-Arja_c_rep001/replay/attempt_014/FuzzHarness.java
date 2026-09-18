package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Locale;

public class FuzzHarness {
    private static String synthesizePattern(byte[] bytes) {
        char[] alphabet = new char[] { '\'', '{', '}', ',', '0', '1', 'a', 'A', ' ' };
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(alphabet[(bytes[i] & 0xFF) % alphabet.length]);
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale[] locales = new Locale[] {
            Locale.ROOT, Locale.US, Locale.UK, Locale.FRANCE, Locale.GERMANY
        };
        Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        String a = data.consumeString(24);
        String b = data.consumeAsciiString(24);
        byte[] raw = data.consumeBytes(32);
        String q = synthesizePattern(raw);

        int n1 = data.consumeInt(-2, 3);
        int n2 = data.consumeInt(-2, 3);

        String[] patterns = new String[] {
            q,
            "'" + q,
            q + "'",
            "'" + q + "'",
            "''" + q,
            q + "''",
            "'" + q + "''",
            "''" + q + "'",
            a + q + b,
            q + "{0}",
            "{0}" + q,
            "'" + q + "{0}",
            "'" + q + "''{0}",
            "{0,'" + q,
            "{0,'" + q + "'}",
            "{0,'" + q + "''}",
            "{0," + q + "}",
            "{0," + q + "," + a + "}",
            "{" + n1 + "}",
            "{" + n1 + "," + q + "}",
            "{" + n1 + "," + q + "," + b + "}",
            "'" + a + "''" + b,
            "'" + a + "''" + b + "'",
            "'" + a + "''" + b + "''",
            "pre'" + q + "post",
            "pre''" + q + "post",
            "{" + n2 + ",'" + q,
            "{" + n2 + ",'" + q + "'}",
            "{" + n2 + ",'" + q + "''}",
            ""
        };

        for (int i = 0; i < patterns.length; i++) {
            String p = patterns[i];

            ExtendedMessageFormat emf = new ExtendedMessageFormat("");
            emf.applyPattern(p);
            emf.toPattern();

            ExtendedMessageFormat emfLocale = new ExtendedMessageFormat("", locale);
            emfLocale.applyPattern(p);
            emfLocale.toPattern();

            ExtendedMessageFormat emfRegistry = new ExtendedMessageFormat("", locale, Collections.EMPTY_MAP);
            emfRegistry.applyPattern(p);
            emfRegistry.toPattern();

            new ExtendedMessageFormat(p);
            new ExtendedMessageFormat(p, locale);
            new ExtendedMessageFormat(p, locale, Collections.EMPTY_MAP);
        }
    }
}