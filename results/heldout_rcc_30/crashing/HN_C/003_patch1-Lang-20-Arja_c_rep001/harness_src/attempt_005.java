package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int length;
        if (data.remainingBytes() > 0) {
            length = data.consumeInt(0, Math.min(16, data.remainingBytes()));
        } else {
            length = 0;
        }

        Object[] array = new Object[length];
        for (int i = 0; i < length; i++) {
            int choice = data.remainingBytes() > 0 ? data.consumeInt(0, 7) : 0;
            switch (choice) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = data.consumeString(32);
                    break;
                case 2:
                    array[i] = data.consumeAsciiString(32);
                    break;
                case 3:
                    array[i] = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    array[i] = Byte.valueOf(data.consumeByte());
                    break;
                case 5:
                    array[i] = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 6:
                    array[i] = data.consumeBytes(Math.min(16, data.remainingBytes()));
                    break;
                default:
                    array[i] = data.consumeRemainingAsString();
                    break;
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = "";
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(16);
        } else {
            stringSeparator = data.consumeString(16);
        }

        StringUtils.join((Object[]) null, charSeparator, data.consumeInt(), data.consumeInt());
        StringUtils.join((Object[]) null, stringSeparator, data.consumeInt(), data.consumeInt());

        int safeStart = 0;
        int safeEnd = length;
        if (length > 0) {
            safeStart = data.consumeInt(0, length);
            safeEnd = data.consumeInt(safeStart, length);
        }

        StringUtils.join(array, charSeparator, safeStart, safeEnd);
        StringUtils.join(array, stringSeparator, safeStart, safeEnd);

        if (length > 0) {
            int singleIndex = data.consumeInt(0, length - 1);
            StringUtils.join(array, charSeparator, singleIndex, singleIndex + 1);
            StringUtils.join(array, stringSeparator, singleIndex, singleIndex + 1);
        }

        StringUtils.join(array, charSeparator, 0, 0);
        StringUtils.join(array, stringSeparator, 0, 0);
        StringUtils.join(array, charSeparator, length, length);
        StringUtils.join(array, stringSeparator, length, length);

        int rawStart = data.consumeInt();
        int rawEnd = data.consumeInt();

        if (data.consumeBoolean()) {
            rawStart = rawStart % (length + 3) - 1;
        }
        if (data.consumeBoolean()) {
            rawEnd = rawEnd % (length + 3) - 1;
        }

        if (data.consumeBoolean()) {
            rawStart = length == 0 ? 0 : length;
        }
        if (data.consumeBoolean()) {
            rawEnd = length == 0 ? 1 : length + 1;
        }

        if (data.consumeBoolean()) {
            StringUtils.join(array, charSeparator, rawStart, rawEnd);
        } else {
            StringUtils.join(array, stringSeparator, rawStart, rawEnd);
        }
    }
}