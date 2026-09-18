package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        int ctorChoice = data.consumeInt(0, 2);
        if (ctorChoice == 0) {
            builder = new StrBuilder();
        } else if (ctorChoice == 1) {
            builder = new StrBuilder(data.consumeString(64));
        } else {
            builder = new StrBuilder(data.consumeInt(0, 128));
        }

        if (data.consumeBoolean()) {
            if (data.consumeBoolean()) {
                builder.setNullText(null);
            } else {
                builder.setNullText(data.consumeString(32));
            }
        }

        int preOps = data.consumeInt(0, 4);
        for (int i = 0; i < preOps; i++) {
            int op = data.consumeInt(0, 3);
            if (op == 0) {
                builder.append(data.consumeString(64));
            } else if (op == 1) {
                builder.append(data.consumeAsciiString(64));
            } else if (op == 2) {
                builder.append((Object) Integer.valueOf(data.consumeInt()));
            } else {
                builder.append(data.consumeBoolean() ? null : data.consumeString(16));
            }
        }

        int calls = data.consumeInt(1, 4);
        for (int i = 0; i < calls; i++) {
            Object obj;
            int objChoice = data.consumeInt(0, 7);
            if (objChoice == 0) {
                obj = null;
            } else if (objChoice == 1) {
                obj = data.consumeString(256);
            } else if (objChoice == 2) {
                obj = data.consumeAsciiString(256);
            } else if (objChoice == 3) {
                obj = new StringBuffer(data.consumeString(256));
            } else if (objChoice == 4) {
                obj = new StrBuilder(data.consumeString(256));
            } else if (objChoice == 5) {
                obj = Integer.valueOf(data.consumeInt());
            } else if (objChoice == 6) {
                obj = Boolean.valueOf(data.consumeBoolean());
            } else {
                obj = builder;
            }

            String asString = (obj == null) ? builder.getNullText() : obj.toString();
            int width;
            if (asString == null) {
                width = data.consumeBoolean() ? data.consumeInt(1, 64) : data.consumeInt(-16, 64);
            } else if (asString.length() > 1 && data.consumeBoolean()) {
                width = data.consumeInt(1, asString.length() - 1);
            } else {
                width = data.consumeInt(-16, 256);
            }

            char padChar = (char) data.consumeInt(0, 65535);
            builder.appendFixedWidthPadRight(obj, width, padChar);
        }
    }
}