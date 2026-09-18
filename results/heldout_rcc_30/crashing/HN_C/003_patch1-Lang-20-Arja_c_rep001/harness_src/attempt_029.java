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
                        array[i] = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 7:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    default:
                        array[i] = data.consumeBytes(16);
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xff);
        String stringSeparator = data.consumeBoolean() ? null : data.consumeString(16);

        int basis = array == null ? data.consumeInt(0, 16) : array.length;
        int startIndex = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-basis - 4, basis + 4);
        int endIndex = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-basis - 4, basis + 4);

        if (data.consumeBoolean()) {
            StringUtils.join(array, charSeparator, startIndex, endIndex);
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
        } else {
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
            StringUtils.join(array, charSeparator, startIndex, endIndex);
        }

        if (array != null) {
            int len = array.length;

            int safeStart = data.consumeInt(0, len);
            int safeEnd = data.consumeInt(0, len);
            if (data.consumeBoolean() && safeStart > safeEnd) {
                int tmp = safeStart;
                safeStart = safeEnd;
                safeEnd = tmp;
            }

            StringUtils.join(array, charSeparator, safeStart, safeEnd);
            StringUtils.join(array, stringSeparator, safeStart, safeEnd);

            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);

            StringUtils.join(array, charSeparator, len, len);
            StringUtils.join(array, stringSeparator, len, len);

            if (len > 0) {
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
            }
        } else {
            StringUtils.join((Object[]) null, charSeparator, 0, 0);
            StringUtils.join((Object[]) null, stringSeparator, 0, 0);
        }
    }
}