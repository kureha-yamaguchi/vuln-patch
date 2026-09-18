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
            builder.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
        }
        if (data.consumeBoolean()) {
            builder.ensureCapacity(data.consumeInt(0, 256));
        }

        int iterations = data.consumeInt(1, 8);
        for (int i = 0; i < iterations; i++) {
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
                    obj = new StringBuilder(data.consumeString(128));
                    break;
                case 4:
                    obj = new StrBuilder(data.consumeString(128));
                    break;
                case 5:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 6:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                default:
                    obj = data.consumeBytes(64);
                    break;
            }

            int width;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    width = data.consumeInt();
                    break;
                case 1:
                    width = data.consumeInt(-4, 4);
                    break;
                case 2:
                    width = data.consumeInt(0, 32);
                    break;
                case 3:
                    width = data.remainingBytes();
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
                    padChar = '\u0000';
                    break;
                case 2:
                    padChar = ' ';
                    break;
                default:
                    padChar = (char) data.consumeInt(0, 65535);
                    break;
            }

            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                builder.toString();
            }
            if (data.consumeBoolean() && builder.length() > 0) {
                builder.charAt(data.consumeInt(0, builder.length() - 1));
            }
        }

        if (data.consumeBoolean()) {
            builder.appendFixedWidthPadRight(null, data.consumeInt(-2, 8), (char) (data.consumeByte() & 0xff));
        }

        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            builder.appendFixedWidthPadRight(data.consumeRemainingAsString(), data.consumeInt(-2, 16), (char) (data.consumeByte() & 0xff));
        }
    }
}