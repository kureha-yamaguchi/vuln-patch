package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(128);
        String s2 = data.consumeAsciiString(128);
        String s3 = data.consumeRemainingAsString();

        String append;
        switch ((s2.length() + s3.length()) % 5) {
            case 0:
                append = null;
                break;
            case 1:
                append = "";
                break;
            case 2:
                append = "...";
                break;
            case 3:
                append = s2;
                break;
            default:
                append = s3;
                break;
        }

        String[] strs = new String[] {
            null,
            "",
            s1,
            s2,
            s3,
            "a",
            "abc",
            "abc def",
            " abc",
            "abc ",
            s1 + " " + s2,
            s2 + " " + s3,
            s1 + s2
        };

        int any1 = s1.length() > 0 ? s1.charAt(0) : -2;
        int any2 = s2.length() > 0 ? -s2.charAt(0) - 1 : -3;
        int any3 = s3.length();
        int any4 = s1.length() + s2.length() + s3.length() + 1;

        for (int i = 0; i < strs.length; i++) {
            String str = strs[i];
            int len = str == null ? 0 : str.length();

            WordUtils.abbreviate(str, 0, -1, append);
            WordUtils.abbreviate(str, 0, 0, append);
            WordUtils.abbreviate(str, 0, 1, append);
            WordUtils.abbreviate(str, 1, 0, append);
            WordUtils.abbreviate(str, -1, -1, append);
            WordUtils.abbreviate(str, -1, 0, append);
            WordUtils.abbreviate(str, 0, -2, append);
            WordUtils.abbreviate(str, -2, -2, append);
            WordUtils.abbreviate(str, -5, -2, append);
            WordUtils.abbreviate(str, len, len, append);
            WordUtils.abbreviate(str, len + 1, len + 1, append);
            WordUtils.abbreviate(str, len + 1, 0, append);
            WordUtils.abbreviate(str, len + 1, -1, append);
            WordUtils.abbreviate(str, len + 2, len + 3, append);
            WordUtils.abbreviate(str, any1, any2, append);
            WordUtils.abbreviate(str, any2, any1, append);
            WordUtils.abbreviate(str, any3, any4, append);
            WordUtils.abbreviate(str, any4, any3, append);
        }
    }
}