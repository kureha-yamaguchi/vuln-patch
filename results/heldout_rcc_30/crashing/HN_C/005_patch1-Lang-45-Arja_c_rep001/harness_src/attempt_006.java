package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String base = data.consumeString(64);
        String ascii = data.consumeAsciiString(64);
        String tail = data.consumeRemainingAsString();

        String str = data.consumeBoolean() ? null : base;
        String appendToEnd = data.consumeBoolean() ? null : ascii;

        int anyLower = data.consumeInt();
        int anyUpper = data.consumeInt();

        int lenBase = base == null ? 0 : base.length();
        int lenAscii = ascii == null ? 0 : ascii.length();
        int lenTail = tail == null ? 0 : tail.length();

        String[] strings = new String[] {
            str,
            "",
            " ",
            base,
            ascii,
            tail,
            base + " " + ascii,
            ascii + " " + tail,
            " " + base,
            base + " ",
            base + "  " + ascii,
            base + "\t" + ascii,
            base + "\n" + ascii
        };

        String[] appends = new String[] {
            appendToEnd,
            "",
            " ",
            ascii,
            tail,
            "...",
            base
        };

        int[] lowers = new int[] {
            anyLower,
            anyUpper,
            -1,
            0,
            1,
            lenBase - 1,
            lenBase,
            lenBase + 1,
            lenAscii - 1,
            lenAscii,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE
        };

        int[] uppers = new int[] {
            anyUpper,
            anyLower,
            -1,
            0,
            1,
            lenBase - 1,
            lenBase,
            lenBase + 1,
            lenTail - 1,
            lenTail,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE
        };

        for (int i = 0; i < strings.length; i++) {
            String s = strings[i];
            String a = appends[i % appends.length];
            int lower = lowers[i % lowers.length];
            int upper = uppers[(i * 3) % uppers.length];
            WordUtils.abbreviate(s, lower, upper, a);
        }

        WordUtils.abbreviate(str, anyLower, anyUpper, appendToEnd);
        WordUtils.abbreviate(base, 0, -1, appendToEnd);
        WordUtils.abbreviate(base, 0, 0, appendToEnd);
        WordUtils.abbreviate(base, 1, 0, appendToEnd);
        WordUtils.abbreviate(base, -1, -1, appendToEnd);
        WordUtils.abbreviate(base, lenBase, lenBase, appendToEnd);
        WordUtils.abbreviate(base, lenBase + 1, lenBase + 1, appendToEnd);
        WordUtils.abbreviate(base + " " + tail, 0, lenBase, appendToEnd);
        WordUtils.abbreviate(base + " " + tail, lenBase, lenBase + 1, appendToEnd);
        WordUtils.abbreviate(" " + base, 0, 1, appendToEnd);
        WordUtils.abbreviate(base + " ", 0, lenBase + 1, appendToEnd);
    }
}