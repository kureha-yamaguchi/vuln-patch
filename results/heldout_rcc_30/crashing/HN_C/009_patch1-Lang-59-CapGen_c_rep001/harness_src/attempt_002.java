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
                builder = new StrBuilder(data.consumeInt(0, 256));
                break;
            case 2:
                builder = new StrBuilder(data.consumeString(128));
                break;
            default:
                builder = new StrBuilder(data.consumeAsciiString(128));
                break;
        }

        if (data.consumeBoolean()) {
            builder.setNullText(null);
        } else {
            if (data.consumeBoolean()) {
                builder.setNullText(data.consumeString(32));
            } else {
                builder.setNullText(data.consumeAsciiString(32));
            }
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

        int iterations = data.consumeInt(1, 6);
        for (int i = 0; i < iterations; i++) {
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
                    obj = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                case 5:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 6:
                    obj = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
                case 7:
                    obj = new StringBuffer(data.consumeString(64));
                    break;
                default:
                    obj = new StrBuilder(data.consumeAsciiString(64));
                    break;
            }

            int width;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    width = data.consumeInt(-4, 4);
                    break;
                case 1:
                    width = data.consumeInt(0, 32);
                    break;
                case 2:
                    width = data.consumeInt(33, 256);
                    break;
                case 3:
                    width = 0;
                    break;
                case 4:
                    width = 1;
                    break;
                default:
                    width = data.consumeInt(-256, 1024);
                    break;
            }

            char padChar = (char) (data.consumeByte() & 0xff);
            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                if (data.consumeBoolean()) {
                    builder.setNullText(null);
                } else if (data.consumeBoolean()) {
                    builder.setNullText(data.consumeString(16));
                } else {
                    builder.setNullText(data.consumeAsciiString(16));
                }
            }
        }

        if (data.remainingBytes() > 0) {
            Object finalObj;
            if (data.consumeBoolean()) {
                finalObj = null;
            } else if (data.consumeBoolean()) {
                finalObj = data.consumeRemainingAsString();
            } else {
                finalObj = new StrBuilder(data.consumeRemainingAsString());
            }
            builder.appendFixedWidthPadRight(finalObj, data.consumeInt(-32, 512), (char) (data.consumeByte() & 0xff));
        }
    }
}