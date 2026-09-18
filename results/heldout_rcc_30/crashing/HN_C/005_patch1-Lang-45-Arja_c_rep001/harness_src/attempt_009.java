package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String a = data.consumeString(64);
        String b = data.consumeAsciiString(64);
        String c = data.consumeRemainingAsString();

        String append;
        switch (data.consumeInt(0, 4)) {
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
                append = "...";
                break;
            default:
                append = " ";
                break;
        }

        String[] inputs = new String[] {
            a,
            b,
            c,
            a + b,
            a + " " + b,
            b + " " + c,
            "A",
            "A " + a,
            a.length() == 0 ? "B" : a,
            b.length() == 0 ? "C" : b
        };

        for (String str : inputs) {
            if (str == null) {
                WordUtils.abbreviate(null, data.consumeInt(), data.consumeInt(), append);
                continue;
            }

            WordUtils.abbreviate(str, 0, -1, append);
            WordUtils.abbreviate(str, 0, str.length(), append);
            WordUtils.abbreviate(str, 0, Math.max(0, str.length() - 1), append);
            WordUtils.abbreviate(str, data.consumeInt(), data.consumeInt(), append);

            if (str.length() > 0) {
                int lower = str.length() + 1 + Math.abs(data.consumeByte());
                int upper = data.consumeBoolean() ? 0 : Math.max(0, str.length() - 1);
                WordUtils.abbreviate(str, lower, upper, append);
            }
        }
    }
}