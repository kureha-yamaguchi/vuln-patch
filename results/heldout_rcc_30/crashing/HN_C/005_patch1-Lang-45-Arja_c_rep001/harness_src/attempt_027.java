package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String primary;
        switch (data.consumeInt(0, 7)) {
            case 0:
                primary = null;
                break;
            case 1:
                primary = "";
                break;
            case 2:
                primary = data.consumeString(data.consumeInt(0, 64));
                break;
            case 3:
                primary = data.consumeAsciiString(data.consumeInt(0, 64));
                break;
            case 4:
                primary = " " + data.consumeString(data.consumeInt(0, 32));
                break;
            case 5:
                primary = data.consumeString(data.consumeInt(0, 32)) + " ";
                break;
            case 6:
                primary = data.consumeString(data.consumeInt(0, 24)) + " " + data.consumeString(data.consumeInt(0, 24));
                break;
            default:
                primary = data.consumeRemainingAsString();
                break;
        }

        String appendToEnd;
        switch (data.consumeInt(0, 4)) {
            case 0:
                appendToEnd = null;
                break;
            case 1:
                appendToEnd = "";
                break;
            case 2:
                appendToEnd = data.consumeString(data.consumeInt(0, 16));
                break;
            case 3:
                appendToEnd = data.consumeAsciiString(data.consumeInt(0, 16));
                break;
            default:
                appendToEnd = data.consumeRemainingAsString();
                break;
        }

        int lower = data.consumeInt();
        int upper = data.consumeInt();

        WordUtils.abbreviate(primary, lower, upper, appendToEnd);

        String[] strs = new String[] {
            primary,
            null,
            "",
            " ",
            "a",
            "ab",
            "a b",
            "word",
            "two words",
            " leading",
            "trailing ",
            "multiple  spaces",
            appendToEnd,
            appendToEnd == null ? null : appendToEnd + " " + appendToEnd
        };

        String[] suffixes = new String[] {
            appendToEnd,
            null,
            "",
            ".",
            "...",
            " ",
            primary
        };

        int[] ints = new int[] {
            lower,
            upper,
            -1,
            0,
            1,
            2,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE
        };

        for (int i = 0; i < strs.length; i++) {
            String s = strs[i];
            for (int j = 0; j < suffixes.length; j++) {
                String suffix = suffixes[j];
                for (int a = 0; a < ints.length; a++) {
                    for (int b = 0; b < ints.length; b++) {
                        WordUtils.abbreviate(s, ints[a], ints[b], suffix);
                    }
                }

                if (s != null) {
                    int len = s.length();
                    WordUtils.abbreviate(s, -1, -1, suffix);
                    WordUtils.abbreviate(s, 0, len, suffix);
                    WordUtils.abbreviate(s, len, len, suffix);
                    WordUtils.abbreviate(s, len + 1, len + 1, suffix);
                    WordUtils.abbreviate(s, 0, len + 1, suffix);
                    WordUtils.abbreviate(s, len / 2, len / 2, suffix);
                    WordUtils.abbreviate(s, len / 2, -1, suffix);
                    WordUtils.abbreviate(s, len / 2, Math.max(0, len / 2 - 1), suffix);

                    int firstSpace = s.indexOf(' ');
                    if (firstSpace >= 0) {
                        WordUtils.abbreviate(s, firstSpace, firstSpace, suffix);
                        WordUtils.abbreviate(s, firstSpace, firstSpace + 1, suffix);
                        WordUtils.abbreviate(s, Math.max(0, firstSpace - 1), firstSpace, suffix);
                    }
                }
            }
        }
    }
}