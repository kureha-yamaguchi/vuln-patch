package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 5)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeString(64));
                break;
            case 2:
                builder = new StrBuilder(data.consumeAsciiString(64));
                break;
            case 3:
                builder = new StrBuilder(data.consumeInt(-8, 128));
                break;
            case 4:
                builder = new StrBuilder();
                builder.append(data.consumeString(32));
                builder.append(data.consumeAsciiString(32));
                break;
            default:
                builder = new StrBuilder();
                if (data.consumeBoolean()) {
                    builder.setNullText(data.consumeBoolean() ? null : data.consumeString(16));
                }
                break;
        }

        if (data.consumeBoolean()) {
            builder.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeBoolean() ? null : data.consumeAsciiString(16));
        }

        switch (data.consumeInt(0, 4)) {
            case 0:
                builder.append(data.consumeString(64));
                break;
            case 1:
                builder.append(data.consumeAsciiString(64));
                break;
            case 2:
                builder.append(data.consumeBoolean());
                builder.append(data.consumeInt(-1000, 1000));
                break;
            case 3:
                builder.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
                break;
            default:
                if (data.consumeBoolean()) {
                    builder.clear();
                }
                break;
        }

        Object obj;
        switch (data.consumeInt(0, 7)) {
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
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 6:
                obj = new StringBuffer(data.consumeString(64));
                break;
            default:
                obj = new StrBuilder(data.consumeString(64));
                break;
        }

        int width;
        switch (data.consumeInt(0, 6)) {
            case 0:
                width = data.consumeInt();
                break;
            case 1:
                width = data.consumeInt(-8, 8);
                break;
            case 2:
                width = data.consumeInt(0, 32);
                break;
            case 3:
                width = data.consumeInt(1, 256);
                break;
            case 4:
                width = 0;
                break;
            case 5:
                width = -1;
                break;
            default:
                width = builder.length() + data.consumeInt(-4, 16);
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);

        if (data.consumeBoolean()) {
            builder.appendFixedWidthPadRight(obj, width, padChar);
        } else {
            int firstWidth = data.consumeInt(-4, 32);
            char firstPad = (char) (data.consumeByte() & 0xff);
            builder.appendFixedWidthPadRight(obj, firstWidth, firstPad);
            builder.appendFixedWidthPadRight(obj, width, padChar);
        }

        if (data.consumeBoolean()) {
            builder.toString();
        }
    }
}