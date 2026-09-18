package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
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
                str = s1 + " " + s2;
                break;
            default:
                str = s3;
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
                appendToEnd = data.consumeString(16);
                break;
            case 3:
                appendToEnd = data.consumeAsciiString(16);
                break;
            default:
                appendToEnd = s2;
                break;
        }

        int lower = data.consumeInt();
        int upper = data.consumeInt();

        WordUtils.abbreviate(str, lower, upper, appendToEnd);

        if (str != null) {
            int len = str.length();

            WordUtils.abbreviate(str, 0, -1, appendToEnd);
            WordUtils.abbreviate(str, 0, 0, appendToEnd);
            WordUtils.abbreviate(str, len, len, appendToEnd);
            WordUtils.abbreviate(str, len + 1, len + 1, appendToEnd);
            WordUtils.abbreviate(str, 0, len, appendToEnd);
            WordUtils.abbreviate(str, 0, len + 1, appendToEnd);
            WordUtils.abbreviate(str, len / 2, -1, appendToEnd);
            WordUtils.abbreviate(str, len / 2, len / 2, appendToEnd);
            WordUtils.abbreviate(str, len / 2, Math.max(0, len / 2 - 1), appendToEnd);
            WordUtils.abbreviate(str, -1, -1, appendToEnd);
            WordUtils.abbreviate(str, -1, 0, appendToEnd);
            WordUtils.abbreviate(str, 0, -2, appendToEnd);
            WordUtils.abbreviate(str, Integer.MIN_VALUE, Integer.MIN_VALUE, appendToEnd);
            WordUtils.abbreviate(str, Integer.MAX_VALUE, Integer.MAX_VALUE, appendToEnd);
            WordUtils.abbreviate(str, Integer.MIN_VALUE, Integer.MAX_VALUE, appendToEnd);

            String withLeadingSpace = " " + str;
            String withTrailingSpace = str + " ";
            String withInternalSpace = str + " " + s1;
            String spacesOnly = "   ";

            WordUtils.abbreviate(withLeadingSpace, lower, upper, appendToEnd);
            WordUtils.abbreviate(withTrailingSpace, lower, upper, appendToEnd);
            WordUtils.abbreviate(withInternalSpace, 0, len, appendToEnd);
            WordUtils.abbreviate(withInternalSpace, 1, len + 1, appendToEnd);
            WordUtils.abbreviate(spacesOnly, 0, 1, appendToEnd);
            WordUtils.abbreviate(spacesOnly, 1, 2, appendToEnd);
        }

        if (appendToEnd != null) {
            WordUtils.abbreviate(str, lower, upper, "");
            WordUtils.abbreviate(str, lower, upper, appendToEnd + s1);
        }

        String combined = s1 + " " + s2 + " " + s3;
        WordUtils.abbreviate(combined, data.consumeInt(), data.consumeInt(), data.consumeString(8));
        WordUtils.abbreviate(combined, 0, -1, null);
        WordUtils.abbreviate(combined, 1, 1, ".");
        WordUtils.abbreviate(combined, 1, 2, "...");
        WordUtils.abbreviate(combined, combined.length(), combined.length() + 10, data.consumeAsciiString(8));
    }
}