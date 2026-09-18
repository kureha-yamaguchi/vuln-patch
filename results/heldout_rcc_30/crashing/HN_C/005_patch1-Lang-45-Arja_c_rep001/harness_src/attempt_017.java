package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String generated = data.consumeString(64);
        String ascii = data.consumeAsciiString(64);
        String withSpaces = data.consumeAsciiString(64);
        String append1 = data.consumeBoolean() ? null : data.consumeString(32);
        String append2 = data.consumeBoolean() ? null : data.consumeAsciiString(32);

        String[] strs = new String[] {
            null,
            "",
            generated,
            ascii,
            " ",
            "  ",
            "a",
            "a b",
            "abc def ghi",
            withSpaces,
            generated + " " + ascii,
            " " + generated,
            ascii + " ",
            data.consumeRemainingAsString()
        };

        String[] appenders = new String[] {
            null,
            "",
            append1,
            append2,
            "...",
            " ",
            generated,
            ascii
        };

        for (int s = 0; s < strs.length; s++) {
            String str = strs[s];
            int len = (str == null) ? 0 : str.length();

            int[] lowers = new int[] {
                data.consumeInt(),
                data.consumeInt(-16, 128),
                Integer.MIN_VALUE,
                -2,
                -1,
                0,
                1,
                len - 1,
                len,
                len + 1,
                Integer.MAX_VALUE
            };

            int[] uppers = new int[] {
                data.consumeInt(),
                data.consumeInt(-16, 128),
                Integer.MIN_VALUE,
                -2,
                -1,
                0,
                1,
                len - 1,
                len,
                len + 1,
                Integer.MAX_VALUE
            };

            for (int i = 0; i < lowers.length; i++) {
                int lower = lowers[i];
                int upper = uppers[i % uppers.length];
                String append = appenders[i % appenders.length];
                WordUtils.abbreviate(str, lower, upper, append);
            }

            if (str != null) {
                WordUtils.abbreviate(str, 0, -1, null);
                WordUtils.abbreviate(str, 0, len, "");
                WordUtils.abbreviate(str, len, len, "...");
                WordUtils.abbreviate(str, len + 1, len, "...");
                WordUtils.abbreviate(str, -1, len + 1, append1);
                WordUtils.abbreviate(str, 1, 0, append2);
            }
        }
    }
}