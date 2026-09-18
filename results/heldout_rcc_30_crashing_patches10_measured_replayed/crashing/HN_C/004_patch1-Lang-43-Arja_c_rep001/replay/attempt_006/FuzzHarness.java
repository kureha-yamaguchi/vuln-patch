package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale locale;
        switch (data.consumeInt(0, 4)) {
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
            default:
                locale = Locale.getDefault();
                break;
        }

        Map registry = new HashMap();
        int argIdx = data.consumeInt(0, 9);
        boolean useAscii = data.consumeBoolean();

        String a = data.consumeString(24);
        String b = data.consumeAsciiString(24);
        String c = data.consumeString(24);
        String d = data.consumeRemainingAsString();

        String base = useAscii ? b : a;
        String q0 = "'";
        String q1 = "''";
        String q2 = "'" + base;
        String q3 = base + "'";
        String q4 = "'" + base + "'";
        String q5 = "'" + base + "''" + c + "'";
        String q6 = "'" + "''" + base + "'";
        String q7 = "'" + base + "''" + c + "''" + d + "'";
        String q8 = "'" + a + d;
        String q9 = "'" + a + "''" + b;
        String q10 = "'" + a + "''" + b + "'";
        String q11 = "'" + a + "''";
        String q12 = "'" + "{" + argIdx + "}" + "'";
        String q13 = "'" + "{" + argIdx + "}" + "''" + c + "'";

        String[] patterns = new String[] {
            "",
            q0,
            q1,
            q2,
            q3,
            q4,
            q5,
            q6,
            q7,
            q8,
            q9,
            q10,
            q11,
            q12,
            q13,
            "{0}",
            "{0,number}",
            "{0,date}",
            "{0,time}",
            "{0," + q0 + "}",
            "{0," + q1 + "}",
            "{0," + q2 + "}",
            "{0," + q3 + "}",
            "{0," + q4 + "}",
            "{0," + q5 + "}",
            "{0," + q6 + "}",
            "{0," + q7 + "}",
            "{0," + q8 + "}",
            "{0," + q9 + "}",
            "{0," + q10 + "}",
            "{0," + q11 + "}",
            "{0," + q12 + "}",
            "{0," + q13 + "}",
            "{0,pre" + q4 + "post}",
            "{0," + q4 + "post}",
            "{0,pre" + q4 + "}",
            "{0,pre" + q5 + "post}",
            "{0," + base + q4 + "}",
            "{0," + q4 + base + "}",
            "{0," + base + q5 + c + "}",
            "{0," + q5 + base + d + "}",
            "{0,{" + argIdx + "}}",
            "{0,'a''b'}",
            "{0,'x''y''z'}",
            "{0,'{''}'}",
            "{0,'foo''bar'}",
            "prefix" + q4 + "suffix",
            "prefix" + q5 + "suffix",
            "{" + argIdx + "}",
            "{" + argIdx + "," + q4 + "}",
            "{" + argIdx + "," + q5 + "}",
            "{" + argIdx + ",pre" + q5 + "post}",
            "{" + argIdx + "," + q0 + "}",
            "{" + argIdx + "," + q1 + "}",
            "{" + argIdx + "," + q11 + "}",
            a + q4 + b,
            a + q5 + b,
            a + "{0," + q5 + "}" + d
        };

        Object[] args = new Object[] {
            a,
            b,
            c,
            d,
            Integer.valueOf(argIdx),
            Boolean.valueOf(data.consumeBoolean())
        };

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];

            ExtendedMessageFormat emf;
            switch (i % 3) {
                case 0:
                    emf = new ExtendedMessageFormat(pattern);
                    break;
                case 1:
                    emf = new ExtendedMessageFormat(pattern, locale);
                    break;
                default:
                    emf = new ExtendedMessageFormat(pattern, locale, registry);
                    break;
            }

            emf.toPattern();
            emf.format(args);

            if (i + 1 < patterns.length) {
                emf.applyPattern(patterns[i + 1]);
                emf.toPattern();
                emf.format(args);
            }
        }

        String dynamic = "{"
                + data.consumeInt(0, 5)
                + ","
                + "'"
                + data.consumeString(12)
                + "''"
                + data.consumeAsciiString(12)
                + "'"
                + data.consumeRemainingAsString()
                + "}";
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(dynamic, locale, registry);
        emf2.toPattern();
        emf2.format(args);
    }
}