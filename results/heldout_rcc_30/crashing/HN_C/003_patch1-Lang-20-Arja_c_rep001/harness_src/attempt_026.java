package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;

        if (data.consumeBoolean()) {
            array = null;
        } else {
            int maxLen = Math.min(32, Math.max(0, data.remainingBytes()));
            int len = maxLen == 0 ? 0 : data.consumeInt(0, maxLen);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                int choice = data.consumeInt(0, 7);
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
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = new String(data.consumeBytes(16));
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeString(32);
        } else {
            stringSeparator = data.consumeAsciiString(32);
        }

        int startIndex;
        int endIndex;

        if (array == null || data.consumeBoolean()) {
            startIndex = data.consumeInt();
            endIndex = data.consumeInt();
        } else {
            int len = array.length;
            int mode = data.consumeInt(0, 8);
            switch (mode) {
                case 0:
                    startIndex = 0;
                    endIndex = len;
                    break;
                case 1:
                    startIndex = 0;
                    endIndex = 0;
                    break;
                case 2:
                    startIndex = len;
                    endIndex = len;
                    break;
                case 3:
                    startIndex = -1;
                    endIndex = len;
                    break;
                case 4:
                    startIndex = 0;
                    endIndex = len + 1;
                    break;
                case 5:
                    startIndex = len == 0 ? 0 : len - 1;
                    endIndex = len;
                    break;
                case 6:
                    startIndex = len;
                    endIndex = 0;
                    break;
                case 7:
                    startIndex = data.consumeInt(-2, len + 2);
                    endIndex = data.consumeInt(-2, len + 2);
                    break;
                default:
                    startIndex = data.consumeInt();
                    endIndex = data.consumeInt();
                    break;
            }
        }

        StringUtils.join(array, charSeparator, startIndex, endIndex);
        StringUtils.join(array, stringSeparator, startIndex, endIndex);

        if (array != null) {
            int len = array.length;
            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);

            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
            }

            StringUtils.join(array, charSeparator, len, len);
            StringUtils.join(array, stringSeparator, len, len);
            StringUtils.join(array, charSeparator, 0, Math.max(0, len - 1));
            StringUtils.join(array, stringSeparator, 0, Math.max(0, len - 1));
        }
    }
}