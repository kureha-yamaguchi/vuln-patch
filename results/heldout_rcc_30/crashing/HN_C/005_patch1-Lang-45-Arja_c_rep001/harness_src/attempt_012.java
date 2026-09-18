package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String str1 = data.consumeString(64);
        String str2 = data.consumeAsciiString(64);
        String tail = data.consumeRemainingAsString();

        String append1 = data.consumeBoolean() ? null : data.consumeString(16);
        String append2 = data.consumeBoolean() ? null : data.consumeAsciiString(16);

        int lower1 = data.consumeInt();
        int upper1 = data.consumeInt();
        int lower2 = data.consumeInt();
        int upper2 = data.consumeInt();

        WordUtils.abbreviate(null, lower1, upper1, append1);
        WordUtils.abbreviate("", lower1, upper1, append1);

        WordUtils.abbreviate(str1, lower1, upper1, append1);
        WordUtils.abbreviate(str2, lower2, upper2, append2);

        String withSpace = str1 + " " + str2;
        String leadingSpace = " " + str1;
        String trailingSpace = str2 + " ";
        String denseSpaces = str1 + "  " + tail;

        WordUtils.abbreviate(withSpace, lower1, upper1, append1);
        WordUtils.abbreviate(leadingSpace, lower2, upper2, append2);
        WordUtils.abbreviate(trailingSpace, lower1, -1, append1);
        WordUtils.abbreviate(denseSpaces, lower2, upper1, append2);

        int len1 = str1.length();
        int len2 = withSpace.length();

        WordUtils.abbreviate(str1, 0, len1, append1);
        WordUtils.abbreviate(str1, 0, Math.max(0, len1 - 1), append1);
        WordUtils.abbreviate(str1, len1, -1, append1);
        WordUtils.abbreviate(str1, len1 + 1, 0, append1);

        WordUtils.abbreviate(withSpace, 0, 0, append2);
        WordUtils.abbreviate(withSpace, 0, len2, append2);
        WordUtils.abbreviate(withSpace, 1, Math.max(0, len2 - 1), append2);
        WordUtils.abbreviate(withSpace, len2 + 1, len2, append2);

        WordUtils.abbreviate("a", lower1, upper1, append1);
        WordUtils.abbreviate("a b", lower2, upper2, append2);
    }
}