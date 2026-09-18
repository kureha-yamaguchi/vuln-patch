package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        int ctorChoice = data.consumeInt(0, 2);
        if (ctorChoice == 0) {
            builder = new StrBuilder();
        } else if (ctorChoice == 1) {
            builder = new StrBuilder(data.consumeInt(-8, 256));
        } else {
            builder = new StrBuilder(data.consumeString(128));
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            builder.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        int preOps = data.consumeInt(0, 6);
        for (int i = 0; i < preOps; i++) {
            switch (data.consumeInt(0, 5)) {
                case 0:
                    builder.append(data.consumeBoolean() ? null : data.consumeString(64));
                    break;
                case 1:
                    builder.append(data.consumeAsciiString(64));
                    break;
                case 2:
                    builder.append(data.consumeInt());
                    break;
                case 3:
                    builder.append(data.consumeBoolean());
                    break;
                case 4:
                    builder.ensureCapacity(data.consumeInt(-16, 512));
                    break;
                default:
                    builder.setLength(data.consumeInt(0, 128));
                    break;
            }
        }

        Object obj;
        switch (data.consumeInt(0, 7)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(256);
                break;
            case 2:
                obj = data.consumeAsciiString(256);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 5:
                obj = Byte.valueOf(data.consumeByte());
                break;
            case 6:
                obj = new String(data.consumeBytes(128));
                break;
            default:
                obj = new StrBuilder(data.consumeString(128));
                break;
        }

        char padChar;
        if (data.consumeBoolean()) {
            padChar = (char) (data.consumeByte() & 0xFF);
        } else {
            String s = data.consumeString(2);
            padChar = s.length() == 0 ? '\u0000' : s.charAt(0);
        }

        int widthChoice = data.consumeInt(0, 8);
        int width;
        switch (widthChoice) {
            case 0:
                width = data.consumeInt();
                break;
            case 1:
                width = data.consumeInt(-16, 256);
                break;
            case 2:
                width = -1;
                break;
            case 3:
                width = 0;
                break;
            case 4:
                width = 1;
                break;
            case 5:
                width = 2;
                break;
            case 6:
                width = 8;
                break;
            case 7:
                width = 64;
                break;
            default:
                width = 256;
                break;
        }

        builder.appendFixedWidthPadRight(obj, width, padChar);

        int postOps = data.consumeInt(0, 3);
        for (int i = 0; i < postOps; i++) {
            Object obj2;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    obj2 = null;
                    break;
                case 1:
                    obj2 = data.consumeRemainingAsString();
                    break;
                case 2:
                    obj2 = Integer.valueOf(data.consumeInt());
                    break;
                case 3:
                    obj2 = new StrBuilder(data.consumeString(64));
                    break;
                default:
                    obj2 = Boolean.valueOf(data.consumeBoolean());
                    break;
            }

            int w2 = data.remainingBytes() > 0 ? data.consumeInt(-8, 128) : 0;
            char p2 = data.remainingBytes() > 0 ? (char) (data.consumeByte() & 0xFF) : ' ';
            builder.appendFixedWidthPadRight(obj2, w2, p2);
        }
    }
}