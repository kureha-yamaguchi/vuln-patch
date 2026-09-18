package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String str;
        if (data.consumeBoolean()) {
            str = null;
        } else {
            if (data.consumeBoolean()) {
                str = data.consumeString(128);
            } else {
                str = data.consumeAsciiString(128);
            }
        }

        String appendToEnd;
        if (data.consumeBoolean()) {
            appendToEnd = null;
        } else {
            if (data.consumeBoolean()) {
                appendToEnd = data.consumeString(32);
            } else {
                appendToEnd = data.consumeAsciiString(32);
            }
        }

        int lower = data.consumeInt();
        int upper = data.consumeInt();

        WordUtils.abbreviate(str, lower, upper, appendToEnd);

        if (str != null) {
            int len = str.length();

            WordUtils.abbreviate(str, 0, -1, appendToEnd);
            WordUtils.abbreviate(str, 0, 0, appendToEnd);
            WordUtils.abbreviate(str, 0, len, appendToEnd);
            WordUtils.abbreviate(str, len, len, appendToEnd);
            WordUtils.abbreviate(str, len + 1, len + 1, appendToEnd);
            WordUtils.abbreviate(str, len + 1, -1, appendToEnd);
            WordUtils.abbreviate(str, -1, -1, appendToEnd);
            WordUtils.abbreviate(str, -1, len, appendToEnd);
            WordUtils.abbreviate(str, len, -1, appendToEnd);

            int boundedLower = data.consumeInt(-2, len + 2);
            int boundedUpper = data.consumeInt(-2, len + 2);
            WordUtils.abbreviate(str, boundedLower, boundedUpper, appendToEnd);

            String withSpacePrefix = " " + str;
            String withSpaceSuffix = str + " ";
            String withSpaceMiddle = len == 0 ? " " : str.substring(0, len / 2) + " " + str.substring(len / 2);

            WordUtils.abbreviate(withSpacePrefix, 0, withSpacePrefix.length(), appendToEnd);
            WordUtils.abbreviate(withSpaceSuffix, 0, withSpaceSuffix.length(), appendToEnd);
            WordUtils.abbreviate(withSpaceMiddle, boundedLower, boundedUpper, appendToEnd);

            if (len > 0) {
                int split = data.consumeInt(0, len);
                String prefix = str.substring(0, split);
                String suffix = str.substring(split);
                String spaced = prefix + " " + suffix;

                WordUtils.abbreviate(spaced, 0, spaced.length(), appendToEnd);
                WordUtils.abbreviate(spaced, split, split, appendToEnd);
                WordUtils.abbreviate(spaced, split, -1, appendToEnd);
                WordUtils.abbreviate(spaced, 0, split, appendToEnd);
            }
        } else {
            WordUtils.abbreviate(null, data.consumeInt(), data.consumeInt(), appendToEnd);
        }
    }
}