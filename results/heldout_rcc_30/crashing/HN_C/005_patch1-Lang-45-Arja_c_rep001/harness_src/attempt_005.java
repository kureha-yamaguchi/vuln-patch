package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int rawLower = data.consumeInt();
        int rawUpper = data.consumeInt();
        int altLower = data.consumeInt(-4, 128);
        int altUpper = data.consumeInt(-4, 128);
        boolean makePrimaryNull = data.consumeBoolean();
        boolean makeAppendNull = data.consumeBoolean();

        String primary = data.consumeString(64);
        String ascii = data.consumeAsciiString(64);
        String extra = data.consumeString(16);
        String remaining = data.consumeRemainingAsString();

        String[] strs = new String[] {
            makePrimaryNull ? null : primary,
            "",
            " ",
            "a",
            ascii,
            primary + " " + ascii,
            ascii + " " + primary + " " + extra,
            remaining,
            remaining + " " + extra,
            primary + remaining
        };

        String[] appendValues = new String[] {
            makeAppendNull ? null : extra,
            null,
            "",
            " ",
            ascii,
            remaining
        };

        for (int i = 0; i < strs.length; i++) {
            String str = strs[i];
            int len = str == null ? 0 : str.length();

            int[] lowers = new int[] {
                rawLower,
                altLower,
                -1,
                0,
                1,
                len - 1,
                len,
                len + 1
            };

            int[] uppers = new int[] {
                rawUpper,
                altUpper,
                -1,
                0,
                1,
                len - 1,
                len,
                len + 1
            };

            for (int lower : lowers) {
                for (int upper : uppers) {
                    for (String appendToEnd : appendValues) {
                        WordUtils.abbreviate(str, lower, upper, appendToEnd);
                    }
                }
            }
        }
    }
}