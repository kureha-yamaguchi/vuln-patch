package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String base = data.consumeString(128);
        String ascii = data.consumeAsciiString(128);
        String append1 = data.consumeString(32);
        String append2 = data.consumeAsciiString(32);

        int anyLower = data.consumeInt();
        int anyUpper = data.consumeInt();
        int choice = data.consumeInt(0, 9);

        String tail = data.consumeRemainingAsString();

        String str;
        switch (choice) {
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
                str = tail;
                break;
            case 5:
                str = base + ascii;
                break;
            case 6:
                str = base + " " + ascii;
                break;
            case 7:
                str = " " + base;
                break;
            case 8:
                str = ascii + " ";
                break;
            default:
                str = "A";
                break;
        }

        String appendToEnd;
        switch (data.consumeInt(0, 4)) {
            case 0:
                appendToEnd = null;
                break;
            case 1:
                appendToEnd = "";
                break;
            case 2:
                appendToEnd = append1;
                break;
            case 3:
                appendToEnd = append2;
                break;
            default:
                appendToEnd = "...";
                break;
        }

        WordUtils.abbreviate(str, anyLower, anyUpper, appendToEnd);

        if (str != null) {
            int len = str.length();

            WordUtils.abbreviate(str, 0, -1, appendToEnd);
            WordUtils.abbreviate(str, 0, len, appendToEnd);
            WordUtils.abbreviate(str, 0, len + 1, appendToEnd);
            WordUtils.abbreviate(str, -1, -1, appendToEnd);
            WordUtils.abbreviate(str, -1, len, appendToEnd);
            WordUtils.abbreviate(str, len, len, appendToEnd);
            WordUtils.abbreviate(str, len + 1, len, appendToEnd);
            WordUtils.abbreviate(str, len + 1, -1, appendToEnd);
            WordUtils.abbreviate(str, len + 2, len + 1, appendToEnd);
            WordUtils.abbreviate(str, len + 10, 0, appendToEnd);

            String noSpace = str.replace(" ", "");
            if (noSpace.length() == 0) {
                noSpace = "X";
            }
            int n = noSpace.length();

            WordUtils.abbreviate(noSpace, 0, -1, appendToEnd);
            WordUtils.abbreviate(noSpace, n + 1, -1, appendToEnd);
            WordUtils.abbreviate(noSpace, n + 1, 0, appendToEnd);
            WordUtils.abbreviate(noSpace, n + 1, n, appendToEnd);
            WordUtils.abbreviate(noSpace, n + 5, n + 4, appendToEnd);

            String single = "Z";
            WordUtils.abbreviate(single, 2, -1, appendToEnd);
            WordUtils.abbreviate(single, 2, 0, appendToEnd);
            WordUtils.abbreviate(single, 100, -1, appendToEnd);

            String withSpace = noSpace + " " + ascii;
            WordUtils.abbreviate(withSpace, n + 1, -1, appendToEnd);
            WordUtils.abbreviate(withSpace, n + 1, 0, appendToEnd);
        }
    }
}