package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        if (data.consumeBoolean()) {
            builder = new StrBuilder();
        } else if (data.consumeBoolean()) {
            builder = new StrBuilder(data.consumeString(64));
        } else {
            builder = new StrBuilder(data.consumeInt(0, 128));
        }

        if (data.consumeBoolean()) {
            builder.setNullText(null);
        } else {
            builder.setNullText(data.consumeString(32));
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

        int iterations = data.remainingBytes() > 0 ? data.consumeInt(1, Math.min(8, data.remainingBytes())) : 1;
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
                    obj = new StringBuilder(data.consumeString(64));
                    break;
                case 7:
                    obj = new StringBuffer(data.consumeAsciiString(64));
                    break;
                default:
                    obj = new StrBuilder(data.consumeString(64));
                    break;
            }

            int width;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    width = data.consumeInt(-16, 16);
                    break;
                case 1:
                    width = data.consumeInt(0, 256);
                    break;
                case 2:
                    width = 0;
                    break;
                case 3:
                    width = 1;
                    break;
                case 4:
                    width = data.consumeInt(-256, -1);
                    break;
                default:
                    width = data.consumeInt();
                    if (width > 4096) {
                        width = 4096;
                    } else if (width < -4096) {
                        width = -4096;
                    }
                    break;
            }

            char padChar;
            if (data.consumeBoolean()) {
                padChar = (char) (data.consumeByte() & 0xff);
            } else {
                String s = data.consumeString(1);
                padChar = s.length() == 0 ? '\0' : s.charAt(0);
            }

            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                if (data.consumeBoolean()) {
                    builder.setNullText(null);
                } else {
                    builder.setNullText(data.consumeString(16));
                }
            }
        }
    }
}