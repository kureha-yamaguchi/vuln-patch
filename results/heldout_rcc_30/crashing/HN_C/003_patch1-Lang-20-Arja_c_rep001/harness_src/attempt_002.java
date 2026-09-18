package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int size = data.consumeInt(0, 16);
            array = new Object[size];
            for (int i = 0; i < size; i++) {
                switch (data.consumeInt(0, 8)) {
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
                        array[i] = Long.valueOf(data.consumeInt());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 7:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeString(16);
        } else {
            stringSeparator = data.consumeAsciiString(16);
        }

        if (array != null) {
            int len = array.length;

            int validStart = data.consumeInt(0, len);
            int validEnd = data.consumeInt(validStart, len);
            StringUtils.join(array, charSeparator, validStart, validEnd);
            StringUtils.join(array, stringSeparator, validStart, validEnd);

            int nearStart = data.consumeInt(-2, len + 2);
            int nearEnd = data.consumeInt(-2, len + 2);
            StringUtils.join(array, charSeparator, nearStart, nearEnd);
            StringUtils.join(array, stringSeparator, nearStart, nearEnd);

            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, len, len);
            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, len);
                StringUtils.join(array, stringSeparator, 0, len);
            }
        }

        int anyStart = data.consumeInt();
        int anyEnd = data.consumeInt();
        StringUtils.join(array, charSeparator, anyStart, anyEnd);
        StringUtils.join(array, stringSeparator, anyStart, anyEnd);
    }
}