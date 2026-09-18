package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String a = data.consumeString(64);
        String b = data.consumeString(64);
        String c = data.consumeAsciiString(64);

        java.util.Locale[] locales = new java.util.Locale[] {
            java.util.Locale.ROOT,
            java.util.Locale.US,
            java.util.Locale.UK,
            java.util.Locale.FRANCE,
            java.util.Locale.GERMANY,
            java.util.Locale.JAPAN
        };
        java.util.Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        int arg1 = data.consumeInt(-2, 3);
        int arg2 = data.consumeInt(-2, 3);
        int choice1 = data.consumeInt(0, 7);
        int choice2 = data.consumeInt(0, 7);

        String[] patterns = new String[8];
        patterns[0] = a;
        patterns[1] = "'" + a;
        patterns[2] = "'" + a + "'";
        patterns[3] = "'" + a + "''" + b;
        patterns[4] = "{" + arg1 + "}";
        patterns[5] = "{" + arg1 + "," + c + "}";
        patterns[6] = "{" + arg1 + ",'" + a + "''" + b + "'}";
        patterns[7] = a + "{"+ arg2 + "," + c + "}" + "'" + b;

        ExtendedMessageFormat emf =
                new ExtendedMessageFormat("", locale, java.util.Collections.EMPTY_MAP);

        emf.applyPattern(patterns[choice1]);
        emf.toPattern();
        emf.hashCode();

        String second = patterns[choice2];
        if (data.consumeBoolean()) {
            second = second + data.consumeRemainingAsString();
        } else if (data.remainingBytes() > 0) {
            second = data.consumeRemainingAsString() + second;
        }

        emf.applyPattern(second);
        emf.toPattern();
        emf.equals(new ExtendedMessageFormat(second, locale, java.util.Collections.EMPTY_MAP));
    }
}