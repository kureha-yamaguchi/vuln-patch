package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String base = data.consumeString(256);
        String ascii = data.consumeAsciiString(256);
        String tail = data.consumeRemainingAsString();

        String str;
        switch (data.consumeInt(0, 7)) {
            case 0:
                str = null;
                break;
            case 1:
                str = "";
                break;
            case 2:
                str = base;
                break;
            case 3:
                str = ascii;
                break;
            case 4:
                str = base + " " + ascii;
                break;
            case 5:
                str = " " + base;
                break;
            case 6:
                str = ascii + " " + tail;
                break;
            default:
                str = tail;
                break;
        }

        String appendToEnd;
        switch (data.consumeInt(0, 5)) {
            case 0:
                appendToEnd = null;
                break;
            case 1:
                appendToEnd = "";
                break;
            case 2:
                appendToEnd = data.consumeString(64);
                break;
            case 3:
                appendToEnd = data.consumeAsciiString(64);
                break;
            case 4:
                appendToEnd = "...";
                break;
            default:
                appendToEnd = " " + data.consumeAsciiString(8);
                break;
        }

        int lower = data.consumeInt();
        int upper = data.consumeInt();

        WordUtils.abbreviate(str, lower, upper, appendToEnd);

        String effective = str == null ? null : str;
        int len = effective == null ? 0 : effective.length();

        if (effective != null) {
            WordUtils.abbreviate(effective, 0, -1, appendToEnd);
            WordUtils.abbreviate(effective, 0, 0, appendToEnd);
            WordUtils.abbreviate(effective, 0, len, appendToEnd);
            WordUtils.abbreviate(effective, len, len, appendToEnd);
            WordUtils.abbreviate(effective, len + 1, len + 1, appendToEnd);
            WordUtils.abbreviate(effective, -1, len, appendToEnd);
            WordUtils.abbreviate(effective, -1, -1, appendToEnd);
            WordUtils.abbreviate(effective, 1, 0, appendToEnd);

            int mid = len / 2;
            WordUtils.abbreviate(effective, mid, mid, appendToEnd);
            WordUtils.abbreviate(effective, mid, mid + 1, appendToEnd);

            if (len > 0) {
                WordUtils.abbreviate(effective, 0, len - 1, appendToEnd);
                WordUtils.abbreviate(effective, len - 1, len, appendToEnd);
            }
        } else {
            WordUtils.abbreviate(null, 0, -1, appendToEnd);
        }
    }
}