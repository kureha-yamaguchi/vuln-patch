package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeBoolean() ? null : data.consumeString(128);
        String s2 = data.consumeBoolean() ? null : data.consumeAsciiString(128);
        String s3 = data.consumeBoolean() ? null : data.consumeRemainingAsString();

        String append1 = data.consumeBoolean() ? null : data.consumeString(32);
        String append2 = data.consumeBoolean() ? null : data.consumeAsciiString(32);

        int anyLower1 = data.consumeInt();
        int anyUpper1 = data.consumeInt();
        int anyLower2 = data.consumeInt();
        int anyUpper2 = data.consumeInt();

        WordUtils.abbreviate(s1, anyLower1, anyUpper1, append1);
        WordUtils.abbreviate(s2, anyLower2, anyUpper2, append2);

        WordUtils.abbreviate("", 0, 0, append1);
        WordUtils.abbreviate("", -1, -1, null);

        if (s1 != null) {
            int len = s1.length();
            int lowerNear = data.consumeInt(-len - 5, len + 5);
            int upperNear = data.consumeInt(-len - 5, len + 5);
            WordUtils.abbreviate(s1, lowerNear, upperNear, append2);
            WordUtils.abbreviate(s1, 0, -1, append1);
            WordUtils.abbreviate(s1, len, len, append1);
            WordUtils.abbreviate(s1, len + 1, len + 1, append2);
            WordUtils.abbreviate(s1, -1, len, "");
            WordUtils.abbreviate(s1, 1, 0, null);
        }

        if (s2 != null) {
            int len = s2.length();
            String withLeadingSpace = " " + s2;
            String withInternalSpaces = s2 + " " + data.consumeAsciiString(32) + " " + data.consumeString(32);
            String noSpaces = s2.replace(" ", "") + data.consumeAsciiString(16).replace(" ", "");

            WordUtils.abbreviate(withLeadingSpace, data.consumeInt(-len - 3, len + 3), data.consumeInt(-len - 3, len + 3), append1);
            WordUtils.abbreviate(withInternalSpaces, data.consumeInt(-withInternalSpaces.length() - 3, withInternalSpaces.length() + 3),
                    data.consumeInt(-withInternalSpaces.length() - 3, withInternalSpaces.length() + 3), append2);
            WordUtils.abbreviate(noSpaces, data.consumeInt(-noSpaces.length() - 3, noSpaces.length() + 3),
                    data.consumeInt(-noSpaces.length() - 3, noSpaces.length() + 3), "");
        }

        if (s3 != null) {
            int len = s3.length();
            WordUtils.abbreviate(s3, data.consumeInt(-len - 7, len + 7), -1, null);
            WordUtils.abbreviate(" " + s3 + " ", data.consumeInt(-len - 9, len + 9), data.consumeInt(-len - 9, len + 9), append1);
        }

        String boundary = data.consumeBoolean() ? " " : data.consumeAsciiString(1);
        boundary = boundary + data.consumeString(4) + (data.consumeBoolean() ? " " : "");
        int blen = boundary.length();
        WordUtils.abbreviate(boundary, data.consumeInt(-blen - 2, blen + 2), data.consumeInt(-blen - 2, blen + 2), data.consumeBoolean() ? null : "...");

        byte[] extra = data.consumeRemainingAsBytes();
        String tail = new String(extra);
        int tlen = tail.length();
        WordUtils.abbreviate(tail, data.consumeInt(-tlen - 2, tlen + 2), data.consumeInt(-tlen - 2, tlen + 2), append2);
    }
}