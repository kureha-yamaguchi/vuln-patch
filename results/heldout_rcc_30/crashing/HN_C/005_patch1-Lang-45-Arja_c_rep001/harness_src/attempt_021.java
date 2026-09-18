package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeRemainingAsString();

        String[] strings = new String[] {
            null,
            "",
            " ",
            "  ",
            s1,
            s2,
            s3,
            s1 + " " + s2,
            s2 + " " + s3,
            " " + s1,
            s1 + " ",
            "a b",
            "word",
            "word word",
            " leading space",
            "trailing space ",
            "multiple  spaces  here"
        };

        String[] appenders = new String[] {
            null,
            "",
            " ",
            "...",
            s1,
            s2,
            s3
        };

        int i1 = data.consumeInt();
        int i2 = data.consumeInt();
        int i3 = data.consumeInt(-4, 128);
        int i4 = data.consumeInt(-4, 128);

        int[] lowers = new int[] {
            i1,
            i2,
            i3,
            i4,
            -1,
            0,
            1,
            2,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE
        };

        int[] uppers = new int[] {
            i1,
            i2,
            i3,
            i4,
            -1,
            0,
            1,
            2,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE
        };

        for (String str : strings) {
            int len = (str == null) ? 0 : str.length();

            WordUtils.abbreviate(str, 0, -1, null);
            WordUtils.abbreviate(str, 0, 0, "");
            WordUtils.abbreviate(str, 0, len, "...");
            WordUtils.abbreviate(str, len, len, s1);
            WordUtils.abbreviate(str, len + 1, len + 1, s2);
            WordUtils.abbreviate(str, 0, len + 1, s3);
            WordUtils.abbreviate(str, Math.max(0, len - 1), Math.max(0, len - 1), null);

            for (String appendToEnd : appenders) {
                WordUtils.abbreviate(str, 0, -1, appendToEnd);
                WordUtils.abbreviate(str, 0, len, appendToEnd);
                WordUtils.abbreviate(str, 0, len + 1, appendToEnd);

                for (int lower : lowers) {
                    WordUtils.abbreviate(str, lower, -1, appendToEnd);
                    WordUtils.abbreviate(str, lower, len, appendToEnd);

                    for (int upper : uppers) {
                        WordUtils.abbreviate(str, lower, upper, appendToEnd);
                    }
                }
            }
        }
    }
}