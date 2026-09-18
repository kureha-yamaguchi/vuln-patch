package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static String fromInt(int value, int len, char[] alphabet) {
        char[] out = new char[len];
        for (int i = 0; i < len; i++) {
            out[i] = alphabet[value % alphabet.length];
            value /= alphabet.length;
        }
        return new String(out);
    }

    private static String mapBytes(byte[] bytes, char[] alphabet) {
        char[] out = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            out[i] = alphabet[(bytes[i] & 0xFF) % alphabet.length];
        }
        return new String(out);
    }

    private static void testPattern(String pattern, Locale locale, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, locale, registry);
        emf.toPattern();
        emf.applyPattern(pattern);
        emf.toPattern();
    }

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Locale locale = data.consumeBoolean() ? Locale.US : Locale.getDefault();
        Map registry = new HashMap();

        String s1 = data.consumeString(24);
        String s2 = data.consumeAsciiString(24);
        String s3 = data.consumeRemainingAsString();

        char[] tiny = new char[] { '\'', '{', '}', ',' };
        char[] rich = new char[] { '\'', '{', '}', ',', '0', '1', 'a', ' ', '#' };

        byte[] raw = data.consumeBytes(data.consumeInt(0, 12));
        String fuzzTiny = mapBytes(raw, tiny);
        String fuzzRich = mapBytes(raw, rich);

        for (int len = 0; len <= 6; len++) {
            int total = 1;
            for (int i = 0; i < len; i++) {
                total *= tiny.length;
            }
            for (int v = 0; v < total; v++) {
                String x = fromInt(v, len, tiny);

                testPattern(x, locale, registry);
                testPattern("'" + x, locale, registry);
                testPattern(x + "'", locale, registry);
                testPattern("'" + x + "'", locale, registry);
                testPattern("{" + x + "}", locale, registry);
                testPattern("{0," + x + "}", locale, registry);
                testPattern("{0,number," + x + "}", locale, registry);
                testPattern("{0,date," + x + "}", locale, registry);
                testPattern("{0,time," + x + "}", locale, registry);
                testPattern("{0,choice," + x + "}", locale, registry);
                testPattern("{0," + x, locale, registry);
                testPattern("{0,number," + x, locale, registry);
                testPattern("{0,date," + x, locale, registry);
                testPattern("{0,time," + x, locale, registry);
                testPattern("a{0," + x + "}b", locale, registry);
                testPattern("{0,a," + x + "}", locale, registry);
                testPattern("{0," + x + ",a}", locale, registry);
                testPattern("{0,'" + x + "'}", locale, registry);
                testPattern("{0,number,'" + x + "'}", locale, registry);
                testPattern("{0,date,'" + x + "'}", locale, registry);
                testPattern("{0,time,'" + x + "'}", locale, registry);
                testPattern("{0,choice,'" + x + "'}", locale, registry);
                testPattern("{0,''" + x + "}", locale, registry);
                testPattern("{0," + x + "''}", locale, registry);
                testPattern("{0,''" + x + "''}", locale, registry);
                testPattern("{0,number,''" + x + "}", locale, registry);
                testPattern("{0,number," + x + "''}", locale, registry);
                testPattern("{0,number,''" + x + "''}", locale, registry);
            }
        }

        String[] extras = new String[] {
            fuzzTiny,
            fuzzRich,
            s1,
            s2,
            s3,
            "'" + fuzzTiny,
            fuzzTiny + "'",
            "'" + fuzzTiny + "'",
            "{0," + fuzzTiny + "}",
            "{0,number," + fuzzTiny + "}",
            "{0,date," + fuzzTiny + "}",
            "{0,time," + fuzzTiny + "}",
            "{0,choice," + fuzzTiny + "}",
            "{0," + fuzzRich + "}",
            "{0,number," + fuzzRich + "}",
            "{0,date," + fuzzRich + "}",
            "{0,time," + fuzzRich + "}",
            "{0,choice," + fuzzRich + "}",
            "{0," + s1 + "}",
            "{0,number," + s1 + "}",
            "{0,date," + s1 + "}",
            "{0,time," + s1 + "}",
            "{0,choice," + s1 + "}",
            "{0,'" + s1 + "'}",
            "{0,number,'" + s1 + "'}",
            "{0,date,'" + s1 + "'}",
            "{0,time,'" + s1 + "'}",
            "{0,choice,'" + s1 + "'}",
            "{0," + s2 + "}",
            "{0,number," + s2 + "}",
            "{0,date," + s2 + "}",
            "{0,time," + s2 + "}",
            "{0,choice," + s2 + "}",
            "{0,'" + s2 + "'}",
            "{0,number,'" + s2 + "'}",
            "{0,date,'" + s2 + "'}",
            "{0,time,'" + s2 + "'}",
            "{0,choice,'" + s2 + "'}",
            "{0," + s3 + "}",
            "{0,number," + s3 + "}",
            "{0,date," + s3 + "}",
            "{0,time," + s3 + "}",
            "{0,choice," + s3 + "}",
            "{0,'" + s3 + "'}",
            "{0,number,'" + s3 + "'}",
            "{0,date,'" + s3 + "'}",
            "{0,time,'" + s3 + "'}",
            "{0,choice,'" + s3 + "'}",
            "{0,''}",
            "{0,'''}",
            "{0,''''}",
            "{0,''''' }",
            "{0,number,''}",
            "{0,number,'''}",
            "{0,number,''''}",
            "{0,date,''}",
            "{0,date,'''}",
            "{0,time,''}",
            "{0,time,'''}",
            "{0,choice,''}",
            "{0,choice,'''}",
            "{0,'}",
            "{0,number,'}",
            "{0,date,'}",
            "{0,time,'}",
            "{0,choice,'}",
            "{0,''",
            "{0,number,''",
            "{0,date,''",
            "{0,time,''",
            "{0,choice,''",
            "{0,'''",
            "{0,number,'''",
            "{0,date,'''",
            "{0,time,'''",
            "{0,choice,'''",
            "{0,'{'}",
            "{0,number,'{'}",
            "{0,date,'{'}",
            "{0,time,'{'}",
            "{0,choice,'{'}",
            "{0,'}'}",
            "{0,number,'}'}",
            "{0,date,'}'}",
            "{0,time,'}'}",
            "{0,choice,'}'}",
            "{0,'{'}'}",
            "{0,number,'{'}'}",
            "{0,'','}",
            "{0,number,'','}",
            "{0,''{''}",
            "{0,number,''{''}",
            "{0,'',}",
            "{0,number,'',}",
            "{0,choice,0#''}",
            "{0,choice,0#''' }",
            "{0,choice,0#'''1#x}",
            "{0,choice,0#'a''1#b}",
            "{0,choice,'0#x'}",
            "{0,choice,''0#x''}",
            "prefix {0,number,''} suffix",
            "prefix {0,date,'''} suffix",
            "prefix {0,time,'" + fuzzTiny + "'} suffix"
        };

        for (int i = 0; i < extras.length; i++) {
            testPattern(extras[i], locale, registry);
        }
    }
}