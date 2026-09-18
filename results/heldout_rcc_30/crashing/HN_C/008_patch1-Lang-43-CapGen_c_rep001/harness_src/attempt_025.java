package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String part1 = data.consumeString(64);
        String part2 = data.consumeAsciiString(64);
        String part3 = data.consumeRemainingAsString();

        StringBuilder sb = new StringBuilder();
        int mode = data.consumeInt(0, 7);

        switch (mode) {
            case 0:
                sb.append(part1);
                break;
            case 1:
                sb.append('\'').append(part1);
                break;
            case 2:
                sb.append(part1).append('\'');
                break;
            case 3:
                sb.append(part1).append("''").append(part2);
                break;
            case 4:
                sb.append('{').append(Math.floorMod(part1.hashCode(), 10)).append('}');
                sb.append('\'').append(part2);
                break;
            case 5:
                sb.append(part1).append('{').append(Math.floorMod(part2.hashCode(), 10)).append("}");
                sb.append("''").append(part3);
                break;
            case 6:
                sb.append('\'').append('{').append(part1).append('}').append('\'').append(part2);
                break;
            default:
                sb.append(part1).append(part2).append(part3);
                break;
        }

        if (sb.length() == 0 || data.consumeBoolean()) {
            int extras = data.consumeInt(0, 8);
            for (int i = 0; i < extras; i++) {
                switch (data.consumeInt(0, 6)) {
                    case 0:
                        sb.append('\'');
                        break;
                    case 1:
                        sb.append("''");
                        break;
                    case 2:
                        sb.append('{').append(data.consumeInt(-3, 12)).append('}');
                        break;
                    case 3:
                        sb.append('{').append(data.consumeInt(-3, 12)).append(',').append(data.consumeAsciiString(8)).append('}');
                        break;
                    case 4:
                        sb.append(data.consumeAsciiString(8));
                        break;
                    case 5:
                        sb.append('{');
                        break;
                    default:
                        sb.append('}');
                        break;
                }
            }
        }

        String pattern = sb.toString();

        Locale[] locales = new Locale[] {
            Locale.getDefault(),
            Locale.US,
            Locale.UK,
            Locale.GERMANY,
            Locale.JAPAN,
            new Locale(part2, part1),
            new Locale(part1, part2, part3)
        };
        Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        Map registry = data.consumeBoolean() ? null : new HashMap();

        int api = data.consumeInt(0, 5);
        ExtendedMessageFormat emf;
        switch (api) {
            case 0:
                emf = new ExtendedMessageFormat(pattern);
                break;
            case 1:
                emf = new ExtendedMessageFormat(pattern, locale);
                break;
            case 2:
                emf = new ExtendedMessageFormat(pattern, registry);
                break;
            case 3:
                emf = new ExtendedMessageFormat(pattern, locale, registry);
                break;
            case 4:
                emf = new ExtendedMessageFormat("");
                emf.applyPattern(pattern);
                emf.toPattern();
                break;
            default:
                emf = new ExtendedMessageFormat("", locale, registry);
                emf.applyPattern(pattern);
                emf.toPattern();
                break;
        }
    }
}