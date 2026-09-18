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
                switch (data.consumeInt(0, 7)) {
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
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = new String(data.consumeBytes(32));
                        break;
                    default:
                        array[i] = new StringBuilder(data.consumeString(32));
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

        int size = array == null ? 0 : array.length;

        int boundedStart = data.consumeInt(-2, size + 2);
        int boundedEnd = data.consumeInt(-2, size + 2);

        StringUtils.join(array, charSeparator, boundedStart, boundedEnd);
        StringUtils.join(array, stringSeparator, boundedStart, boundedEnd);

        if (array != null) {
            StringUtils.join(array, charSeparator, 0, size);
            StringUtils.join(array, stringSeparator, 0, size);

            StringUtils.join(array, charSeparator, size, size);
            StringUtils.join(array, stringSeparator, size, size);

            if (size > 0) {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);

                StringUtils.join(array, charSeparator, size - 1, size);
                StringUtils.join(array, stringSeparator, size - 1, size);
            }
        }

        int arbitraryStart = data.consumeInt();
        int arbitraryEnd = data.consumeInt();
        StringUtils.join(array, charSeparator, arbitraryStart, arbitraryEnd);
        StringUtils.join(array, stringSeparator, arbitraryStart, arbitraryEnd);
    }
}