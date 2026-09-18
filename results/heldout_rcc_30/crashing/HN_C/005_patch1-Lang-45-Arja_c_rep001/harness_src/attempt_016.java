package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String baseStr = data.consumeBoolean() ? null : data.consumeString(128);
        String append = data.consumeBoolean() ? null : data.consumeString(32);

        int rawLower = data.consumeInt();
        int rawUpper = data.consumeInt();

        WordUtils.abbreviate(baseStr, rawLower, rawUpper, append);

        String altStr;
        if (data.consumeBoolean()) {
            altStr = data.consumeAsciiString(128);
        } else {
            altStr = data.consumeString(128);
        }
        if (data.consumeBoolean()) {
            altStr = "";
        }
        if (data.consumeBoolean()) {
            altStr = " ";
        }
        if (data.consumeBoolean()) {
            altStr = altStr + " " + data.consumeAsciiString(32);
        }

        String altAppend = data.consumeBoolean() ? null : data.consumeAsciiString(16);

        int len = altStr == null ? 0 : altStr.length();

        int[] lowers = new int[] {
            rawLower,
            rawUpper,
            -1,
            0,
            1,
            len - 1,
            len,
            len + 1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE
        };

        int[] uppers = new int[] {
            rawUpper,
            rawLower,
            -1,
            0,
            1,
            len - 1,
            len,
            len + 1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE
        };

        for (int i = 0; i < lowers.length; i++) {
            WordUtils.abbreviate(altStr, lowers[i], uppers[i], altAppend);
        }

        String remainingStr = data.consumeRemainingAsString();
        String append2 = data.consumeBoolean() ? null : "";
        int remLen = remainingStr == null ? 0 : remainingStr.length();

        WordUtils.abbreviate(remainingStr, 0, remLen, append2);
        WordUtils.abbreviate(remainingStr, 0, -1, append2);
        WordUtils.abbreviate(remainingStr, remLen, remLen, append2);
        WordUtils.abbreviate(remainingStr, remLen + 1, remLen - 1, append2);
        WordUtils.abbreviate(remainingStr, -1, remLen, append2);
    }
}