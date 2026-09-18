package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        switch (data.consumeInt(0, 2)) {
            case 0:
                sb = new StrBuilder();
                break;
            case 1:
                sb = new StrBuilder(data.consumeInt(0, 256));
                break;
            default:
                sb = new StrBuilder(data.consumeBoolean() ? null : data.consumeString(64));
                break;
        }

        switch (data.consumeInt(0, 3)) {
            case 0:
                sb.setNullText(null);
                break;
            case 1:
                sb.setNullText("");
                break;
            case 2:
                sb.setNullText(data.consumeString(16));
                break;
            default:
                sb.setNullText(data.consumeAsciiString(16));
                break;
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
        if (data.consumeBoolean()) {
            sb.ensureCapacity(data.consumeInt(0, 512));
        }
        if (data.consumeBoolean()) {
            sb.setLength(data.consumeInt(0, 128));
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
                obj = new StringBuffer(data.consumeString(64));
                break;
            default:
                obj = data.consumeRemainingAsString();
                break;
        }

        String objString = (obj == null ? sb.getNullText() : obj.toString());
        int width;
        if (objString == null) {
            switch (data.consumeInt(0, 3)) {
                case 0:
                    width = data.consumeInt(1, 64);
                    break;
                case 1:
                    width = 0;
                    break;
                case 2:
                    width = -data.consumeInt(0, 64);
                    break;
                default:
                    width = data.consumeInt();
                    break;
            }
        } else {
            int len = objString.length();
            switch (data.consumeInt(0, 6)) {
                case 0:
                    width = len;
                    break;
                case 1:
                    width = len + 1;
                    break;
                case 2:
                    width = len == 0 ? 0 : len - 1;
                    break;
                case 3:
                    width = 0;
                    break;
                case 4:
                    width = -data.consumeInt(0, 64);
                    break;
                case 5:
                    width = data.consumeInt(1, 64);
                    break;
                default:
                    width = data.consumeInt();
                    break;
            }
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
                    obj2 = data.consumeString(32);
                    break;
                case 2:
                    obj2 = Long.valueOf(data.consumeInt());
                    break;
                case 3:
                    obj2 = new StringBuffer(data.consumeAsciiString(32));
                    break;
                default:
                    obj2 = data.consumeRemainingAsString();
                    break;
            }

            int width2;
            if (data.consumeBoolean()) {
                width2 = data.consumeInt();
            } else {
                String s2 = (obj2 == null ? sb.getNullText() : obj2.toString());
                if (s2 == null) {
                    width2 = data.consumeInt(1, 32);
                } else {
                    switch (data.consumeInt(0, 2)) {
                        case 0:
                            width2 = s2.length();
                            break;
                        case 1:
                            width2 = s2.length() + data.consumeInt(0, 8);
                            break;
                        default:
                            width2 = s2.length() == 0 ? 0 : s2.length() - 1;
                            break;
                    }
                }
            }
            sb.appendFixedWidthPadRight(obj2, width2, (char) (data.consumeByte() & 0xff));
        }
    }
}