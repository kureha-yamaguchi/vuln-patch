package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int lowerAny = data.consumeInt();
        int upperAny = data.consumeInt();
        int lowerSmall = data.consumeInt(-16, 128);
        int upperSmall = data.consumeInt(-16, 128);
        byte marker = data.consumeByte();
        boolean nullStr = data.consumeBoolean();
        boolean nullAppend = data.consumeBoolean();

        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        byte[] bytes = data.consumeBytes(32);
        String rest = data.consumeRemainingAsString();

        String fromBytes = new String(bytes);

        String primary = nullStr ? null : s1;
        String appendPrimary = nullAppend ? null : s2;

        String[] strings = new String[] {
            primary,
            "",
            " ",
            "  ",
            s1,
            s2,
            rest,
            fromBytes,
            s1 + s2,
            s1 + " " + s2,
            s2 + " " + s1,
            " " + s1,
            s1 + " ",
            " " + s1 + " ",
            s1 + "  " + rest,
            rest + " " + fromBytes,
            String.valueOf((char) marker),
            String.valueOf((char) marker) + s1,
            s1 + String.valueOf((char) marker),
            StringUtils.defaultString(s1) + " " + StringUtils.defaultString(rest)
        };

        String[] appends = new String[] {
            appendPrimary,
            null,
            "",
            "...",
            " ",
            s1,
            s2,
            rest,
            fromBytes,
            String.valueOf((char) marker)
        };

        for (int i = 0; i < strings.length; i++) {
            String str = strings[i];
            int len = (str == null) ? 0 : str.length();

            int[] lowers = new int[] {
                lowerAny,
                upperAny,
                lowerSmall,
                upperSmall,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                -2,
                -1,
                0,
                1,
                len - 1,
                len,
                len + 1
            };

            int[] uppers = new int[] {
                upperAny,
                lowerAny,
                upperSmall,
                lowerSmall,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                -2,
                -1,
                0,
                1,
                len - 1,
                len,
                len + 1
            };

            for (int l = 0; l < lowers.length; l++) {
                for (int u = 0; u < uppers.length; u++) {
                    for (int a = 0; a < appends.length; a++) {
                        WordUtils.abbreviate(str, lowers[l], uppers[u], appends[a]);
                    }
                }
            }
        }
    }
}