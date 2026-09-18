package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int maxLen = Math.min(32, data.remainingBytes() + 1);
            int len = data.consumeInt(0, maxLen);
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
                        array[i] = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) data.consumeInt(0, 65535));
                        break;
                    case 6:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 7:
                        array[i] = new String(data.consumeBytes(data.consumeInt(0, 16)));
                        break;
                    default:
                        array[i] = new StringBuilder(data.consumeAsciiString(16));
                        break;
                }
            }
        }

        char charSeparator = (char) data.consumeInt(0, 65535);
        String stringSeparator;
        switch (data.consumeInt(0, 3)) {
            case 0:
                stringSeparator = null;
                break;
            case 1:
                stringSeparator = data.consumeString(16);
                break;
            case 2:
                stringSeparator = data.consumeAsciiString(16);
                break;
            default:
                stringSeparator = data.consumeRemainingAsString();
                break;
        }

        if (array != null) {
            int len = array.length;

            int safeStart = data.consumeInt(0, len);
            int safeEnd = data.consumeInt(0, len);
            StringUtils.join(array, charSeparator, safeStart, safeEnd);
            StringUtils.join(array, stringSeparator, safeStart, safeEnd);

            int orderedStart = data.consumeInt(0, len);
            int orderedEnd = data.consumeInt(orderedStart, len);
            StringUtils.join(array, charSeparator, orderedStart, orderedEnd);
            StringUtils.join(array, stringSeparator, orderedStart, orderedEnd);

            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, len);
                StringUtils.join(array, stringSeparator, 0, len);
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
            } else {
                StringUtils.join(array, charSeparator, 0, 0);
                StringUtils.join(array, stringSeparator, 0, 0);
            }
        } else {
            StringUtils.join((Object[]) null, charSeparator, 0, 0);
            StringUtils.join((Object[]) null, stringSeparator, 0, 0);
        }

        int bound = array == null ? 4 : array.length + 4;
        int startIndex = data.consumeInt(-bound, bound);
        int endIndex = data.consumeInt(-bound, bound);

        if (data.consumeBoolean()) {
            StringUtils.join(array, charSeparator, startIndex, endIndex);
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
        } else {
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
            StringUtils.join(array, charSeparator, startIndex, endIndex);
        }
    }
}