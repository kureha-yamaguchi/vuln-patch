package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeRemainingAsString();

        String base = data.consumeBoolean() ? s1 : s2;
        String extra = data.consumeBoolean() ? s2 : s3;

        String[] strs = new String[] {
            null,
            "",
            " ",
            "  ",
            "a",
            "a b",
            "abc def",
            " abc",
            "abc ",
            "abc  def",
            base,
            extra,
            base + " " + extra,
            extra + " " + base,
            base + base,
            extra + extra
        };

        String[] appenders = new String[] {
            null,
            "",
            " ",
            "...",
            ".",
            s1,
            s2,
            s3
        };

        int any1 = data.consumeInt();
        int any2 = data.consumeInt();

        for (int i = 0; i < strs.length; i++) {
            String str = strs[i];
            int len = (str == null) ? 0 : str.length();

            int[] lowers = new int[] {
                Integer.MIN_VALUE,
                -1,
                0,
                1,
                2,
                len - 1,
                len,
                len + 1,
                any1
            };

            int[] uppers = new int[] {
                Integer.MIN_VALUE,
                -1,
                0,
                1,
                2,
                len - 1,
                len,
                len + 1,
                Integer.MAX_VALUE,
                any2
            };

            String appendToEnd = appenders[Math.abs(any1 + i) % appenders.length];

            WordUtils.abbreviate(str, lowers[0], uppers[0], appendToEnd);
            WordUtils.abbreviate(str, lowers[1], uppers[1], appendToEnd);
            WordUtils.abbreviate(str, lowers[2], uppers[2], appendToEnd);
            WordUtils.abbreviate(str, lowers[3], uppers[3], appendToEnd);
            WordUtils.abbreviate(str, lowers[4], uppers[4], appendToEnd);
            WordUtils.abbreviate(str, lowers[5], uppers[5], appendToEnd);
            WordUtils.abbreviate(str, lowers[6], uppers[6], appendToEnd);
            WordUtils.abbreviate(str, lowers[7], uppers[7], appendToEnd);
            WordUtils.abbreviate(str, lowers[8], uppers[8], appendToEnd);

            WordUtils.abbreviate(str, lowers[2], uppers[8], appendToEnd);
            WordUtils.abbreviate(str, lowers[8], uppers[2], appendToEnd);
            WordUtils.abbreviate(str, lowers[6], uppers[1], appendToEnd);
            WordUtils.abbreviate(str, lowers[1], uppers[6], appendToEnd);
        }
    }
}