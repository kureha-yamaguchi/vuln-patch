package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static String mapToAlphabet(byte[] bytes, char[] alphabet) {
        char[] out = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            out[i] = alphabet[(bytes[i] & 0xFF) % alphabet.length];
        }
        return new String(out);
    }

    private static void exercise(String pattern, Map registry, Locale locale) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, locale, registry);
        emf.toPattern();
        emf.applyPattern(pattern);
        emf.toPattern();
    }

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Map registry = new HashMap();
        Locale locale = data.consumeBoolean() ? Locale.US : Locale.getDefault();

        String fuzz = data.consumeString(32);
        String ascii = data.consumeAsciiString(32);
        String rest = data.consumeRemainingAsString();

        char[] alphabet = new char[] { '\'', '{', '}', ',', '0', '1', 'a', ' ' };
        String core = mapToAlphabet(data.consumeBytes(data.consumeInt(0, 8)), alphabet);

        String[] quoteRuns = new String[] {
            "'",
            "''",
            "'''",
            "''''",
            "'''''",
            "''''''",
            "'''''''",
            "''''''''"
        };

        for (int i = 0; i < quoteRuns.length; i++) {
            String q = quoteRuns[i];
            exercise(q, registry, locale);
            exercise(q + "a", registry, locale);
            exercise("a" + q, registry, locale);
            exercise("a" + q + "a", registry, locale);
            exercise("{" + q + "}", registry, locale);
            exercise("{0," + q + "}", registry, locale);
            exercise("{0,number," + q + "}", registry, locale);
            exercise("{0," + q + ",x}", registry, locale);
            exercise("prefix " + q + " suffix", registry, locale);
        }

        String[] patterns = new String[] {
            core,
            "'" + core,
            core + "'",
            "'" + core + "'",
            "''" + core,
            core + "''",
            "'" + core + "''",
            "''" + core + "'",
            "{" + core + "}",
            "{0}",
            "{0," + core + "}",
            "{0,number," + core + "}",
            "{0,date," + core + "}",
            "{0,time," + core + "}",
            "{0," + core + ",x}",
            "{0,'" + core + "'}",
            "{0,''" + core + "}",
            "{0," + core + "''}",
            "{0," + core + "'" + "}",
            "{0," + "'" + core + "}",
            "a" + core + "b",
            "a'" + core,
            core + "'b",
            "a''" + core,
            core + "''b",
            "a'" + core + "'b",
            "a''" + core + "''b",
            fuzz,
            ascii,
            rest,
            "'" + fuzz,
            "'" + ascii,
            "'" + rest,
            fuzz + "'",
            ascii + "'",
            rest + "'",
            "'" + fuzz + "'",
            "'" + ascii + "'",
            "'" + rest + "'",
            "{0," + fuzz + "}",
            "{0," + ascii + "}",
            "{0," + rest + "}",
            "{0,number," + fuzz + "}",
            "{0,number," + ascii + "}",
            "{0,number," + rest + "}",
            "{0,'" + fuzz + "'}",
            "{0,'" + ascii + "'}",
            "{0,'" + rest + "'}",
            "{0,''}",
            "{0,'''}",
            "{0,''''}",
            "{0,''''' }",
            "{0,''' }",
            "{0,''a}",
            "{0,a''}",
            "{0,'a''}",
            "{0,''a''}",
            "{0,'''a}",
            "{0,a'''}",
            "{0,''''a}",
            "{0,a''''}",
            "{0,number,''}",
            "{0,number,'''}",
            "{0,number,''''}",
            "{0,number,'''a}",
            "{0,number,a'''}",
            "{0,number,''a}",
            "{0,number,a''}",
            "'''",
            "''''''",
            "a'''",
            "'''a",
            "a'''a",
            "''a''",
            "'{0}",
            "{0}'",
            "'{0}'",
            "{0,'}",
            "{0,''}",
            "{0,'''}",
            "{0,'''",
            "{0,''''",
            "{0,'''''",
            "{0,number,'''",
            "{0,number,''''",
            "{0,number,'''''"
        };

        for (int i = 0; i < patterns.length; i++) {
            exercise(patterns[i], registry, locale);
        }
    }
}