package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        String a = data.consumeString(64);
        String b = data.consumeAsciiString(64);
        String c = data.consumeRemainingAsString();
        boolean useRegistry = data.consumeBoolean();
        boolean useLocale = data.consumeBoolean();
        int pick = data.consumeInt(-16, 16);

        Map registry = useRegistry ? new HashMap() : null;
        Locale locale = useLocale ? Locale.US : Locale.ROOT;

        String[] seeds = new String[] {
            a,
            b,
            c,
            "",
            "'",
            "''",
            "'''",
            "''''",
            "'a",
            "a'",
            "'a'",
            "a''b",
            "''a",
            "a''",
            "'{",
            "{'",
            "'}'",
            "}'",
            "{0}",
            "'{0}'",
            "''{0}''",
            "'{0}",
            "{0}'",
            "{0,''}",
            "{0,'" + a + "'}",
            "'" + a,
            a + "'",
            "'" + a + "'",
            "''" + a,
            a + "''",
            "'" + a + "''",
            "''" + a + "'",
            "'" + a + "''" + b,
            a + "''" + b,
            "'" + a + b,
            a + b + "'",
            "'" + a + "'" + b,
            a + "'" + b,
            "'" + a + "'" + b + "'",
            "prefix '" + a,
            "prefix " + a + "' suffix",
            "'" + a + "{0}",
            "{0}'" + a,
            "'" + a + "{0}'",
            "{" + Math.abs(pick) + "}",
            "{" + Math.abs(pick) + ",number}",
            "{0,number}",
            "{0,date}",
            "{0,time}",
            a + "{0}" + b,
            "'" + a + "{" + Math.abs(pick) + "}" + b,
            a + "{" + b + "}",
            a + "'{" + b + "}",
            "'{" + a + "}",
            a + "''{" + b + "}",
            "'" + a + "''" + c + "'",
            "'" + a + "''",
            "''" + a + "''",
            "'" + "'",
            "'\u0000",
            "\u0000'",
            "'\uD800",
            "\uD800'",
            "'\uDC00",
            "\uDC00'",
            a + "\u0000" + "'",
            "'" + "\u0000" + a,
            a + "\uD800" + "'",
            "'" + "\uDC00" + a
        };

        Object[] formatArgs = new Object[] {
            a,
            b,
            c,
            Integer.valueOf(pick),
            Long.valueOf((long) pick),
            Double.valueOf((double) pick),
            Boolean.TRUE
        };

        for (int i = 0; i < seeds.length; i++) {
            String p = seeds[i];

            ExtendedMessageFormat emf;
            switch (Math.abs(pick + i) % 4) {
                case 0:
                    emf = new ExtendedMessageFormat(p);
                    break;
                case 1:
                    emf = new ExtendedMessageFormat(p, locale);
                    break;
                case 2:
                    emf = new ExtendedMessageFormat(p, registry);
                    break;
                default:
                    emf = new ExtendedMessageFormat(p, locale, registry);
                    break;
            }

            emf.toPattern();
            emf.format(formatArgs);
            emf.format(formatArgs, new StringBuffer(), new FieldPosition(0));

            String p2 = seeds[(i + 1) % seeds.length];
            String p3 = seeds[(i + 2) % seeds.length];
            String p4 = seeds[(i + 3) % seeds.length];

            emf.applyPattern(p2);
            emf.toPattern();
            emf.format(formatArgs);

            emf.applyPattern(p + p2);
            emf.toPattern();
            emf.format(formatArgs);

            emf.applyPattern(p2 + p);
            emf.toPattern();
            emf.format(formatArgs);

            emf.applyPattern(p + "'" + p2);
            emf.toPattern();
            emf.format(formatArgs);

            emf.applyPattern("'" + p + p2);
            emf.toPattern();
            emf.format(formatArgs);

            emf.applyPattern(p + "''" + p2);
            emf.toPattern();
            emf.format(formatArgs);

            emf.applyPattern("''" + p + p2);
            emf.toPattern();
            emf.format(formatArgs);

            emf.applyPattern(p3 + "'" + p4 + "'");
            emf.toPattern();
            emf.format(formatArgs);
        }
    }
}