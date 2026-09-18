package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 2)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeInt(0, 128));
                break;
            default:
                builder = new StrBuilder(data.consumeString(64));
                break;
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(64));
        }

        switch (data.consumeInt(0, 3)) {
            case 0:
                builder.setNullText(null);
                break;
            case 1:
                builder.setNullText("");
                break;
            case 2:
                builder.setNullText(data.consumeString(32));
                break;
            default:
                builder.setNullText(data.consumeAsciiString(32));
                break;
        }

        Object obj1;
        switch (data.consumeInt(0, 6)) {
            case 0:
                obj1 = null;
                break;
            case 1:
                obj1 = data.consumeString(64);
                break;
            case 2:
                obj1 = data.consumeAsciiString(64);
                break;
            case 3:
                obj1 = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj1 = new StrBuilder(data.consumeString(32));
                break;
            case 5:
                obj1 = new StringBuffer(data.consumeString(32));
                break;
            default:
                obj1 = data.consumeBytes(32);
                break;
        }

        int width1;
        switch (data.consumeInt(0, 7)) {
            case 0:
                width1 = -1;
                break;
            case 1:
                width1 = 0;
                break;
            case 2:
                width1 = 1;
                break;
            case 3:
                width1 = data.consumeInt(-16, 16);
                break;
            case 4:
                width1 = data.consumeInt(0, 64);
                break;
            case 5:
                width1 = data.consumeInt(1, 128);
                break;
            case 6:
                width1 = data.consumeInt(-64, 256);
                break;
            default:
                width1 = data.consumeInt();
                if (width1 > 512) {
                    width1 = 512;
                } else if (width1 < -64) {
                    width1 = -64;
                }
                break;
        }

        char pad1 = (char) (data.consumeByte() & 0xFF);
        builder.appendFixedWidthPadRight(obj1, width1, pad1);

        Object obj2;
        switch (data.consumeInt(0, 4)) {
            case 0:
                obj2 = null;
                break;
            case 1:
                obj2 = builder.toString();
                break;
            case 2:
                obj2 = new StrBuilder(data.consumeAsciiString(24));
                break;
            case 3:
                obj2 = Boolean.valueOf(data.consumeBoolean());
                break;
            default:
                obj2 = data.consumeRemainingAsString();
                break;
        }

        int width2 = data.consumeInt(-32, 256);
        char pad2 = (char) (data.consumeByte() & 0xFF);
        builder.appendFixedWidthPadRight(obj2, width2, pad2);

        if (data.remainingBytes() > 0) {
            Object obj3 = data.consumeBoolean() ? null : data.consumeRemainingAsBytes();
            int width3 = data.remainingBytes() == 0 ? data.consumeInt(-8, 32) : data.consumeInt(-8, 64);
            char pad3 = (char) (data.consumeByte() & 0xFF);
            builder.appendFixedWidthPadRight(obj3, width3, pad3);
        }
    }
}