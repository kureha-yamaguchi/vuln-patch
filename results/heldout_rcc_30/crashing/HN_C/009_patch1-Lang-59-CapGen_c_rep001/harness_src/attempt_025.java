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
                builder = new StrBuilder(data.consumeInt(0, 256));
                break;
            default:
                builder = new StrBuilder(data.consumeAsciiString(64));
                break;
        }

        switch (data.consumeInt(0, 3)) {
            case 0:
                builder.setNullText(null);
                break;
            case 1:
                builder.setNullText("");
                break;
            case 2:
                builder.setNullText(data.consumeString(32));
                break;
            default:
                builder.setNullText(data.consumeAsciiString(32));
                break;
        }

        int rounds = data.consumeInt(1, 6);
        for (int i = 0; i < rounds; i++) {
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
                    obj = new StringBuilder(data.consumeString(64));
                    break;
                default:
                    obj = new String(data.consumeBytes(64));
                    break;
            }

            int width;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    width = data.consumeInt();
                    if (width > 4096) {
                        width = 4096;
                    } else if (width < -4096) {
                        width = -4096;
                    }
                    break;
                case 1:
                    width = data.consumeInt(-8, 8);
                    break;
                case 2:
                    width = data.consumeInt(0, 32);
                    break;
                case 3:
                    width = data.consumeInt(33, 256);
                    break;
                default:
                    width = data.remainingBytes();
                    break;
            }

            char padChar;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    padChar = (char) data.consumeByte();
                    break;
                case 1:
                    padChar = (char) data.consumeInt(0, 65535);
                    break;
                case 2:
                    padChar = data.consumeBoolean() ? ' ' : '0';
                    break;
                default:
                    String s = data.consumeString(1);
                    padChar = s.length() == 0 ? '\0' : s.charAt(0);
                    break;
            }

            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                builder.toString();
                builder.length();
                builder.capacity();
            }

            if (data.consumeBoolean()) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        builder.setNullText(null);
                        break;
                    case 1:
                        builder.setNullText(data.consumeString(16));
                        break;
                    default:
                        builder.setNullText(data.consumeAsciiString(16));
                        break;
                }
            }
        }

        if (data.consumeBoolean()) {
            Object tailObj = data.consumeBoolean() ? null : data.consumeRemainingAsString();
            int tailWidth = data.remainingBytes();
            char tailPad = (char) (data.consumeBoolean() ? 0 : 'A');
            builder.appendFixedWidthPadRight(tailObj, tailWidth, tailPad);
        }
    }
}