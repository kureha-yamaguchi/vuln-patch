package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
            int maxLen = Math.min(16, Math.max(0, data.remainingBytes()));
            int len = maxLen == 0 ? 0 : data.consumeInt(0, maxLen);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                array[i] = makeElement(data);
            }
        }

        int len = array == null ? 0 : array.length;
        int startIndex = chooseIndex(data, len);
        int endIndex = chooseIndex(data, len);

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(32);
        } else {
            stringSeparator = data.consumeString(32);
        }

        switch (data.consumeInt(0, 5)) {
            case 0:
                StringUtils.join(array, charSeparator, startIndex, endIndex);
                break;
            case 1:
                StringUtils.join(array, stringSeparator, startIndex, endIndex);
                break;
            case 2:
                StringUtils.join(array, charSeparator, startIndex, endIndex);
                StringUtils.join(array, stringSeparator, startIndex, endIndex);
                break;
            case 3:
                StringUtils.join(array, stringSeparator, startIndex, endIndex);
                StringUtils.join(array, charSeparator, startIndex, endIndex);
                break;
            case 4:
                StringUtils.join(array, charSeparator, endIndex, startIndex);
                break;
            default:
                StringUtils.join(array, stringSeparator, endIndex, startIndex);
                break;
        }
    }

    private static int chooseIndex(FuzzedDataProvider data, int len) {
        switch (data.consumeInt(0, 11)) {
            case 0:
                return Integer.MIN_VALUE;
            case 1:
                return -2;
            case 2:
                return -1;
            case 3:
                return 0;
            case 4:
                return 1;
            case 5:
                return len - 1;
            case 6:
                return len;
            case 7:
                return len + 1;
            case 8:
                return len + 2;
            case 9:
                return Integer.MAX_VALUE;
            case 10:
                return data.consumeInt();
            default:
                int low = len - 2;
                int high = len + 2;
                if (low > high) {
                    int tmp = low;
                    low = high;
                    high = tmp;
                }
                return data.consumeInt(low, high);
        }
    }

    private static Object makeElement(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 8)) {
            case 0:
                return null;
            case 1:
                return data.consumeString(32);
            case 2:
                return data.consumeAsciiString(32);
            case 3:
                return Integer.valueOf(data.consumeInt());
            case 4:
                return Long.valueOf(data.consumeInt());
            case 5:
                return Boolean.valueOf(data.consumeBoolean());
            case 6:
                return Character.valueOf((char) (data.consumeByte() & 0xFF));
            case 7:
                return data.consumeBytes(16);
            default:
                return new StringBuilder(data.consumeString(32));
        }
    }
}