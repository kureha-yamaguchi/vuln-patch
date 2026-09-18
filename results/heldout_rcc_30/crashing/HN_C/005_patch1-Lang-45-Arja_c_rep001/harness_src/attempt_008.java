package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeRemainingAsString();

        String base;
        switch (data.consumeInt(0, 7)) {
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
                base = s3;
                break;
            case 5:
                base = " " + s1;
                break;
            case 6:
                base = s1 + " " + s2;
                break;
            default:
                base = s1 + " " + s2 + " " + s3;
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
                append = data.consumeString(16);
                break;
            case 3:
                append = data.consumeAsciiString(16);
                break;
            case 4:
                append = "...";
                break;
            default:
                append = " ";
                break;
        }

        int anyLower = data.consumeInt();
        int anyUpper = data.consumeInt();

        int len = base == null ? 0 : base.length();

        int[] lowers = new int[] {
            anyLower,
            anyUpper,
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
            anyUpper,
            anyLower,
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
            WordUtils.abbreviate(base, lowers[i], uppers[i], append);
        }

        if (base != null) {
            WordUtils.abbreviate(base, 0, len, append);
            WordUtils.abbreviate(base, 0, -1, append);
            WordUtils.abbreviate(base, len, len, append);
            WordUtils.abbreviate(base, len + 1, len, append);
            WordUtils.abbreviate(base, -1, len, append);
            WordUtils.abbreviate(base, -1, -1, append);

            String withLeadingSpace = " " + base;
            String withTrailingSpace = base + " ";
            String withInnerSpace = base + " " + base;

            WordUtils.abbreviate(withLeadingSpace, anyLower, anyUpper, append);
            WordUtils.abbreviate(withTrailingSpace, anyLower, anyUpper, append);
            WordUtils.abbreviate(withInnerSpace, anyLower, anyUpper, append);
            WordUtils.abbreviate(withInnerSpace, 0, withInnerSpace.length(), append);
            WordUtils.abbreviate(withInnerSpace, 1, withInnerSpace.length() - 1, append);
            WordUtils.abbreviate(withInnerSpace, withInnerSpace.length(), -1, append);
        }
    }
}