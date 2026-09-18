package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int initialCapacity = data.consumeInt(-16, 64);
        StrBuilder builder = new StrBuilder(initialCapacity);

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        } else if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(64));
        }

        if (data.consumeBoolean()) {
            builder.minimizeCapacity();
        }

        if (data.consumeBoolean()) {
            if (data.consumeBoolean()) {
                builder.setNullText(null);
            } else if (data.consumeBoolean()) {
                builder.setNullText(data.consumeString(32));
            } else {
                builder.setNullText(data.consumeAsciiString(32));
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
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 5:
                obj = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 6:
                obj = new StrBuilder(data.consumeString(256));
                break;
            default:
                obj = new String(data.consumeBytes(256));
                break;
        }

        int width = data.consumeInt();
        char padChar = (char) (data.consumeByte() & 0xff);
        builder.appendFixedWidthPadRight(obj, width, padChar);

        if (data.consumeBoolean()) {
            StrBuilder targeted = new StrBuilder();
            if (data.consumeBoolean()) {
                targeted.append(data.consumeString(16));
            }
            targeted.minimizeCapacity();

            String longString;
            if (data.consumeBoolean()) {
                longString = data.consumeRemainingAsString();
            } else {
                longString = data.consumeString(512);
            }

            int targetedWidth;
            if (data.consumeBoolean()) {
                targetedWidth = data.consumeInt(1, 32);
            } else {
                targetedWidth = Math.max(1, data.consumeInt() & 31);
            }

            char targetedPad = (char) (data.consumeByte() & 0xff);
            targeted.appendFixedWidthPadRight(longString, targetedWidth, targetedPad);
        }
    }
}