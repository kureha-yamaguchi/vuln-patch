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

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeInt());
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeBoolean());
        }
        if (data.consumeBoolean()) {
            builder.appendPadding(data.consumeInt(-8, 64), (char) (data.consumeByte() & 0xff));
        }
        if (data.consumeBoolean()) {
            builder.ensureCapacity(data.consumeInt(-16, 256));
        }
        if (data.consumeBoolean() && builder.length() > 0) {
            builder.deleteCharAt(data.consumeInt(0, builder.length() - 1));
        }

        Object obj;
        switch (data.consumeInt(0, 8)) {
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
                obj = Long.valueOf((long) data.consumeInt());
                break;
            case 5:
                obj = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 6:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 7:
                obj = data.consumeBytes(64);
                break;
            default:
                obj = new StrBuilder(data.consumeString(64));
                break;
        }

        int width = data.consumeInt();
        char padChar = (char) (data.consumeByte() & 0xff);

        builder.appendFixedWidthPadRight(obj, width, padChar);

        if (data.consumeBoolean()) {
            Object obj2;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    obj2 = null;
                    break;
                case 1:
                    obj2 = data.consumeRemainingAsString();
                    break;
                case 2:
                    obj2 = data.consumeRemainingAsBytes();
                    break;
                case 3:
                    obj2 = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj2 = new StrBuilder(data.consumeString(32));
                    break;
                default:
                    obj2 = builder.toString();
                    break;
            }
            builder.appendFixedWidthPadRight(obj2, data.consumeInt(-32, 256), (char) (data.consumeByte() & 0xff));
        }
    }
}