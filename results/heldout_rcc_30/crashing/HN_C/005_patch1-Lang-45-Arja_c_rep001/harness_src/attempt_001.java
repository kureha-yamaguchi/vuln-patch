package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String str;
        if (data.consumeBoolean()) {
            str = null;
        } else if (data.consumeBoolean()) {
            str = data.consumeString(256);
        } else {
            str = data.consumeAsciiString(256);
        }

        String appendToEnd;
        if (data.consumeBoolean()) {
            appendToEnd = null;
        } else if (data.consumeBoolean()) {
            appendToEnd = data.consumeString(64);
        } else {
            appendToEnd = data.consumeAsciiString(64);
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
            WordUtils.abbreviate(str, -1, len, appendToEnd);
            WordUtils.abbreviate(str, 0, len + 1, appendToEnd);
            WordUtils.abbreviate(str, len / 2, -1, appendToEnd);
            WordUtils.abbreviate(str, len / 2, len / 2, appendToEnd);
            WordUtils.abbreviate(str, len / 2, Math.max(0, len / 2 - 1), appendToEnd);

            if (len > 0) {
                WordUtils.abbreviate(str, 1, len - 1, appendToEnd);
            }

            String withSpaces = " " + str + " ";
            WordUtils.abbreviate(withSpaces, lower, upper, appendToEnd);
            WordUtils.abbreviate(withSpaces, 0, -1, appendToEnd);
            WordUtils.abbreviate(withSpaces, 1, len, appendToEnd);

            String prefixSpace = str + " tail";
            WordUtils.abbreviate(prefixSpace, 0, len, appendToEnd);
            WordUtils.abbreviate(prefixSpace, len, len + 5, appendToEnd);

            String internalSpace = "aa " + str;
            WordUtils.abbreviate(internalSpace, 0, 2, appendToEnd);
            WordUtils.abbreviate(internalSpace, 0, 3, appendToEnd);
            WordUtils.abbreviate(internalSpace, 1, internalSpace.length(), appendToEnd);
        } else {
            WordUtils.abbreviate(null, 0, -1, appendToEnd);
        }

        int extraCalls = data.remainingBytes() > 0 ? data.consumeInt(1, 8) : 1;
        for (int i = 0; i < extraCalls; i++) {
            String s;
            if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                s = null;
            } else if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                s = data.consumeAsciiString(128);
            } else {
                s = data.consumeString(128);
            }

            String end;
            if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                end = null;
            } else if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                end = data.consumeAsciiString(32);
            } else {
                end = data.consumeString(32);
            }

            int l = data.remainingBytes() > 0 ? data.consumeInt() : i - 2;
            int u = data.remainingBytes() > 0 ? data.consumeInt() : i;

            WordUtils.abbreviate(s, l, u, end);

            if (s != null) {
                int slen = s.length();
                WordUtils.abbreviate(s, 0, slen, end);
                WordUtils.abbreviate(s, 0, Math.max(0, slen - 1), end);
                WordUtils.abbreviate(s, slen, -1, end);
            }
        }
    }
}