package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale[] locales = new Locale[] {
            Locale.ROOT,
            Locale.US,
            Locale.UK,
            Locale.FRANCE,
            Locale.GERMANY,
            Locale.JAPAN,
            Locale.CHINA
        };
        Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        Map registry = data.consumeBoolean() ? null : new HashMap();

        String a = data.consumeString(32);
        String b = data.consumeAsciiString(32);
        String c = data.consumeString(32);
        byte[] raw = data.consumeBytes(32);
        String d = new String(raw);
        int argIndex = data.consumeInt(0, 3);

        StringBuilder p1 = new StringBuilder();
        switch (data.consumeInt(0, 7)) {
            case 0:
                p1.append(a);
                break;
            case 1:
                p1.append('\'').append(a);
                break;
            case 2:
                p1.append("''").append(a).append('\'').append(b);
                break;
            case 3:
                p1.append("pre").append('\'').append(a).append('\'').append("post");
                break;
            case 4:
                p1.append('{').append(argIndex).append('}').append('\'').append(a);
                break;
            case 5:
                p1.append('\'').append(a).append('\'').append('{').append(argIndex).append('}');
                break;
            case 6:
                p1.append("{").append(argIndex).append(",number,").append(b).append("}");
                break;
            default:
                p1.append(a).append('\'').append(b).append('{').append(argIndex).append('}').append(d);
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(p1.toString(), locale, registry);
        emf.toPattern();
        emf.hashCode();
        emf.equals(new ExtendedMessageFormat(p1.toString(), locale, registry));

        Object[] args = new Object[] {
            a,
            Integer.valueOf(data.consumeInt()),
            b,
            Long.valueOf(data.consumeInt())
        };
        emf.format(args);

        String e = data.consumeAsciiString(32);
        String f = data.consumeString(32);
        String tail = data.consumeRemainingAsString();

        StringBuilder p2 = new StringBuilder();
        switch (data.consumeInt(0, 9)) {
            case 0:
                p2.append(e);
                break;
            case 1:
                p2.append('\'');
                break;
            case 2:
                p2.append("''");
                break;
            case 3:
                p2.append('\'').append(e);
                break;
            case 4:
                p2.append(e).append('\'').append(f);
                break;
            case 5:
                p2.append('\'').append(e).append('\'');
                break;
            case 6:
                p2.append("{").append(argIndex).append("}");
                break;
            case 7:
                p2.append("{").append(argIndex).append("}").append('\'').append(e);
                break;
            case 8:
                p2.append('\'').append(e).append("''").append(f).append('\'').append(tail);
                break;
            default:
                p2.append(d).append('\'').append(tail);
                break;
        }

        emf.applyPattern(p2.toString());
        emf.toPattern();
        emf.format(args);

        if (data.consumeBoolean()) {
            StringBuilder p3 = new StringBuilder();
            p3.append(data.consumeAsciiString(16));
            if (data.consumeBoolean()) {
                p3.append('\'');
            }
            if (data.consumeBoolean()) {
                p3.append("''");
            }
            if (data.consumeBoolean()) {
                p3.append('{').append(data.consumeInt(0, 3)).append('}');
            }
            p3.append(data.consumeRemainingAsString());
            emf.applyPattern(p3.toString());
            emf.format(args);
        }
    }
}