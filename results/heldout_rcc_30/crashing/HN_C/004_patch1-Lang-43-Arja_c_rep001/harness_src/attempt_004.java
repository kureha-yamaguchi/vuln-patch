package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int localeChoice = data.consumeInt(0, 5);
        Locale locale;
        switch (localeChoice) {
            case 0:
                locale = Locale.ROOT;
                break;
            case 1:
                locale = Locale.US;
                break;
            case 2:
                locale = Locale.JAPAN;
                break;
            case 3:
                locale = Locale.GERMANY;
                break;
            case 4:
                locale = Locale.CHINA;
                break;
            default:
                locale = Locale.getDefault();
                break;
        }

        int argIndex = data.consumeInt(0, 9);
        boolean useNullRegistry = data.consumeBoolean();
        Map registry = useNullRegistry ? null : new HashMap();

        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeString(32);
        String s4 = data.consumeRemainingAsString();

        String quotedHeavy = "'" + s1 + "''" + s2 + "'";
        String braceHeavy = "{" + argIndex + "," + s2 + "," + s3 + "}";
        String[] patterns = new String[] {
            "",
            "'",
            "''",
            "'''",
            "{0}",
            "'{0}'",
            s1,
            s2,
            s1 + s2,
            "'" + s1,
            s1 + "'",
            "''" + s1,
            s1 + "''",
            quotedHeavy,
            quotedHeavy + s3,
            s1 + "'{" + argIndex + "}'" + s2,
            s1 + "{" + argIndex + "}" + s2,
            s1 + "{" + argIndex + "," + s2 + "}" + s3,
            braceHeavy,
            "'" + braceHeavy,
            braceHeavy + "'",
            s1 + s4,
            "'" + s1 + s4,
            s1 + "'" + s4,
            s1 + "''" + s4 + "{" + argIndex + "}",
            s4
        };

        Object[] formatArgs = new Object[] {
            s1,
            s2,
            s3,
            s4,
            Integer.valueOf(argIndex),
            Integer.valueOf(s1.length()),
            Long.valueOf(s2.length()),
            Boolean.valueOf(data.consumeBoolean()),
            Double.valueOf(data.consumeInt()),
            Character.valueOf((char) (data.consumeByte() & 0xFF))
        };

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            ExtendedMessageFormat emf;
            switch (i % 4) {
                case 0:
                    emf = new ExtendedMessageFormat(pattern);
                    break;
                case 1:
                    emf = new ExtendedMessageFormat(pattern, locale);
                    break;
                case 2:
                    emf = new ExtendedMessageFormat(pattern, registry);
                    break;
                default:
                    emf = new ExtendedMessageFormat(pattern, locale, registry);
                    break;
            }

            emf.toPattern();
            emf.format(formatArgs);

            String nextPattern = patterns[(i + 1) % patterns.length];
            emf.applyPattern(nextPattern);
            emf.toPattern();
            emf.format(formatArgs);
        }
    }
}