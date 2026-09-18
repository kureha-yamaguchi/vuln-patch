package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        if (data.consumeBoolean()) {
            sb = new StrBuilder();
        } else {
            sb = new StrBuilder(data.consumeString(64));
        }

        if (data.consumeBoolean()) {
            sb.setNewLineText(data.consumeBoolean() ? null : data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            sb.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            sb.setLength(data.consumeInt(0, 32));
        }
        if (data.consumeBoolean()) {
            sb.ensureCapacity(data.consumeInt(-16, 256));
        }
        if (data.consumeBoolean()) {
            sb.minimizeCapacity();
        }

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
                obj = Long.valueOf(data.consumeInt());
                break;
            case 5:
                obj = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 6:
                obj = new StringBuffer(data.consumeString(128));
                break;
            case 7:
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
                width = data.consumeInt(0, 1);
                break;
            case 2:
                width = data.consumeInt(2, 32);
                break;
            case 3:
                width = data.consumeInt(33, 256);
                break;
            case 4:
                width = data.consumeInt();
                break;
            default:
                width = data.remainingBytes();
                break;
        }

        char padChar;
        if (data.consumeBoolean()) {
            padChar = (char) (data.consumeByte() & 0xff);
        } else {
            String s = data.consumeString(2);
            padChar = s.length() == 0 ? '\0' : s.charAt(0);
        }

        sb.appendFixedWidthPadRight(obj, width, padChar);

        if (data.consumeBoolean()) {
            sb.appendFixedWidthPadRight(null, data.consumeInt(-4, 16), (char) (data.consumeByte() & 0xff));
        }
        if (data.consumeBoolean()) {
            sb.appendFixedWidthPadRight(data.consumeAsciiString(64), data.consumeInt(-4, 64), (char) (data.consumeByte() & 0xff));
        }
        if (data.consumeBoolean()) {
            sb.appendFixedWidthPadRight(new StrBuilder(data.consumeString(64)), data.consumeInt(-4, 64), (char) (data.consumeByte() & 0xff));
        }
    }
}