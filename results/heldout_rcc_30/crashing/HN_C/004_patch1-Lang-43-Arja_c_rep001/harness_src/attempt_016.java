package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Locale;

public class FuzzHarness {
    private static String quoteHeavy(String s) {
        StringBuffer sb = new StringBuffer();
        sb.append('\'');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (i % 6) {
                case 0:
                    sb.append(ch).append('\'').append('\'');
                    break;
                case 1:
                    sb.append('\'').append(ch);
                    break;
                case 2:
                    sb.append(ch);
                    break;
                case 3:
                    sb.append('\'').append('\'').append(ch);
                    break;
                case 4:
                    sb.append(ch).append('\'');
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }
        return sb.toString();
    }

    private static String fromBytes(byte[] bytes) {
        char[] alphabet = new char[] { '\'', '{', '}', ',', 'a', 'b', '0', '1', ' ', ':' };
        StringBuffer sb = new StringBuffer(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            sb.append(alphabet[(bytes[i] & 0xFF) % alphabet.length]);
        }
        return sb.toString();
    }

    private static void runPattern(String pattern, Locale locale) {
        new ExtendedMessageFormat(pattern, locale, Collections.EMPTY_MAP);
        ExtendedMessageFormat emf = new ExtendedMessageFormat("", locale, Collections.EMPTY_MAP);
        emf.applyPattern(pattern);
        emf.toPattern();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale[] locales = new Locale[] {
            Locale.ROOT, Locale.US, Locale.UK, Locale.FRANCE, Locale.GERMANY
        };
        Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        String s1 = data.consumeString(20);
        String s2 = data.consumeAsciiString(20);
        String s3 = fromBytes(data.consumeBytes(20));
        String tail = fromBytes(data.consumeRemainingAsBytes());

        String q1 = quoteHeavy(s1);
        String q2 = quoteHeavy(s2);
        String q3 = quoteHeavy(s3);
        String q4 = quoteHeavy(tail);

        String[] descs = new String[] {
            "'",
            "''",
            "'''",
            "''''",
            q1,
            q2,
            q3,
            q4,
            q1 + "''",
            q2 + "''",
            q3 + "''",
            q4 + "''",
            q1 + "'x",
            q2 + "'x",
            q3 + "'x",
            q4 + "'x",
            "'a''b",
            "'a''",
            "'a''b''c",
            "''," + q1,
            q1 + "," + q2,
            q3 + "," + q4
        };

        String[] patterns = new String[] {
            "{0," + descs[0] + "}",
            "{0," + descs[1] + "}",
            "{0," + descs[2] + "}",
            "{0," + descs[3] + "}",
            "{0," + descs[4] + "}",
            "{0," + descs[5] + "}",
            "{0," + descs[6] + "}",
            "{0," + descs[7] + "}",
            "{0," + descs[8] + "}",
            "{0," + descs[9] + "}",
            "{0," + descs[10] + "}",
            "{0," + descs[11] + "}",
            "{0," + descs[12] + "}",
            "{0," + descs[13] + "}",
            "{0," + descs[14] + "}",
            "{0," + descs[15] + "}",
            "{0," + descs[16] + "}",
            "{0," + descs[17] + "}",
            "{0," + descs[18] + "}",
            "{0," + descs[19] + "}",
            "{0," + descs[20] + "}",
            "{0," + descs[21] + "}",

            "x{0," + descs[data.consumeInt(0, descs.length - 1)] + "}y",
            "x{0," + descs[data.consumeInt(0, descs.length - 1)] + "}''",
            "''{0," + descs[data.consumeInt(0, descs.length - 1)] + "}z",
            "{0,number," + descs[data.consumeInt(0, descs.length - 1)] + "}",
            "{0,date," + descs[data.consumeInt(0, descs.length - 1)] + "}",
            "{0,time," + descs[data.consumeInt(0, descs.length - 1)] + "}",
            "{0," + descs[data.consumeInt(0, descs.length - 1)] + "," + q1 + "}",
            "{0," + descs[data.consumeInt(0, descs.length - 1)] + "," + q2 + "}",
            "{0," + descs[data.consumeInt(0, descs.length - 1)] + "," + q3 + "}",
            "{0," + descs[data.consumeInt(0, descs.length - 1)] + "," + q4 + "}"
        };

        for (int i = 0; i < patterns.length; i++) {
            runPattern(patterns[i], locale);
        }
    }
}