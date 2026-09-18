package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeRemainingAsString();

        String[] atoms = new String[] {
            "",
            "'",
            "''",
            "{",
            "}",
            ",",
            "{0}",
            "{0,number}",
            "{0,date}",
            "{0,time}",
            s1,
            s2,
            s3
        };

        StringBuilder patternBuilder = new StringBuilder();
        int parts = data.consumeInt(0, 12);
        for (int i = 0; i < parts; i++) {
            String a = atoms[data.consumeInt(0, atoms.length - 1)];
            patternBuilder.append(a);
            if (data.consumeBoolean()) {
                patternBuilder.append('\'');
            }
            if (data.consumeBoolean()) {
                patternBuilder.append('{').append(data.consumeInt()).append('}');
            }
            if (data.consumeBoolean()) {
                patternBuilder.append('{')
                        .append(data.consumeInt())
                        .append(',')
                        .append(atoms[data.consumeInt(0, atoms.length - 1)])
                        .append('}');
            }
        }

        if (data.consumeBoolean()) {
            patternBuilder.insert(0, '\'');
        }
        if (data.consumeBoolean()) {
            patternBuilder.append('\'');
        }
        if (data.consumeBoolean()) {
            patternBuilder.append("''");
        }
        if (data.consumeBoolean()) {
            patternBuilder.append("{0,'").append(s2).append("'}");
        }
        if (data.consumeBoolean()) {
            patternBuilder.append("{").append(data.consumeInt()).append(",");
            patternBuilder.append(s1);
            if (data.consumeBoolean()) {
                patternBuilder.append('\'');
            }
            patternBuilder.append("}");
        }

        String pattern = patternBuilder.toString();

        Map registry = data.consumeBoolean() ? null : new HashMap();
        Locale locale;
        switch (data.consumeInt(0, 5)) {
            case 0:
                locale = Locale.ROOT;
                break;
            case 1:
                locale = Locale.US;
                break;
            case 2:
                locale = Locale.ENGLISH;
                break;
            case 3:
                locale = Locale.JAPAN;
                break;
            case 4:
                locale = Locale.getDefault();
                break;
            default:
                locale = new Locale(s2, s1);
                break;
        }

        ExtendedMessageFormat emf;
        switch (data.consumeInt(0, 3)) {
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

        if (data.consumeBoolean()) {
            emf.toPattern();
        }

        int extraPatterns = data.consumeInt(0, 4);
        for (int i = 0; i < extraPatterns; i++) {
            String p;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    p = s1;
                    break;
                case 1:
                    p = s2;
                    break;
                case 2:
                    p = s3;
                    break;
                case 3:
                    p = "'" + s1;
                    break;
                case 4:
                    p = s1 + "'" + s2;
                    break;
                case 5:
                    p = "{0,'" + s2 + "'}" + s3;
                    break;
                default:
                    p = pattern + "'" + atoms[data.consumeInt(0, atoms.length - 1)];
                    break;
            }
            emf.applyPattern(p);
            if (data.consumeBoolean()) {
                emf.toPattern();
            }
        }
    }
}