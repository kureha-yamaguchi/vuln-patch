package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 4)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeString(32));
                break;
            case 2:
                builder = new StrBuilder(data.consumeInt(-16, 128));
                break;
            case 3:
                builder = new StrBuilder(data.consumeAsciiString(32));
                break;
            default:
                builder = new StrBuilder(data.consumeRemainingAsString());
                break;
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeBoolean() ? null : data.consumeString(16));
        }

        if (data.consumeBoolean()) {
            switch (data.consumeInt(0, 3)) {
                case 0:
                    builder.setNullText(null);
                    break;
                case 1:
                    builder.setNullText("");
                    break;
                case 2:
                    builder.setNullText(data.consumeString(16));
                    break;
                default:
                    builder.setNullText(data.consumeAsciiString(16));
                    break;
            }
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(32));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeInt());
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeBoolean());
        }
        if (data.consumeBoolean()) {
            builder.appendPadding(data.consumeInt(-8, 32), (char) (data.consumeByte() & 0xff));
        }

        int iterations = data.consumeInt(1, 8);
        for (int i = 0; i < iterations; i++) {
            Object obj;
            switch (data.consumeInt(0, 8)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(64);
                    break;
                case 2:
                    obj = data.consumeAsciiString(64);
                    break;
                case 3:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj = Long.valueOf(data.consumeInt());
                    break;
                case 5:
                    obj = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
                case 6:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 7:
                    obj = new String(data.consumeBytes(64));
                    break;
                default:
                    obj = data.consumeRemainingAsString();
                    break;
            }

            int width;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    width = data.consumeInt(-4, 4);
                    break;
                case 1:
                    width = data.consumeInt(0, 16);
                    break;
                case 2:
                    width = data.consumeInt(16, 128);
                    break;
                case 3:
                    width = data.consumeInt();
                    break;
                case 4:
                    width = builder.length();
                    break;
                default:
                    width = builder.length() + data.consumeInt(-8, 8);
                    break;
            }

            char padChar;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    padChar = (char) (data.consumeByte() & 0xff);
                    break;
                case 1:
                    padChar = ' ';
                    break;
                case 2:
                    padChar = '\0';
                    break;
                default:
                    padChar = (char) data.consumeInt(0, Character.MAX_VALUE);
                    break;
            }

            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                builder.length();
                builder.capacity();
                builder.toString();
            }
            if (data.consumeBoolean()) {
                int start = data.consumeInt(0, builder.length());
                int end = data.consumeInt(start, builder.length());
                builder.substring(start, end);
            }
        }

        if (data.consumeBoolean()) {
            Object finalObj = data.consumeBoolean() ? null : data.consumeRemainingAsString();
            int finalWidth = data.consumeBoolean() ? 0 : data.consumeInt(-32, 256);
            char finalPad = (char) (data.consumeByte() & 0xff);
            builder.appendFixedWidthPadRight(finalObj, finalWidth, finalPad);
        }
    }
}