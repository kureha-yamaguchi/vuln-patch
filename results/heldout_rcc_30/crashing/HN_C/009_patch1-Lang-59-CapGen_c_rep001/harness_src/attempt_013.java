package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        int ctorMode = data.consumeInt(0, 2);
        if (ctorMode == 0) {
            sb = new StrBuilder();
        } else if (ctorMode == 1) {
            sb = new StrBuilder(data.consumeString(64));
        } else {
            sb = new StrBuilder(data.consumeInt(-32, 256));
        }

        if (data.consumeBoolean()) {
            sb.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            sb.setNewLineText(data.consumeBoolean() ? null : data.consumeString(16));
        }

        int preOps = data.consumeInt(0, 8);
        for (int i = 0; i < preOps; i++) {
            switch (data.consumeInt(0, 6)) {
                case 0:
                    sb.append(data.consumeString(32));
                    break;
                case 1:
                    sb.append(data.consumeBoolean() ? null : data.consumeAsciiString(32));
                    break;
                case 2:
                    sb.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
                    break;
                case 3:
                    sb.appendNewLine();
                    break;
                case 4:
                    sb.ensureCapacity(data.consumeInt(0, 512));
                    break;
                case 5:
                    if (data.consumeBoolean()) {
                        sb.clear();
                    } else {
                        sb.minimizeCapacity();
                    }
                    break;
                default:
                    if (sb.length() > 0) {
                        int start = data.consumeInt(0, sb.length());
                        int end = data.consumeInt(start, sb.length());
                        sb.substring(start, end);
                    }
                    break;
            }
        }

        int calls = data.consumeInt(1, 6);
        for (int i = 0; i < calls; i++) {
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
                    obj = new StringBuilder(data.consumeString(64));
                    break;
                case 6:
                    obj = new StringBuffer(data.consumeAsciiString(64));
                    break;
                default:
                    obj = new StrBuilder(data.consumeString(64));
                    break;
            }

            int rawWidth = data.consumeInt();
            int width;
            if (rawWidth > 8192) {
                width = 8192;
            } else if (rawWidth < -8192) {
                width = -8192;
            } else {
                width = rawWidth;
            }

            char padChar = (char) (data.consumeByte() & 0xff);
            sb.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                sb.toString();
            }
            if (data.consumeBoolean() && sb.length() > 0) {
                int start = data.consumeInt(0, sb.length());
                int end = data.consumeInt(start, sb.length());
                sb.substring(start, end);
            }
        }

        if (data.consumeBoolean()) {
            Object finalObj = data.consumeBoolean() ? null : data.consumeRemainingAsString();
            int width = data.remainingBytes() > 0 ? data.consumeInt(-32, 256) : 0;
            char padChar = (char) (data.consumeByte() & 0xff);
            sb.appendFixedWidthPadRight(finalObj, width, padChar);
        }
    }
}