package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 3)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeString(64));
                break;
            case 2:
                builder = new StrBuilder(data.consumeInt(-32, 128));
                break;
            default:
                builder = new StrBuilder(data.consumeAsciiString(64));
                break;
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeString(16));
        }
        if (data.consumeBoolean()) {
            if (data.consumeBoolean()) {
                builder.setNullText(null);
            } else {
                builder.setNullText(data.consumeString(32));
            }
        }

        if (data.consumeBoolean()) {
            builder.ensureCapacity(data.consumeInt(-32, 256));
        }
        if (data.consumeBoolean()) {
            builder.minimizeCapacity();
        }
        if (data.consumeBoolean()) {
            builder.setLength(data.consumeInt(0, 64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            builder.appendPadding(data.consumeInt(-8, 32), (char) (data.consumeByte() & 0xff));
        }
        if (data.consumeBoolean()) {
            builder.appendNewLine();
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
                obj = Long.valueOf(((long) data.consumeInt() << 32) ^ (long) data.consumeInt());
                break;
            case 5:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 6:
                obj = new StrBuilder(data.consumeString(128));
                break;
            case 7:
                obj = new StringBuffer(data.consumeString(128));
                break;
            default:
                obj = new char[] { (char) (data.consumeByte() & 0xff), (char) (data.consumeByte() & 0xff) };
                break;
        }

        int width;
        switch (data.consumeInt(0, 4)) {
            case 0:
                width = data.consumeInt();
                break;
            case 1:
                width = data.consumeInt(-8, 8);
                break;
            case 2:
                width = data.consumeInt(0, 256);
                break;
            case 3:
                width = 0;
                break;
            default:
                width = data.consumeBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);

        builder.appendFixedWidthPadRight(obj, width, padChar);

        if (data.consumeBoolean()) {
            builder.appendFixedWidthPadRight(data.consumeRemainingAsString(), data.consumeInt(-8, 64), (char) (data.consumeByte() & 0xff));
        }
    }
}