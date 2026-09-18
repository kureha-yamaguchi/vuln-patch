package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeBoolean() ? null : data.consumeString(128);
        String s2 = data.consumeBoolean() ? null : data.consumeAsciiString(128);
        String s3 = data.consumeBoolean() ? null : data.consumeRemainingAsString();

        String append1 = data.consumeBoolean() ? null : data.consumeString(32);
        String append2 = data.consumeBoolean() ? null : data.consumeAsciiString(32);

        int lower1 = data.consumeInt();
        int upper1 = data.consumeInt();
        int lower2 = data.consumeInt(-16, 256);
        int upper2 = data.consumeInt(-16, 256);

        String[] strings = new String[] {
            s1,
            s2,
            s3,
            "",
            " ",
            "a",
            "word",
            "two words",
            " leading space",
            "trailing space ",
            "many  spaces  inside",
            "\t",
            "\n",
            "a b c",
            "abcdef"
        };

        String[] suffixes = new String[] {
            append1,
            append2,
            "",
            "...",
            " ",
            "\t",
            "\n"
        };

        int[] lowers = new int[] {
            lower1,
            lower2,
            -1,
            0,
            1,
            2,
            5,
            10,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE
        };

        int[] uppers = new int[] {
            upper1,
            upper2,
            -1,
            0,
            1,
            2,
            5,
            10,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE
        };

        for (int i = 0; i < strings.length; i++) {
            String str = strings[i];
            int len = str == null ? 0 : str.length();

            int[] dynamicLowers = new int[] {
                0,
                1,
                len - 1,
                len,
                len + 1,
                lower1,
                lower2
            };

            int[] dynamicUppers = new int[] {
                -1,
                0,
                1,
                len - 1,
                len,
                len + 1,
                upper1,
                upper2
            };

            for (int li = 0; li < lowers.length; li++) {
                for (int ui = 0; ui < uppers.length; ui++) {
                    WordUtils.abbreviate(str, lowers[li], uppers[ui], suffixes[(li + ui) % suffixes.length]);
                }
            }

            for (int li = 0; li < dynamicLowers.length; li++) {
                for (int ui = 0; ui < dynamicUppers.length; ui++) {
                    for (int si = 0; si < suffixes.length; si++) {
                        WordUtils.abbreviate(str, dynamicLowers[li], dynamicUppers[ui], suffixes[si]);
                    }
                }
            }
        }
    }
}