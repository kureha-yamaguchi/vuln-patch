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
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 6:
                        array[i] = data.consumeBytes(16);
                        break;
                    case 7:
                        array[i] = new StringBuilder(data.consumeString(16));
                        break;
                    default:
                        array[i] = new Object[] {
                            data.consumeString(8),
                            Integer.valueOf(data.consumeInt()),
                            null
                        };
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

        int startIndex;
        int endIndex;
        if (array != null && data.consumeBoolean()) {
            int len = array.length;
            switch (data.consumeInt(0, 9)) {
                case 0:
                    startIndex = 0;
                    break;
                case 1:
                    startIndex = len;
                    break;
                case 2:
                    startIndex = -1;
                    break;
                case 3:
                    startIndex = len + 1;
                    break;
                default:
                    startIndex = data.consumeInt(-2, len + 2);
                    break;
            }
            switch (data.consumeInt(0, 9)) {
                case 0:
                    endIndex = 0;
                    break;
                case 1:
                    endIndex = len;
                    break;
                case 2:
                    endIndex = -1;
                    break;
                case 3:
                    endIndex = len + 1;
                    break;
                default:
                    endIndex = data.consumeInt(-2, len + 2);
                    break;
            }
        } else {
            startIndex = data.consumeInt();
            endIndex = data.consumeInt();
        }

        if (data.consumeBoolean()) {
            StringUtils.join(array, charSeparator, startIndex, endIndex);
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
        } else {
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
            StringUtils.join(array, charSeparator, startIndex, endIndex);
        }

        if (array != null && data.remainingBytes() > 0) {
            int len = array.length;
            int[][] extraRanges = new int[][] {
                {0, len},
                {0, 0},
                {len, len},
                {-1, len},
                {0, len + 1},
                {1, len},
                {0, len - 1}
            };
            int which = data.consumeInt(0, extraRanges.length - 1);
            int s = extraRanges[which][0];
            int e = extraRanges[which][1];
            if (data.consumeBoolean()) {
                StringUtils.join(array, charSeparator, s, e);
            } else {
                StringUtils.join(array, stringSeparator, s, e);
            }
        }
    }
}