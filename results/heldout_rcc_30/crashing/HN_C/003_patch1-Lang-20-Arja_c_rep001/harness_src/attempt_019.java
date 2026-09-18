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
                        array[i] = Long.valueOf((long) data.consumeInt());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
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

        char charSeparator = (char) (data.consumeByte() & 0xff);
        String stringSeparator = data.consumeBoolean() ? null : data.consumeString(16);

        if (array == null) {
            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);
        } else {
            int safeStart = data.consumeInt(0, array.length);
            int safeEnd = data.consumeInt(safeStart, array.length);

            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);
            StringUtils.join(array, charSeparator, safeStart, safeEnd);
            StringUtils.join(array, stringSeparator, safeStart, safeEnd);
            StringUtils.join(array, charSeparator, 0, array.length);
            StringUtils.join(array, stringSeparator, 0, array.length);
        }

        int len = array == null ? 0 : array.length;
        int edgeStart = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);
        int edgeEnd = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);

        StringUtils.join(array, charSeparator, edgeStart, edgeEnd);

        String secondSeparator;
        if (data.consumeBoolean()) {
            secondSeparator = null;
        } else if (data.consumeBoolean()) {
            secondSeparator = data.consumeAsciiString(8);
        } else {
            secondSeparator = data.consumeRemainingAsString();
        }

        int edgeStart2 = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);
        int edgeEnd2 = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);

        StringUtils.join(array, secondSeparator, edgeStart2, edgeEnd2);
    }
}