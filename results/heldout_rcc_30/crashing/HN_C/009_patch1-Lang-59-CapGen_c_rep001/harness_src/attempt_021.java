package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 2)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeString(64));
                break;
            default:
                builder = new StrBuilder(data.consumeInt(0, 256));
                break;
        }

        if (data.consumeBoolean()) {
            builder.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
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

        int iterations = data.consumeInt(1, 8);
        for (int i = 0; i < iterations; i++) {
            Object obj;
            switch (data.consumeInt(0, 6)) {
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
                    obj = Byte.valueOf(data.consumeByte());
                    break;
                default:
                    obj = data.consumeRemainingAsString();
                    break;
            }

            int width;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    width = data.consumeInt(-8, 8);
                    break;
                case 1:
                    width = data.consumeInt(0, 64);
                    break;
                case 2:
                    width = data.consumeInt(1, 256);
                    break;
                case 3:
                    width = 0;
                    break;
                default:
                    width = data.remainingBytes();
                    break;
            }

            char padChar;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    padChar = (char) data.consumeInt(0, 65535);
                    break;
                case 1:
                    padChar = (char) (data.consumeByte() & 0xff);
                    break;
                case 2:
                    padChar = ' ';
                    break;
                default:
                    padChar = '\0';
                    break;
            }

            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                builder.toString();
            }
            if (data.consumeBoolean() && width >= 0 && width <= builder.length()) {
                builder.substring(0, width);
            }
        }
    }
}