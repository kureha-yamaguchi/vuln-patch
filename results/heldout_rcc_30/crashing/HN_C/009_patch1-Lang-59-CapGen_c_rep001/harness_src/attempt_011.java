package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        int ctorChoice = data.consumeInt(0, 2);
        if (ctorChoice == 0) {
            builder = new StrBuilder();
        } else if (ctorChoice == 1) {
            builder = new StrBuilder(data.consumeInt(0, 256));
        } else {
            builder = new StrBuilder(data.consumeString(64));
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
            builder.appendPadding(data.consumeInt(0, 64), (char) (data.consumeByte() & 0xFF));
        }

        int ops = data.consumeInt(1, 6);
        for (int i = 0; i < ops; i++) {
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
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 5:
                    obj = Byte.valueOf(data.consumeByte());
                    break;
                case 6:
                    obj = new StringBuilder(data.consumeString(128));
                    break;
                default:
                    obj = new StrBuilder(data.consumeString(128));
                    break;
            }

            int width;
            if (data.consumeBoolean()) {
                width = data.consumeInt(-8, 256);
            } else {
                int r = data.remainingBytes();
                width = (r == 0) ? 0 : data.consumeInt(-32, Math.min(4096, r * 64 + 32));
            }

            char padChar = (char) (data.consumeByte() & 0xFF);
            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                builder.toString();
            }
            if (data.consumeBoolean()) {
                builder.length();
            }
        }

        if (data.consumeBoolean()) {
            StrBuilder second = new StrBuilder(builder.toString());
            if (data.consumeBoolean()) {
                second.setNullText(null);
            } else {
                second.setNullText(data.consumeString(16));
            }
            second.appendFixedWidthPadRight(
                data.consumeBoolean() ? null : data.consumeRemainingAsString(),
                data.consumeInt(-4, 128),
                (char) (data.consumeByte() & 0xFF)
            );
            second.toString();
        } else {
            builder.toString();
        }
    }
}