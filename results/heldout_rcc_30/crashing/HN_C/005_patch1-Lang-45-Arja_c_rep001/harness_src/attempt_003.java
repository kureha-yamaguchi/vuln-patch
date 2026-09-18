package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String base = data.consumeString(64);
        String ascii = data.consumeAsciiString(64);
        String tail = data.consumeRemainingAsString();

        String[] strs = new String[] {
            null,
            "",
            " ",
            "  ",
            base,
            ascii,
            tail,
            base + " " + ascii,
            ascii + " " + tail,
            " " + base,
            base + " ",
            "a b",
            "word",
            "word word",
            " " + base + " " + ascii + " ",
            base + "  " + tail
        };

        String[] suffixes = new String[] {
            null,
            "",
            " ",
            "...",
            ascii,
            tail,
            base
        };

        for (int i = 0; i < strs.length; i++) {
            String str = strs[i];
            int len = (str == null) ? 0 : str.length();

            int any1 = data.consumeInt();
            int any2 = data.consumeInt();

            int[] lowers = new int[] {
                any1,
                any2,
                -1,
                0,
                1,
                2,
                len - 2,
                len - 1,
                len,
                len + 1,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE
            };

            int[] uppers = new int[] {
                data.consumeInt(),
                data.consumeInt(),
                -1,
                0,
                1,
                2,
                len - 2,
                len - 1,
                len,
                len + 1,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE
            };

            for (int l = 0; l < lowers.length; l++) {
                for (int u = 0; u < uppers.length; u++) {
                    String append = suffixes[(l + u) % suffixes.length];
                    WordUtils.abbreviate(str, lowers[l], uppers[u], append);
                }
            }

            if (str != null) {
                int boundedA = data.consumeInt(-2, len + 2);
                int boundedB = data.consumeInt(-2, len + 2);
                WordUtils.abbreviate(str, boundedA, boundedB, suffixes[i % suffixes.length]);

                WordUtils.abbreviate(str, 0, -1, null);
                WordUtils.abbreviate(str, 0, len, "");
                WordUtils.abbreviate(str, 0, len + 1, "...");
                WordUtils.abbreviate(str, len, len, "...");
                WordUtils.abbreviate(str, len + 1, len - 1, tail);
                WordUtils.abbreviate(str, -1, len, base);
                WordUtils.abbreviate(str, -1, -1, ascii);
            } else {
                WordUtils.abbreviate(null, data.consumeInt(), data.consumeInt(), suffixes[i % suffixes.length]);
            }
        }
    }
}