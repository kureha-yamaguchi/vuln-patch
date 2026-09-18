package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String primary = data.consumeString(128);
        String secondary = data.consumeAsciiString(128);
        String append1 = data.consumeString(32);
        String append2 = data.consumeAsciiString(32);

        int any1 = data.consumeInt();
        int any2 = data.consumeInt();
        int bounded1 = data.consumeInt(-256, 256);
        int bounded2 = data.consumeInt(-256, 256);
        boolean useNullStr = data.consumeBoolean();
        boolean useNullAppend = data.consumeBoolean();

        String spacedPrimary = primary;
        if (primary != null && primary.length() > 0) {
            int split = Math.abs(any1);
            if (primary.length() > 0) {
                split %= (primary.length() + 1);
            }
            spacedPrimary = primary.substring(0, split) + " " + primary.substring(split);
        }

        String doubled = primary + " " + secondary;
        String noSpace = primary.replace(" ", "") + secondary.replace(" ", "");
        String onlySpaces = data.consumeAsciiString(16).replaceAll("[^ ]", " ");
        String remaining = data.consumeRemainingAsString();

        String[] strs = new String[] {
            useNullStr ? null : primary,
            "",
            secondary,
            spacedPrimary,
            doubled,
            noSpace,
            onlySpaces,
            remaining
        };

        String[] appends = new String[] {
            useNullAppend ? null : append1,
            "",
            append2,
            " ",
            remaining
        };

        int lenPrimary = primary == null ? 0 : primary.length();
        int lenSecondary = secondary == null ? 0 : secondary.length();

        int[] lowers = new int[] {
            any1,
            any2,
            bounded1,
            bounded2,
            -1,
            0,
            1,
            lenPrimary,
            lenPrimary + 1,
            lenSecondary
        };

        int[] uppers = new int[] {
            any2,
            any1,
            bounded2,
            bounded1,
            -1,
            0,
            1,
            lenPrimary,
            lenPrimary + 1,
            lenSecondary
        };

        for (int i = 0; i < strs.length; i++) {
            String s = strs[i];
            for (int j = 0; j < appends.length; j++) {
                String a = appends[j];

                WordUtils.abbreviate(s, 0, -1, a);
                WordUtils.abbreviate(s, 0, 0, a);
                WordUtils.abbreviate(s, 1, 1, a);

                for (int l = 0; l < lowers.length; l++) {
                    for (int u = 0; u < uppers.length; u++) {
                        WordUtils.abbreviate(s, lowers[l], uppers[u], a);
                    }
                }

                if (s != null) {
                    int slen = s.length();
                    WordUtils.abbreviate(s, slen, slen, a);
                    WordUtils.abbreviate(s, slen + 1, slen, a);
                    WordUtils.abbreviate(s, 0, slen, a);
                    WordUtils.abbreviate(s, 0, slen + 1, a);
                    WordUtils.abbreviate(s, -1, slen, a);
                    WordUtils.abbreviate(s, -1, -1, a);
                }
            }
        }
    }
}