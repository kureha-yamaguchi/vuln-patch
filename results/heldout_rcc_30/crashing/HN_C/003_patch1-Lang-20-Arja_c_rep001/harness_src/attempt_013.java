package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int length = data.consumeInt(0, 16);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                int kind = data.consumeInt(0, 7);
                switch (kind) {
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
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Byte.valueOf(data.consumeByte());
                        break;
                    case 6:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xff);
        String stringSeparator = data.consumeBoolean() ? null : data.consumeString(16);

        int len = array == null ? 0 : array.length;

        int start1 = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);
        int end1 = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);
        StringUtils.join(array, charSeparator, start1, end1);

        int start2 = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);
        int end2 = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);
        StringUtils.join(array, stringSeparator, start2, end2);

        if (array != null) {
            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);

            if (len > 0) {
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
            }

            StringUtils.join(array, charSeparator, len, len);
            StringUtils.join(array, stringSeparator, len, len);

            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);

            StringUtils.join(array, charSeparator, -1, len);
            StringUtils.join(array, stringSeparator, -1, len);

            StringUtils.join(array, charSeparator, 0, len + 1);
            StringUtils.join(array, stringSeparator, 0, len + 1);
        } else {
            StringUtils.join(array, charSeparator, data.consumeInt(), data.consumeInt());
            StringUtils.join(array, stringSeparator, data.consumeInt(), data.consumeInt());
        }
    }
}