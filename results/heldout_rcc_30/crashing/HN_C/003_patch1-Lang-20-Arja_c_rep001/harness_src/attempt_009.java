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
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    default:
                        array[i] = data.consumeBytes(16);
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator1 = data.consumeBoolean() ? null : data.consumeString(16);
        String stringSeparator2 = data.consumeBoolean() ? "" : data.consumeAsciiString(16);

        int rawStart = data.consumeInt();
        int rawEnd = data.consumeInt();

        int len = array == null ? 0 : array.length;
        int[] starts = new int[] {
            rawStart,
            rawEnd,
            0,
            len,
            len == 0 ? 0 : len - 1,
            -1,
            len + 1
        };
        int[] ends = new int[] {
            rawEnd,
            rawStart,
            0,
            len,
            len == 0 ? 0 : len - 1,
            -1,
            len + 1
        };

        for (int i = 0; i < starts.length; i++) {
            for (int j = 0; j < ends.length; j++) {
                StringUtils.join(array, charSeparator, starts[i], ends[j]);
                StringUtils.join(array, stringSeparator1, starts[i], ends[j]);
                StringUtils.join(array, stringSeparator2, starts[i], ends[j]);
            }
        }
    }
}