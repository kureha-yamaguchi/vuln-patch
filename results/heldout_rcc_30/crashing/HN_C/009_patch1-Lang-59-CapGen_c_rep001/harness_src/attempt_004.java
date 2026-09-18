package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        if (data.consumeBoolean()) {
            builder = new StrBuilder(data.consumeInt(0, 256));
        } else {
            builder = new StrBuilder(data.consumeString(64));
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(64));
        }
        if (data.consumeBoolean()) {
            builder.append((char) (data.consumeByte() & 0xff));
        }

        String nullText = null;
        if (data.consumeBoolean()) {
            nullText = data.consumeBoolean() ? null : data.consumeString(32);
            builder.setNullText(nullText);
        }

        Object obj;
        switch (data.consumeInt(0, 7)) {
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
                obj = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                break;
            case 5:
                obj = new StringBuffer(data.consumeString(64));
                break;
            case 6:
                obj = new StrBuilder(data.consumeString(64));
                break;
            default:
                obj = data.consumeBytes(32);
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);
        int width = data.consumeInt(-8, 256);

        builder.appendFixedWidthPadRight(obj, width, padChar);

        String rendered = (obj == null) ? nullText : obj.toString();
        if (rendered != null) {
            int len = rendered.length();
            builder.appendFixedWidthPadRight(obj, -1, padChar);
            builder.appendFixedWidthPadRight(obj, 0, padChar);
            builder.appendFixedWidthPadRight(obj, 1, padChar);
            builder.appendFixedWidthPadRight(obj, Math.max(0, len - 1), padChar);
            builder.appendFixedWidthPadRight(obj, len, padChar);
            builder.appendFixedWidthPadRight(obj, len + 1, padChar);
        } else {
            builder.appendFixedWidthPadRight(null, data.consumeInt(-2, 8), padChar);
        }

        if (data.remainingBytes() > 0) {
            Object obj2 = data.consumeBoolean() ? data.consumeRemainingAsString() : data.consumeRemainingAsBytes();
            int width2 = data.consumeBoolean() ? data.remainingBytes() : 0;
            builder.appendFixedWidthPadRight(obj2, width2, (char) (padChar ^ 0x5a));
        }
    }
}