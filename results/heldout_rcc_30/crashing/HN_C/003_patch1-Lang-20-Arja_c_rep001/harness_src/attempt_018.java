package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;

        if (data.consumeBoolean()) {
            array = null;
        } else {
            int len = data.consumeInt(0, 16);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                if (data.consumeBoolean()) {
                    array[i] = null;
                    continue;
                }
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        array[i] = data.consumeString(32);
                        break;
                    case 1:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 2:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 3:
                        array[i] = Long.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 7:
                        array[i] = new StringBuffer(data.consumeAsciiString(32));
                        break;
                    case 8:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = "";
                        break;
                }
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

        int startIndex1 = data.consumeInt();
        int endIndex1 = data.consumeInt();
        StringUtils.join(array, charSeparator, startIndex1, endIndex1);

        int startIndex2;
        int endIndex2;
        if (array != null && data.consumeBoolean()) {
            int len = array.length;
            startIndex2 = data.consumeInt(-2, len + 2);
            endIndex2 = data.consumeInt(-2, len + 2);
        } else {
            startIndex2 = data.consumeInt();
            endIndex2 = data.consumeInt();
        }
        StringUtils.join(array, stringSeparator, startIndex2, endIndex2);

        if (array != null) {
            int len = array.length;

            int startIndex3 = data.consumeBoolean() ? 0 : len;
            int endIndex3 = data.consumeBoolean() ? len : 0;
            StringUtils.join(array, charSeparator, startIndex3, endIndex3);

            int startIndex4 = data.consumeInt(-1, len + 1);
            int endIndex4 = startIndex4 + data.consumeInt(-1, 2);
            StringUtils.join(array, stringSeparator, startIndex4, endIndex4);
        } else {
            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);
        }
    }
}