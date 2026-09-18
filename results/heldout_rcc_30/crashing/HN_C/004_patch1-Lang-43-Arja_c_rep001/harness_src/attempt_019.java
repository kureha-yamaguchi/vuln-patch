package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static String buildPattern(byte[] bytes, int mode) {
        char[] alphabet = new char[] {
            '\'', '{', '}', ',', ' ', 'a', 'b', 'c', '0', '1', ':'
        };
        StringBuilder sb = new StringBuilder();

        switch (mode % 8) {
            case 0:
                break;
            case 1:
                sb.append('\'');
                break;
            case 2:
                sb.append('{');
                break;
            case 3:
                sb.append("'{");
                break;
            case 4:
                sb.append("''");
                break;
            case 5:
                sb.append("{0,");
                break;
            case 6:
                sb.append("a");
                break;
            default:
                sb.append("'}");
                break;
        }

        for (int i = 0; i < bytes.length; i++) {
            int idx = bytes[i] & 0xFF;
            sb.append(alphabet[idx % alphabet.length]);
            if (((idx >>> 4) & 1) != 0) {
                sb.append('\'');
            }
            if (((idx >>> 5) & 1) != 0) {
                sb.append('{');
            }
            if (((idx >>> 6) & 1) != 0) {
                sb.append('}');
            }
        }

        switch (mode % 8) {
            case 0:
                sb.append('\'');
                break;
            case 1:
                sb.append("''");
                break;
            case 2:
                sb.append('}');
                break;
            case 3:
                sb.append("{0}");
                break;
            case 4:
                sb.append("',");
                break;
            case 5:
                sb.append("'");
                break;
            case 6:
                sb.append("}{");
                break;
            default:
                break;
        }

        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale[] locales = new Locale[] {
            Locale.ROOT, Locale.US, Locale.UK, Locale.FRANCE, Locale.GERMANY
        };

        Locale locale = locales[data.consumeInt(0, locales.length - 1)];
        boolean useRegistry = data.consumeBoolean();

        Map registry = null;
        if (useRegistry) {
            registry = new HashMap();
        }

        byte[] bytes1 = data.consumeBytes(32);
        byte[] bytes2 = data.consumeBytes(32);
        byte[] bytes3 = data.consumeRemainingAsBytes();

        String p1 = buildPattern(bytes1, data.consumeInt());
        String p2 = buildPattern(bytes2, data.consumeInt());
        String p3 = buildPattern(bytes3, data.consumeInt());

        String ascii = data.consumeAsciiString(16);
        String uni = data.consumeString(16);

        String[] patterns = new String[] {
            p1,
            p2,
            p3,
            p1 + p2,
            p2 + p3,
            p1 + "'" + p2,
            "'" + p1,
            p1 + "'",
            "'" + p1 + "'",
            p1 + "''" + p2,
            p1 + "{0}" + p2,
            p1 + "{0," + ascii + "}" + p2,
            p1 + "'" + uni,
            "'" + p1 + "{" + p2,
            p1 + "}" + "'" + p2 + "{",
            ascii,
            uni
        };

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];

            new ExtendedMessageFormat(pattern);
            new ExtendedMessageFormat(pattern, locale);
            new ExtendedMessageFormat(pattern, registry);
            new ExtendedMessageFormat(pattern, locale, registry);

            ExtendedMessageFormat emf = new ExtendedMessageFormat("", locale, registry);
            emf.applyPattern(pattern);
            emf.toPattern();

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, locale, registry);
            emf2.applyPattern(pattern + "'");
            emf2.toPattern();

            ExtendedMessageFormat emf3 = new ExtendedMessageFormat(pattern, locale, registry);
            emf3.applyPattern("'" + pattern);
            emf3.toPattern();

            ExtendedMessageFormat emf4 = new ExtendedMessageFormat(pattern, locale, registry);
            emf4.applyPattern(pattern + "''");
            emf4.toPattern();
        }
    }
}