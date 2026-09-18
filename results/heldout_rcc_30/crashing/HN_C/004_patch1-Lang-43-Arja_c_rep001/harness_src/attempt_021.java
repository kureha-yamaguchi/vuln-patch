package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        String s3 = data.consumeString(32);
        String s4 = data.consumeRemainingAsString();

        String q = "'";
        String qq = "''";

        String[] atoms = new String[] {
            "",
            q,
            qq,
            s1,
            s2,
            s3,
            s4,
            s1 + q,
            q + s1,
            s1 + qq,
            qq + s1,
            s1 + q + s2,
            s1 + qq + s2,
            q + s1 + q,
            q + s1 + qq,
            qq + s1 + q,
            s1 + "}" + q,
            q + "}",
            "}",
            "{",
            ",",
            "x",
            "x" + q,
            q + "x",
            "x" + qq,
            qq + "x"
        };

        String[] patterns = new String[] {
            "{0," + atoms[data.consumeInt(0, atoms.length - 1)] + "}",
            "{0," + q + atoms[data.consumeInt(0, atoms.length - 1)] + "}",
            "{0," + atoms[data.consumeInt(0, atoms.length - 1)] + q + "}",
            "{0," + q + atoms[data.consumeInt(0, atoms.length - 1)] + q + "}",
            "{0," + q + atoms[data.consumeInt(0, atoms.length - 1)] + qq + "}",
            "{0," + qq + atoms[data.consumeInt(0, atoms.length - 1)] + "}",
            "{0," + atoms[data.consumeInt(0, atoms.length - 1)] + qq + atoms[data.consumeInt(0, atoms.length - 1)] + "}",
            "{0," + q + atoms[data.consumeInt(0, atoms.length - 1)] + qq + atoms[data.consumeInt(0, atoms.length - 1)] + "}",
            "{0," + q + atoms[data.consumeInt(0, atoms.length - 1)] + qq + atoms[data.consumeInt(0, atoms.length - 1)] + q + "}",
            "{0," + atoms[data.consumeInt(0, atoms.length - 1)] + q + atoms[data.consumeInt(0, atoms.length - 1)] + "}",
            "{0," + atoms[data.consumeInt(0, atoms.length - 1)] + qq + "}",
            "{0," + qq + "}",
            "{0," + q + "}",
            "{0," + q + qq + "}",
            "{0," + qq + q + "}",
            "{0," + q + s1 + qq + "}",
            "{0," + q + s1 + qq + s2 + "}",
            "{0," + q + s1 + qq + s2 + q + "}",
            "{0," + s1 + qq + s2 + "}",
            "{0," + s1 + qq + s2 + q + "}",
            "x{0," + q + s1 + qq + "}",
            "x{0," + q + s1 + qq + s2 + "}",
            "x{0," + q + s1 + qq + s2 + q + "}y",
            "{0," + q + q + "}",
            "{0," + q + q + q + "}",
            "{0," + q + q + q + q + "}",
            "{0," + q + "a" + qq + "}",
            "{0," + q + "a" + qq + "b" + "}",
            "{0," + q + "a" + qq + "b" + q + "}",
            "{0," + q + "a" + qq + "b" + qq + "}",
            "{0," + q + "a" + qq + "b" + qq + "c" + "}",
            "{0," + q + atoms[data.consumeInt(0, atoms.length - 1)] + qq + atoms[data.consumeInt(0, atoms.length - 1)] + qq + atoms[data.consumeInt(0, atoms.length - 1)] + "}"
        };

        Map registry = new HashMap();
        Locale locale;
        switch (data.consumeInt(0, 3)) {
            case 0:
                locale = Locale.ROOT;
                break;
            case 1:
                locale = Locale.US;
                break;
            case 2:
                locale = Locale.getDefault();
                break;
            default:
                locale = new Locale("en", "US");
                break;
        }

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            if (data.consumeBoolean()) {
                ExtendedMessageFormat emf = new ExtendedMessageFormat("", locale, registry);
                emf.applyPattern(pattern);
            } else {
                new ExtendedMessageFormat(pattern, locale, registry);
            }
        }
    }
}