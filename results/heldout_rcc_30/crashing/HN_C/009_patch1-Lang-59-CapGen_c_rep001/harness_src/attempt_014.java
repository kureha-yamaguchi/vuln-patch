package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        if (data.consumeBoolean()) {
            builder = new StrBuilder();
        } else {
            builder = new StrBuilder(data.consumeString(64));
        }

        if (data.consumeBoolean()) {
            builder.setNullText(null);
        } else {
            builder.setNullText(data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeString(16));
        }

        int preOps = data.consumeInt(0, 6);
        for (int i = 0; i < preOps; i++) {
            switch (data.consumeInt(0, 7)) {
                case 0:
                    builder.append(data.consumeString(32));
                    break;
                case 1:
                    builder.append(data.consumeAsciiString(32));
                    break;
                case 2:
                    builder.append(data.consumeInt());
                    break;
                case 3:
                    builder.append(data.consumeBoolean());
                    break;
                case 4:
                    builder.appendPadding(data.consumeInt(0, 16), (char) (data.consumeByte() & 0xFF));
                    break;
                case 5:
                    builder.appendFixedWidthPadLeft(data.consumeString(32), data.consumeInt(-8, 32),
                            (char) (data.consumeByte() & 0xFF));
                    break;
                case 6:
                    if (builder.length() > 0) {
                        int start = data.consumeInt(0, builder.length() - 1);
                        int end = data.consumeInt(start, builder.length());
                        builder.delete(start, end);
                    }
                    break;
                default:
                    if (builder.length() > 0) {
                        int idx = data.consumeInt(0, builder.length() - 1);
                        builder.setCharAt(idx, (char) (data.consumeByte() & 0xFF));
                    }
                    break;
            }
        }

        int mode = data.consumeInt(0, 8);
        Object obj;
        switch (mode) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(128);
                break;
            case 2:
                obj = data.consumeAsciiString(128);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 5:
                obj = new StrBuilder(data.consumeString(64));
                break;
            case 6:
                obj = data.consumeBytes(64);
                break;
            case 7:
                obj = builder.toStringBuffer();
                break;
            default:
                obj = builder;
                break;
        }

        int width1 = data.consumeInt();
        char pad1 = (char) (data.consumeByte() & 0xFF);
        builder.appendFixedWidthPadRight(obj, width1, pad1);

        if (data.remainingBytes() > 0) {
            Object obj2;
            switch (data.consumeInt(0, 5)) {
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
                    obj2 = new StrBuilder(data.consumeString(32));
                    break;
                case 4:
                    obj2 = builder.toString();
                    break;
                default:
                    obj2 = data.consumeRemainingAsBytes();
                    break;
            }
            int width2 = data.remainingBytes() > 0 ? data.consumeInt(-16, 64) : 1;
            char pad2 = data.remainingBytes() > 0 ? (char) (data.consumeByte() & 0xFF) : ' ';
            builder.appendFixedWidthPadRight(obj2, width2, pad2);
        }
    }
}