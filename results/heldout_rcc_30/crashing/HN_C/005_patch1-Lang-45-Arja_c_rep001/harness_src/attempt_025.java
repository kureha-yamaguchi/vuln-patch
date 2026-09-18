package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String a = data.consumeString(64);
        String b = data.consumeAsciiString(64);
        String c = data.consumeRemainingAsString();

        String base;
        switch (data.consumeInt(0, 7)) {
            case 0:
                base = null;
                break;
            case 1:
                base = "";
                break;
            case 2:
                base = a;
                break;
            case 3:
                base = b;
                break;
            case 4:
                base = c;
                break;
            case 5:
                base = a + " " + b;
                break;
            case 6:
                base = " " + a;
                break;
            default:
                base = a + " " + b + " " + c;
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
                append = b;
                break;
        }

        int lower = data.consumeInt();
        int upper = data.consumeInt();

        WordUtils.abbreviate(base, lower, upper, append);

        String nonNull;
        switch (data.consumeInt(0, 5)) {
            case 0:
                nonNull = "abc";
                break;
            case 1:
                nonNull = "abcdef";
                break;
            case 2:
                nonNull = "abc def";
                break;
            case 3:
                nonNull = a.length() == 0 ? "x" : a;
                break;
            case 4:
                nonNull = b.length() == 0 ? "y z" : b;
                break;
            default:
                nonNull = (a + b).length() == 0 ? "qrs" : (a + b);
                break;
        }

        int len = nonNull.length();

        WordUtils.abbreviate(nonNull, len + 1, -1, append);
        WordUtils.abbreviate(nonNull, len + 10, len, append);
        WordUtils.abbreviate(nonNull, len + 100, 0, append);
        WordUtils.abbreviate(nonNull, Integer.MAX_VALUE, -1, append);

        WordUtils.abbreviate(nonNull, -1, -2, append);
        WordUtils.abbreviate(nonNull, -5, -1, append);
        WordUtils.abbreviate(nonNull, Integer.MIN_VALUE, Integer.MIN_VALUE + 1, append);

        WordUtils.abbreviate("abc", 10, -1, append);
        WordUtils.abbreviate("abc", 4, 2, append);
        WordUtils.abbreviate("abc", 100, 100, append);
        WordUtils.abbreviate("abc", -1, -1, append);
        WordUtils.abbreviate("abc", -10, -5, append);

        WordUtils.abbreviate("abc def", 10, -1, append);
        WordUtils.abbreviate("abc def", 100, 1, append);
        WordUtils.abbreviate("abc def", -3, -1, append);

        if (len > 0) {
            WordUtils.abbreviate(nonNull, len, -1, append);
            WordUtils.abbreviate(nonNull, len, len - 1, append);
            WordUtils.abbreviate(nonNull, 0, len, append);
            WordUtils.abbreviate(nonNull, 0, len + 1, append);
        }
    }
}