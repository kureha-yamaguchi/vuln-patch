package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        switch (data.consumeInt(0, 4)) {
            case 0:
                sb = new StrBuilder();
                break;
            case 1:
                sb = new StrBuilder(data.consumeString(32));
                break;
            case 2:
                sb = new StrBuilder(data.consumeAsciiString(32));
                break;
            case 3:
                sb = new StrBuilder(data.consumeInt(-16, 64));
                break;
            default:
                sb = new StrBuilder(data.consumeRemainingAsString());
                break;
        }

        if (data.consumeBoolean()) {
            sb.setNullText(null);
        } else {
            sb.setNullText(data.consumeString(16));
        }

        if (data.consumeBoolean()) {
            sb.setNewLineText(data.consumeString(8));
        }

        int setupOps = data.consumeInt(0, 6);
        for (int i = 0; i < setupOps; i++) {
            switch (data.consumeInt(0, 7)) {
                case 0:
                    sb.append(data.consumeString(24));
                    break;
                case 1:
                    sb.append(data.consumeAsciiString(24));
                    break;
                case 2:
                    sb.append(data.consumeInt());
                    break;
                case 3:
                    sb.append(data.consumeBoolean());
                    break;
                case 4:
                    sb.appendPadding(data.consumeInt(0, 16), (char) (data.consumeByte() & 0xff));
                    break;
                case 5:
                    sb.appendNewLine();
                    break;
                case 6:
                    if (sb.length() > 0) {
                        int start = data.consumeInt(0, sb.length());
                        int end = data.consumeInt(start, sb.length());
                        sb.delete(start, end);
                    }
                    break;
                default:
                    if (data.consumeBoolean()) {
                        sb.clear();
                    }
                    break;
            }
        }

        Object obj;
        switch (data.consumeInt(0, 6)) {
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
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 5:
                obj = new StringBuilder(data.consumeString(64));
                break;
            default:
                obj = data.consumeRemainingAsBytes();
                break;
        }

        int width;
        switch (data.consumeInt(0, 3)) {
            case 0:
                width = data.consumeInt(-8, 8);
                break;
            case 1:
                width = data.consumeInt();
                break;
            case 2:
                width = sb.length();
                break;
            default:
                width = sb.length() + data.consumeInt(-4, 4);
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);

        sb.appendFixedWidthPadRight(obj, width, padChar);

        if (data.remainingBytes() > 0) {
            Object obj2;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    obj2 = null;
                    break;
                case 1:
                    obj2 = data.consumeRemainingAsString();
                    break;
                case 2:
                    obj2 = Long.valueOf(data.consumeInt());
                    break;
                case 3:
                    obj2 = sb.toString();
                    break;
                default:
                    obj2 = new StringBuffer(data.consumeString(32));
                    break;
            }
            int width2 = data.remainingBytes() > 0 ? data.consumeInt() : 0;
            char padChar2 = data.remainingBytes() > 0 ? (char) (data.consumeByte() & 0xff) : '\0';
            sb.appendFixedWidthPadRight(obj2, width2, padChar2);
        }
    }
}