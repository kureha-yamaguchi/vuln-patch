package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String consumed = data.consumeString(128);
        String ascii = data.consumeAsciiString(128);
        String remaining = data.consumeRemainingAsString();

        String str = data.consumeBoolean() ? null : consumed;
        String append = data.consumeBoolean() ? null : ascii;

        int anyLower = data.consumeInt();
        int anyUpper = data.consumeInt();

        if (str != null) {
            int len = str.length();

            WordUtils.abbreviate(str, anyLower, anyUpper, append);

            WordUtils.abbreviate(str, -1, -1, append);
            WordUtils.abbreviate(str, 0, -1, append);
            WordUtils.abbreviate(str, 0, 0, append);
            WordUtils.abbreviate(str, 0, len, append);
            WordUtils.abbreviate(str, 0, len + 1, append);
            WordUtils.abbreviate(str, len, len, append);
            WordUtils.abbreviate(str, len + 1, len + 1, append);
            WordUtils.abbreviate(str, len, -1, append);
            WordUtils.abbreviate(str, len + 1, -1, append);
            WordUtils.abbreviate(str, 1, 0, append);
            WordUtils.abbreviate(str, anyLower, -1, append);
            WordUtils.abbreviate(str, anyUpper, anyLower, append);

            String withLeadingSpace = " " + str;
            String withTrailingSpace = str + " ";
            String withInnerSpaces = str + " " + ascii;
            String onlySpaces = "   ";
            String noSpaces = str.replace(" ", "");

            WordUtils.abbreviate(withLeadingSpace, anyLower, anyUpper, append);
            WordUtils.abbreviate(withTrailingSpace, anyLower, anyUpper, append);
            WordUtils.abbreviate(withInnerSpaces, anyLower, anyUpper, append);
            WordUtils.abbreviate(onlySpaces, anyLower, anyUpper, append);
            WordUtils.abbreviate(noSpaces, anyLower, anyUpper, append);

            if (remaining.length() > 0) {
                String combined = str + remaining;
                String spacedCombined = str + " " + remaining;
                WordUtils.abbreviate(combined, anyLower, anyUpper, append);
                WordUtils.abbreviate(spacedCombined, anyLower, anyUpper, append);
                WordUtils.abbreviate(spacedCombined, 0, spacedCombined.length(), remaining);
                WordUtils.abbreviate(spacedCombined, spacedCombined.length(), -1, remaining);
            }

            if (len > 0) {
                int bounded = data.consumeInt(0, len);
                int boundedUpper = data.consumeInt(-1, len + 4);
                WordUtils.abbreviate(str, bounded, boundedUpper, append);
                WordUtils.abbreviate(withInnerSpaces, bounded, boundedUpper, remaining);
            }
        } else {
            WordUtils.abbreviate(null, anyLower, anyUpper, append);
            WordUtils.abbreviate(null, -1, -1, null);
            WordUtils.abbreviate(null, 0, 0, "");
        }

        WordUtils.abbreviate("", anyLower, anyUpper, append);
        WordUtils.abbreviate("", 0, -1, null);
        WordUtils.abbreviate(" ", anyLower, anyUpper, append);
        WordUtils.abbreviate("a", anyLower, anyUpper, append);
        WordUtils.abbreviate("a b", anyLower, anyUpper, append);
        WordUtils.abbreviate(ascii, anyLower, anyUpper, remaining);
        WordUtils.abbreviate(remaining, anyLower, anyUpper, ascii);
    }
}