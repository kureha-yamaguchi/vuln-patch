package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        switch (data.consumeInt(0, 2)) {
            case 0:
                sb = new StrBuilder();
                break;
            case 1:
                sb = new StrBuilder(data.consumeString(32));
                break;
            default:
                sb = new StrBuilder(data.consumeInt(0, 128));
                break;
        }

        if (data.consumeBoolean()) {
            switch (data.consumeInt(0, 2)) {
                case 0:
                    sb.setNullText(null);
                    break;
                case 1:
                    sb.setNullText("");
                    break;
                default:
                    sb.setNullText(data.consumeString(32));
                    break;
            }
        }

        if (data.consumeBoolean()) {
            sb.append(data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            sb.append(data.consumeAsciiString(32));
        }
        if (data.consumeBoolean()) {
            sb.appendPadding(data.consumeInt(0, 16), (char) (data.consumeByte() & 0xff));
        }

        for (int i = 0; i < 2 && data.remainingBytes() > 0; i++) {
            Object obj;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(32);
                    break;
                case 2:
                    obj = data.consumeAsciiString(32);
                    break;
                case 3:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 5:
                    obj = new StrBuilder(data.consumeString(16));
                    break;
                default:
                    obj = data.consumeBytes(16);
                    break;
            }

            int width = data.consumeBoolean() ? data.consumeInt(1, 64) : data.consumeInt(-8, 64);
            char padChar = (char) (data.consumeByte() & 0xff);
            sb.appendFixedWidthPadRight(obj, width, padChar);
        }

        Object targetObj;
        switch (data.consumeInt(0, 7)) {
            case 0:
                targetObj = null;
                break;
            case 1:
                targetObj = data.consumeString(128);
                break;
            case 2:
                targetObj = data.consumeAsciiString(128);
                break;
            case 3:
                targetObj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                targetObj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 5:
                targetObj = new StrBuilder(data.consumeString(64));
                break;
            case 6:
                targetObj = data.consumeRemainingAsString();
                break;
            default:
                targetObj = data.consumeRemainingAsBytes();
                break;
        }

        int targetWidth;
        if (data.remainingBytes() > 0) {
            targetWidth = data.consumeBoolean() ? data.consumeInt(1, 256) : data.consumeInt(-16, 256);
        } else {
            targetWidth = 1;
        }
        char targetPad = (char) (data.remainingBytes() > 0 ? (data.consumeByte() & 0xff) : ' ');

        sb.appendFixedWidthPadRight(targetObj, targetWidth, targetPad);
    }
}