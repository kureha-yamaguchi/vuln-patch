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
            if (data.consumeBoolean()) {
                builder.setNullText(null);
            } else {
                builder.setNullText(data.consumeString(32));
            }
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(32));
        }

        Object obj;
        int kind = data.consumeInt(0, 5);
        switch (kind) {
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
                obj = new StrBuilder(data.consumeString(256));
                break;
            default:
                obj = data.consumeRemainingAsBytes();
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);

        int width;
        if (obj instanceof String && data.consumeBoolean()) {
            String s = (String) obj;
            if (data.consumeBoolean()) {
                width = s.length();
            } else {
                width = s.length() == 0 ? 0 : data.consumeInt(-2, s.length() + 2);
            }
        } else if (obj instanceof StrBuilder && data.consumeBoolean()) {
            int len = ((StrBuilder) obj).length();
            width = len == 0 ? data.consumeInt(-2, 2) : data.consumeInt(-2, len + 2);
        } else {
            width = data.consumeInt();
        }

        builder.appendFixedWidthPadRight(obj, width, padChar);

        if (data.remainingBytes() > 0) {
            String s = data.consumeRemainingAsString();
            int secondWidth;
            if (data.consumeBoolean()) {
                secondWidth = s.length() == 0 ? 0 : data.consumeInt(-2, s.length() + 2);
            } else {
                secondWidth = data.consumeInt();
            }
            builder.appendFixedWidthPadRight(s, secondWidth, (char) (padChar ^ 0x5a));
        }
    }
}