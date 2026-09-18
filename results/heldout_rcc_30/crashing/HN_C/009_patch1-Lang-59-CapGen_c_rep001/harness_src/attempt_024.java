package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        switch (data.consumeInt(0, 3)) {
            case 0:
                sb = new StrBuilder();
                break;
            case 1:
                sb = new StrBuilder(data.consumeString(32));
                break;
            case 2:
                sb = new StrBuilder(data.consumeInt(-4, 64));
                break;
            default:
                sb = new StrBuilder(data.consumeAsciiString(32));
                break;
        }

        if (data.consumeBoolean()) {
            sb.setNullText(null);
        } else {
            sb.setNullText(data.consumeString(16));
        }

        if (data.consumeBoolean()) {
            sb.append(data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            sb.append(data.consumeAsciiString(32));
        }
        if (data.consumeBoolean()) {
            sb.append(data.consumeInt());
        }

        Object obj1;
        switch (data.consumeInt(0, 7)) {
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
                obj1 = Boolean.valueOf(data.consumeBoolean());
                break;
            case 5:
                obj1 = new StrBuilder(data.consumeString(32));
                break;
            case 6:
                obj1 = new StringBuffer(data.consumeAsciiString(32));
                break;
            default:
                obj1 = data.consumeBytes(16);
                break;
        }

        int width1;
        switch (data.consumeInt(0, 5)) {
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
                width1 = data.consumeInt(-8, 8);
                break;
            case 4:
                width1 = data.consumeInt(0, 64);
                break;
            default:
                width1 = data.consumeInt(-32, 128);
                break;
        }
        char pad1 = (char) (data.consumeByte() & 0xff);
        sb.appendFixedWidthPadRight(obj1, width1, pad1);

        Object obj2;
        switch (data.consumeInt(0, 6)) {
            case 0:
                obj2 = null;
                break;
            case 1:
                obj2 = data.consumeRemainingAsString();
                break;
            case 2:
                obj2 = Long.valueOf(data.consumeInt());
                break;
            case 3:
                obj2 = new StrBuilder(data.consumeAsciiString(24));
                break;
            case 4:
                obj2 = new StringBuffer(data.consumeString(24));
                break;
            case 5:
                obj2 = data.consumeRemainingAsBytes();
                break;
            default:
                obj2 = "";
                break;
        }

        int width2 = data.remainingBytes() == 0 ? data.consumeInt(-2, 4) : data.consumeInt(-16, 96);
        char pad2 = (char) (data.consumeByte() & 0xff);
        sb.appendFixedWidthPadRight(obj2, width2, pad2);

        if (data.consumeBoolean()) {
            sb.appendFixedWidthPadRight(null, data.consumeInt(-4, 16), (char) (data.consumeByte() & 0xff));
        }
    }
}