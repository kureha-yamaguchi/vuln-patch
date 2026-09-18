package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String a = data.consumeString(64);
        String b = data.consumeAsciiString(64);
        String c = data.consumeString(32);
        String d = data.consumeAsciiString(32);
        boolean useLocale = data.consumeBoolean();
        boolean useEmptyRegistry = data.consumeBoolean();

        Locale locale = useLocale
                ? new Locale(data.consumeAsciiString(8), data.consumeAsciiString(8), data.consumeAsciiString(8))
                : Locale.getDefault();
        Map registry = useEmptyRegistry ? Collections.EMPTY_MAP : null;

        String[] patterns = new String[] {
                a,
                b,
                c + d,
                "",
                "'",
                "''",
                "'''",
                a + "'",
                "'" + a,
                "'" + a + "'",
                "''" + a + "''",
                "{0}",
                "{0," + b + "}",
                "{0," + b + "," + c + "}",
                "'" + "{0}" + "'",
                "{0}'" + a,
                "'" + a + "{0}",
                a + "{0}" + b,
                "{" + data.consumeInt() + "}",
                "'" + d + "''" + c + "'",
                "{0," + d + ",'" + c + "'}",
                "'{'0'}'",
                a + "''" + b,
                "'" + a + "''" + b + "'"
        };

        int rounds = data.consumeInt(1, patterns.length);
        for (int i = 0; i < rounds; i++) {
            String p = patterns[data.consumeInt(0, patterns.length - 1)];

            ExtendedMessageFormat emf;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    emf = new ExtendedMessageFormat(p);
                    break;
                case 1:
                    emf = new ExtendedMessageFormat(p, locale);
                    break;
                default:
                    emf = new ExtendedMessageFormat(p, locale, registry);
                    break;
            }

            if (data.consumeBoolean()) {
                emf.toPattern();
            }

            if (data.consumeBoolean()) {
                String p2 = patterns[data.consumeInt(0, patterns.length - 1)];
                emf.applyPattern(p2);
            }

            if (data.consumeBoolean()) {
                Object[] args = new Object[data.consumeInt(0, 4)];
                for (int j = 0; j < args.length; j++) {
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            args[j] = null;
                            break;
                        case 1:
                            args[j] = data.consumeString(16);
                            break;
                        case 2:
                            args[j] = Integer.valueOf(data.consumeInt());
                            break;
                        case 3:
                            args[j] = Boolean.valueOf(data.consumeBoolean());
                            break;
                        default:
                            args[j] = new byte[] { data.consumeByte() };
                            break;
                    }
                }
                emf.format(args, new StringBuffer(), new FieldPosition(0));
            }

            if (data.consumeBoolean()) {
                emf.equals(new ExtendedMessageFormat(patterns[data.consumeInt(0, patterns.length - 1)], locale, registry));
                emf.hashCode();
            }
        }
    }
}