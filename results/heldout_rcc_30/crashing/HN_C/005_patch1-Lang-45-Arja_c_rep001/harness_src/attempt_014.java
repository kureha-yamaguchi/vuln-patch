package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeBoolean() ? data.consumeRemainingAsString() : "";

        String base;
        switch (data.consumeInt(0, 5)) {
            case 0:
                base = null;
                break;
            case 1:
                base = "";
                break;
            case 2:
                base = s1;
                break;
            case 3:
                base = s2;
                break;
            case 4:
                base = s1 + " " + s2;
                break;
            default:
                base = s2 + " " + s1 + s3;
                break;
        }

        String append;
        switch (data.consumeInt(0, 5)) {
            case 0:
                append = null;
                break;
            case 1:
                append = "";
                break;
            case 2:
                append = s1;
                break;
            case 3:
                append = s2;
                break;
            case 4:
                append = " ";
                break;
            default:
                append = s3;
                break;
        }

        String[] strs;
        if (base == null) {
            strs = new String[] {
                null,
                "",
                " ",
                "  ",
                "word",
                "word word",
                " word",
                "word ",
                s1,
                s2,
                s3
            };
        } else {
            strs = new String[] {
                null,
                "",
                base,
                base + " ",
                " " + base,
                base + " " + s1,
                s2 + " " + base,
                " ",
                "  ",
                "word",
                "word word"
            };
        }

        String[] appends = new String[] {
            null,
            "",
            append,
            s1,
            s2,
            s3,
            "...",
            " "
        };

        int sampleLen = base == null ? 0 : base.length();
        int any1 = data.consumeInt();
        int any2 = data.consumeInt();

        int[] ints = new int[] {
            Integer.MIN_VALUE,
            -2,
            -1,
            0,
            1,
            2,
            sampleLen - 1,
            sampleLen,
            sampleLen + 1,
            Integer.MAX_VALUE,
            any1,
            any2
        };

        for (int i = 0; i < strs.length; i++) {
            String str = strs[i];
            int len = str == null ? 0 : str.length();

            int[] perStringInts = new int[] {
                Integer.MIN_VALUE,
                -2,
                -1,
                0,
                1,
                len - 1,
                len,
                len + 1,
                Integer.MAX_VALUE,
                any1,
                any2
            };

            for (int j = 0; j < appends.length; j++) {
                String appendToEnd = appends[j];

                WordUtils.abbreviate(str, perStringInts[0], perStringInts[1], appendToEnd);
                WordUtils.abbreviate(str, perStringInts[2], perStringInts[3], appendToEnd);
                WordUtils.abbreviate(str, perStringInts[4], perStringInts[5], appendToEnd);
                WordUtils.abbreviate(str, perStringInts[6], perStringInts[7], appendToEnd);
                WordUtils.abbreviate(str, perStringInts[8], perStringInts[9], appendToEnd);
                WordUtils.abbreviate(str, perStringInts[10], perStringInts[6], appendToEnd);
                WordUtils.abbreviate(str, perStringInts[6], perStringInts[10], appendToEnd);
                WordUtils.abbreviate(str, perStringInts[10], perStringInts[9], appendToEnd);
                WordUtils.abbreviate(str, perStringInts[9], perStringInts[10], appendToEnd);
            }
        }

        for (String str : strs) {
            for (String appendToEnd : appends) {
                for (int k = 0; k + 1 < ints.length; k += 2) {
                    WordUtils.abbreviate(str, ints[k], ints[k + 1], appendToEnd);
                    WordUtils.abbreviate(str, ints[k + 1], ints[k], appendToEnd);
                }
            }
        }
    }
}