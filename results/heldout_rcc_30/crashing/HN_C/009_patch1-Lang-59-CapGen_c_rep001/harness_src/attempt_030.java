package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        if (data.consumeBoolean()) {
            builder = new StrBuilder();
        } else if (data.consumeBoolean()) {
            builder = new StrBuilder(data.consumeString(128));
        } else {
            builder = new StrBuilder(data.consumeInt(-16, 512));
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeBoolean() ? null : data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            builder.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        int prefixOps = data.consumeInt(0, 6);
        for (int i = 0; i < prefixOps; i++) {
            switch (data.consumeInt(0, 7)) {
                case 0:
                    builder.append(data.consumeString(64));
                    break;
                case 1:
                    builder.append(data.consumeAsciiString(64));
                    break;
                case 2:
                    builder.append(data.consumeBoolean());
                    break;
                case 3:
                    builder.append(data.consumeInt());
                    break;
                case 4:
                    builder.append(data.consumeByte());
                    break;
                case 5:
                    builder.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
                    break;
                case 6:
                    builder.appendNewLine();
                    break;
                default:
                    builder.ensureCapacity(data.consumeInt(-16, 1024));
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
                obj = Long.valueOf((long) data.consumeInt());
                break;
            case 5:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 6:
                obj = new String(data.consumeBytes(128));
                break;
            default:
                obj = new StrBuilder(data.consumeString(128));
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
                width = data.consumeInt(0, 64);
                break;
            case 3:
                width = data.consumeInt(0, 1024);
                break;
            default:
                width = data.remainingBytes();
                break;
        }

        char padChar;
        switch (data.consumeInt(0, 3)) {
            case 0:
                padChar = (char) (data.consumeByte() & 0xff);
                break;
            case 1:
                padChar = (char) data.consumeInt(0, Character.MAX_VALUE);
                break;
            case 2:
                String s = data.consumeString(2);
                padChar = s.length() == 0 ? '\0' : s.charAt(0);
                break;
            default:
                padChar = ' ';
                break;
        }

        if (data.consumeBoolean()) {
            builder.appendFixedWidthPadRight(obj, width, padChar);
        } else {
            builder.appendFixedWidthPadLeft(obj, width, padChar);
            builder.appendFixedWidthPadRight(obj, width, padChar);
        }

        if (data.consumeBoolean()) {
            builder.toString();
            builder.length();
            if (builder.length() > 0 && data.consumeBoolean()) {
                builder.charAt(data.consumeInt(0, builder.length() - 1));
            }
        }
    }
}