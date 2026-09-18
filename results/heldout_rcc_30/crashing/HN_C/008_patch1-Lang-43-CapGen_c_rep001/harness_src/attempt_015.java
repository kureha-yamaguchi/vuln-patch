package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String raw = data.consumeString(128);
        String ascii = data.consumeAsciiString(128);
        String tail = data.consumeRemainingAsString();

        String[] seeds = new String[] {
            raw,
            ascii,
            tail,
            "'" + raw,
            raw + "'",
            "'" + raw + "'",
            "''" + raw,
            raw + "''",
            "{" + raw + "}",
            "{0}",
            "{0,'" + raw + "'}",
            "prefix '" + raw + "' suffix",
            "prefix ''" + raw + "'' suffix",
            ascii + "'" + tail,
            "'" + ascii + tail,
            ascii + "{" + tail + "}",
            buildPattern(raw, ascii, tail)
        };

        Locale[] locales = Locale.getAvailableLocales();
        Locale locale = locales.length == 0
                ? Locale.getDefault()
                : locales[Math.floorMod(data.consumeInt(), locales.length)];

        Map registry = data.consumeBoolean() ? null : Collections.EMPTY_MAP;

        for (int i = 0; i < seeds.length; i++) {
            String pattern = seeds[i];
            int mode = data.consumeInt(0, 5);
            ExtendedMessageFormat emf;
            switch (mode) {
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
                    break;
                default:
                    emf = new ExtendedMessageFormat("", locale, registry);
                    emf.applyPattern(pattern);
                    break;
            }

            if (data.consumeBoolean()) {
                emf.toPattern();
            }
            if (data.consumeBoolean()) {
                emf.equals(new ExtendedMessageFormat(pattern, locale, registry));
            }
            if (data.consumeBoolean()) {
                emf.hashCode();
            }
            if (data.consumeBoolean()) {
                Object[] args = new Object[Math.max(0, data.consumeInt(0, 4))];
                for (int j = 0; j < args.length; j++) {
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            args[j] = data.consumeString(16);
                            break;
                        case 1:
                            args[j] = Integer.valueOf(data.consumeInt());
                            break;
                        case 2:
                            args[j] = Boolean.valueOf(data.consumeBoolean());
                            break;
                        default:
                            args[j] = null;
                            break;
                    }
                }
                MessageFormat mf = emf;
                mf.format(args, new StringBuffer(), null);
            }
        }
    }

    private static String buildPattern(String a, String b, String c) {
        String[] parts = new String[] { a, b, c };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i] == null ? "" : parts[i];
            sb.append(i % 2 == 0 ? "X" : "Y");
            if (p.length() > 0) {
                int cut = Math.min(p.length(), 8);
                sb.append(p.substring(0, cut));
            }
            switch (i) {
                case 0:
                    sb.append('\'');
                    break;
                case 1:
                    sb.append("''");
                    break;
                default:
                    sb.append("{0}");
                    break;
            }
        }
        return sb.toString();
    }
}