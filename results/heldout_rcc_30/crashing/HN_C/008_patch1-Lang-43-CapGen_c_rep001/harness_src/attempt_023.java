package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static void exercise(String pattern, Locale locale, Map registry, Object[] args) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, locale, registry);
        emf.toPattern();
        emf.format(args);

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat("", locale, registry);
        emf2.applyPattern(pattern);
        emf2.toPattern();
        emf2.format(args);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String a = data.consumeString(24);
        String b = data.consumeAsciiString(24);
        String c = data.consumeString(24);
        String d = data.consumeAsciiString(24);
        String rest = data.consumeRemainingAsString();

        Locale locale;
        switch (data.consumeInt(0, 4)) {
            case 0:
                locale = Locale.ROOT;
                break;
            case 1:
                locale = Locale.US;
                break;
            case 2:
                locale = Locale.UK;
                break;
            case 3:
                locale = Locale.GERMANY;
                break;
            default:
                locale = Locale.JAPAN;
                break;
        }

        Map registry = new HashMap();
        Object[] args = new Object[] { a, b, c, d, rest, Integer.valueOf(data.consumeInt()) };

        String q1 = "'" + a + "''" + b + "'";
        String q2 = "'" + b + "''" + c + "'";
        String q3 = "'" + c + "''" + d + "'";
        String q4 = "'" + rest + "''" + a + "'";

        String[] patterns = new String[] {
            "{0," + q1 + "}",
            "{0,choice," + q1 + "}",
            "{0,number," + q1 + "}",
            "{0,date," + q1 + "}",
            "{0,time," + q1 + "}",
            "{0," + a + q1 + "}",
            "{0," + q1 + a + "}",
            "{0," + a + q1 + b + "}",
            "{0," + q2 + "}",
            "{0," + q3 + "}",
            "{0," + q4 + "}",
            "{0,{" + q1 + "}}",
            "{0," + a + "," + q1 + "}",
            "x{0," + q1 + "}y",
            "'" + a + "'{0," + q1 + "}",
            "{0," + q1 + "}'" + b,
            "{0," + "'" + a + "''" + "'}",
            "{0," + "'x''y'" + "}",
            "{0," + "'a''b'" + "}",
            "{0,choice,'a''b'}",
            "{0,number,'a''b'}",
            "{0,date,'a''b'}",
            "{0,time,'a''b'}"
        };

        for (int i = 0; i < patterns.length; i++) {
            exercise(patterns[i], locale, registry, args);
        }
    }
}