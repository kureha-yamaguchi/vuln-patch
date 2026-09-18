package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String primary;
        int strChoice = data.consumeInt(0, 7);
        switch (strChoice) {
            case 0:
                primary = null;
                break;
            case 1:
                primary = "";
                break;
            case 2:
                primary = data.consumeString(64);
                break;
            case 3:
                primary = data.consumeAsciiString(64);
                break;
            case 4:
                primary = " " + data.consumeAsciiString(32);
                break;
            case 5:
                primary = data.consumeAsciiString(32) + " " + data.consumeAsciiString(32);
                break;
            case 6:
                primary = data.consumeAsciiString(16) + "  " + data.consumeAsciiString(16) + " " + data.consumeAsciiString(16);
                break;
            default:
                primary = data.consumeRemainingAsString();
                break;
        }

        String append;
        int appendChoice = data.consumeInt(0, 5);
        switch (appendChoice) {
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
                append = " " + data.consumeAsciiString(8);
                break;
        }

        int lower = data.consumeInt();
        int upper = data.consumeInt();

        WordUtils.abbreviate(primary, lower, upper, append);

        int len = primary == null ? 0 : primary.length();

        WordUtils.abbreviate(primary, -1, -1, append);
        WordUtils.abbreviate(primary, 0, -1, append);
        WordUtils.abbreviate(primary, 0, 0, append);
        WordUtils.abbreviate(primary, 1, 0, append);
        WordUtils.abbreviate(primary, 0, 1, append);
        WordUtils.abbreviate(primary, len, len, append);
        WordUtils.abbreviate(primary, len + 1, len + 1, append);
        WordUtils.abbreviate(primary, len + 1, -1, append);
        WordUtils.abbreviate(primary, Integer.MIN_VALUE, Integer.MIN_VALUE, append);
        WordUtils.abbreviate(primary, Integer.MIN_VALUE, Integer.MAX_VALUE, append);
        WordUtils.abbreviate(primary, Integer.MAX_VALUE, Integer.MIN_VALUE, append);
        WordUtils.abbreviate(primary, Integer.MAX_VALUE, Integer.MAX_VALUE, append);

        if (primary != null) {
            WordUtils.abbreviate(primary, 0, len, null);
            WordUtils.abbreviate(primary, 0, Math.max(0, len - 1), "");
            WordUtils.abbreviate(primary, 0, Math.max(0, len - 1), "...");
            WordUtils.abbreviate(primary, Math.max(0, len / 2), len, append);
            WordUtils.abbreviate(primary, Math.max(0, len / 2), Math.max(0, len / 2), append);
            WordUtils.abbreviate(primary, Math.max(0, len / 2), Math.max(0, len / 2 - 1), append);
        }

        if (data.remainingBytes() > 0) {
            String extra = data.consumeRemainingAsString();
            WordUtils.abbreviate(extra, data.consumeInt(), data.consumeInt(), append);
        }
    }
}