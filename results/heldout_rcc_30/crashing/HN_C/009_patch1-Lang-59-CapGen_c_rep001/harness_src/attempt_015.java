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
                builder = new StrBuilder(data.consumeString(64));
                break;
            case 2:
                builder = new StrBuilder(data.consumeAsciiString(64));
                break;
            case 3:
                builder = new StrBuilder(data.consumeInt(-32, 256));
                break;
            default:
                builder = new StrBuilder(data.consumeRemainingAsString());
                break;
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            builder.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
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
            builder.append(new String(data.consumeBytes(32)));
        }

        Object obj;
        switch (data.consumeInt(0, 10)) {
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
                obj = Long.valueOf(data.consumeInt());
                break;
            case 5:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 6:
                obj = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 7:
                obj = new String(data.consumeBytes(128));
                break;
            case 8:
                obj = new StringBuffer(data.consumeString(128));
                break;
            case 9:
                obj = new StrBuilder(data.consumeString(128));
                break;
            default:
                obj = data.consumeRemainingAsString();
                break;
        }

        int width;
        switch (data.consumeInt(0, 5)) {
            case 0:
                width = data.consumeInt(-8, 8);
                break;
            case 1:
                width = data.consumeInt(0, 256);
                break;
            case 2:
                width = data.consumeInt(-256, 256);
                break;
            case 3:
                width = 0;
                break;
            case 4:
                width = 1;
                break;
            default:
                width = data.consumeInt();
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);

        builder.appendFixedWidthPadRight(obj, width, padChar);

        if (data.consumeBoolean()) {
            Object obj2;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    obj2 = null;
                    break;
                case 1:
                    obj2 = builder.toString();
                    break;
                case 2:
                    obj2 = new StrBuilder(data.consumeAsciiString(64));
                    break;
                case 3:
                    obj2 = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj2 = new String(data.consumeRemainingAsBytes());
                    break;
                case 5:
                    obj2 = Boolean.valueOf(data.consumeBoolean());
                    break;
                default:
                    obj2 = data.consumeString(64);
                    break;
            }
            builder.appendFixedWidthPadRight(obj2, data.consumeInt(-16, 128), (char) (data.consumeByte() & 0xff));
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