package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        int padByte = data.consumeByte();
        char padChar = (char) (padByte & 0xFF);

        int width1 = data.consumeInt();
        int width2 = data.consumeInt(-32, 256);
        int width3 = data.consumeInt(-1, 64);

        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeRemainingAsString();

        Object obj1;
        switch (data.consumeInt(0, 5)) {
            case 0:
                obj1 = null;
                break;
            case 1:
                obj1 = s1;
                break;
            case 2:
                obj1 = Integer.valueOf(width1);
                break;
            case 3:
                obj1 = Long.valueOf(((long) width1 << 32) ^ width2);
                break;
            case 4:
                obj1 = new StringBuilder(s2);
                break;
            default:
                obj1 = new StrBuilder(s3);
                break;
        }

        builder = new StrBuilder();
        if (data.consumeBoolean()) {
            builder.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        builder.appendFixedWidthPadRight(obj1, width1, padChar);

        builder = new StrBuilder(data.consumeString(64));
        if (data.consumeBoolean()) {
            builder.setNullText(null);
        } else {
            builder.setNullText(data.consumeAsciiString(32));
        }
        Object obj2;
        switch (data.consumeInt(0, 4)) {
            case 0:
                obj2 = null;
                break;
            case 1:
                obj2 = "";
                break;
            case 2:
                obj2 = s2;
                break;
            case 3:
                obj2 = builder;
                break;
            default:
                obj2 = data.consumeBytes(Math.min(32, Math.max(0, data.remainingBytes())));
                break;
        }
        builder.appendFixedWidthPadRight(obj2, width2, (char) (data.consumeByte() & 0xFF));

        builder = new StrBuilder();
        builder.setNullText(null);
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(16));
        }
        builder.appendFixedWidthPadRight(null, width3, (char) (data.consumeByte() & 0xFF));

        builder = new StrBuilder();
        builder.setNullText(data.consumeBoolean() ? "" : data.consumeString(16));
        builder.appendFixedWidthPadRight(s3, data.consumeInt(-8, 128), (char) (data.consumeByte() & 0xFF));
    }
}