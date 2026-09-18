package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(256);
        String s2 = data.consumeAsciiString(256);
        String s3 = data.consumeRemainingAsString();

        String str;
        switch (data.consumeInt(0, 5)) {
            case 0:
                str = null;
                break;
            case 1:
                str = "";
                break;
            case 2:
                str = s1;
                break;
            case 3:
                str = s2;
                break;
            case 4:
                str = s3;
                break;
            default:
                str = s1 + " " + s2;
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
                appendToEnd = data.consumeString(32);
                break;
            case 3:
                appendToEnd = data.consumeAsciiString(32);
                break;
            default:
                appendToEnd = "...";
                break;
        }

        int lowerAny = data.consumeInt();
        int upperAny = data.consumeInt();

        int len = str == null ? 0 : str.length();
        int nearLower = data.consumeInt(-2, len + 2);
        int nearUpper = data.consumeInt(-2, len + 2);

        WordUtils.abbreviate(str, lowerAny, upperAny, appendToEnd);
        WordUtils.abbreviate(str, nearLower, nearUpper, appendToEnd);
        WordUtils.abbreviate(str, 0, -1, appendToEnd);
        WordUtils.abbreviate(str, 0, 0, appendToEnd);
        WordUtils.abbreviate(str, 1, 1, appendToEnd);
        WordUtils.abbreviate(str, len, len, appendToEnd);
        WordUtils.abbreviate(str, len + 1, len + 1, appendToEnd);
        WordUtils.abbreviate(str, len, -1, appendToEnd);
        WordUtils.abbreviate(str, -1, len, appendToEnd);
        WordUtils.abbreviate(str, nearUpper, nearLower, appendToEnd);

        if (str != null) {
            String withLeadingSpace = " " + str;
            String withTrailingSpace = str + " ";
            String withInnerSpace = str + " " + appendToEnd;
            String doubleSpaced = str + "  " + s2;

            WordUtils.abbreviate(withLeadingSpace, lowerAny, upperAny, appendToEnd);
            WordUtils.abbreviate(withTrailingSpace, nearLower, nearUpper, appendToEnd);
            WordUtils.abbreviate(withInnerSpace, 0, len, appendToEnd);
            WordUtils.abbreviate(doubleSpaced, 1, len + 1, appendToEnd);
        }
    }
}