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
            sb.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            sb.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            sb.append(data.consumeAsciiString(64));
        }
        if (data.consumeBoolean()) {
            sb.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
        }

        Object obj;
        switch (data.consumeInt(0, 7)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(512);
                break;
            case 2:
                obj = data.consumeAsciiString(512);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Long.valueOf(((long) data.consumeInt() << 32) ^ (long) data.consumeInt());
                break;
            case 5:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 6:
                obj = new StringBuffer(data.consumeString(256));
                break;
            default:
                obj = new StrBuilder(data.consumeString(256));
                break;
        }

        int width;
        switch (data.consumeInt(0, 4)) {
            case 0:
                width = data.consumeInt(-8, 8);
                break;
            case 1:
                width = data.consumeInt(0, 32);
                break;
            case 2:
                width = data.consumeInt(0, 512);
                break;
            case 3:
                String s;
                if (obj == null) {
                    s = sb.getNullText();
                    width = (s == null) ? data.consumeInt(0, 16) : Math.max(1, s.length() - data.consumeInt(0, 4));
                } else {
                    s = obj.toString();
                    width = Math.max(1, s.length() - data.consumeInt(0, 4));
                }
                break;
            default:
                width = data.consumeInt();
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);

        sb.appendFixedWidthPadRight(obj, width, padChar);

        if (data.consumeBoolean()) {
            Object obj2;
            if (data.consumeBoolean()) {
                obj2 = data.consumeString(1024);
            } else if (data.consumeBoolean()) {
                obj2 = null;
                sb.setNullText(data.consumeBoolean() ? null : data.consumeString(64));
            } else {
                obj2 = new StrBuilder(data.consumeAsciiString(1024));
            }

            int width2;
            if (obj2 == null) {
                String nullText = sb.getNullText();
                width2 = (nullText == null) ? data.consumeInt(-4, 32)
                        : Math.max(1, nullText.length() - data.consumeInt(0, 8));
            } else {
                width2 = Math.max(1, obj2.toString().length() - data.consumeInt(0, 8));
            }

            sb.appendFixedWidthPadRight(obj2, width2, (char) (data.consumeByte() & 0xff));
        }
    }
}