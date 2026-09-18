package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 4)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeString(64));
                break;
            case 2:
                builder = new StrBuilder(data.consumeInt(-16, 256));
                break;
            case 3:
                builder = new StrBuilder(data.consumeAsciiString(64));
                break;
            default:
                builder = new StrBuilder();
                break;
        }

        if (data.consumeBoolean()) {
            String nullText = data.consumeBoolean() ? null : data.consumeString(32);
            builder.setNullText(nullText);
        }
        if (data.consumeBoolean()) {
            String newLineText = data.consumeBoolean() ? null : data.consumeAsciiString(16);
            builder.setNewLineText(newLineText);
        }

        int prefixOps = data.consumeInt(0, 8);
        for (int i = 0; i < prefixOps; i++) {
            switch (data.consumeInt(0, 11)) {
                case 0:
                    builder.append(data.consumeString(32));
                    break;
                case 1:
                    builder.append(data.consumeAsciiString(32));
                    break;
                case 2:
                    builder.append(data.consumeBoolean());
                    break;
                case 3:
                    builder.append(data.consumeInt());
                    break;
                case 4:
                    builder.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
                    break;
                case 5:
                    builder.appendNewLine();
                    break;
                case 6:
                    builder.clear();
                    break;
                case 7:
                    builder.ensureCapacity(data.consumeInt(-16, 512));
                    break;
                case 8:
                    builder.minimizeCapacity();
                    break;
                case 9:
                    builder.setLength(data.consumeInt(0, 64));
                    break;
                case 10:
                    builder.reverse();
                    break;
                default:
                    builder.trim();
                    break;
            }
        }

        int calls = data.consumeInt(1, 6);
        for (int i = 0; i < calls; i++) {
            Object obj;
            switch (data.consumeInt(0, 6)) {
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
                    obj = new StrBuilder(data.consumeString(64));
                    break;
                default:
                    byte[] bytes = data.consumeBytes(64);
                    obj = new String(bytes);
                    break;
            }

            int width;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    width = data.consumeInt();
                    break;
                case 1:
                    width = data.consumeInt(-4, 4);
                    break;
                case 2:
                    width = data.consumeInt(0, 32);
                    break;
                case 3:
                    width = data.consumeInt(33, 256);
                    break;
                case 4:
                    width = 0;
                    break;
                default:
                    width = -1;
                    break;
            }

            char padChar = (char) (data.consumeByte() & 0xff);

            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                builder.length();
                builder.capacity();
                builder.size();
                builder.toString();
                if (builder.length() > 0) {
                    int start = data.consumeInt(0, builder.length() - 1);
                    int end = data.consumeInt(start, builder.length());
                    builder.substring(start, end);
                }
            }
        }
    }
}