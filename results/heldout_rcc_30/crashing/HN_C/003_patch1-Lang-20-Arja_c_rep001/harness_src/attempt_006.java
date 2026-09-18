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
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 7:
                        array[i] = new StringBuffer(data.consumeAsciiString(32));
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator = data.consumeBoolean() ? null : data.consumeString(16);

        int len = array == null ? 0 : array.length;

        int start1 = data.consumeInt();
        int end1 = data.consumeInt();

        int start2;
        switch (data.consumeInt(0, 8)) {
            case 0:
                start2 = -1;
                break;
            case 1:
                start2 = 0;
                break;
            case 2:
                start2 = 1;
                break;
            case 3:
                start2 = len - 1;
                break;
            case 4:
                start2 = len;
                break;
            case 5:
                start2 = len + 1;
                break;
            case 6:
                start2 = Integer.MIN_VALUE;
                break;
            case 7:
                start2 = Integer.MAX_VALUE;
                break;
            default:
                start2 = data.consumeInt();
                break;
        }

        int end2;
        switch (data.consumeInt(0, 8)) {
            case 0:
                end2 = -1;
                break;
            case 1:
                end2 = 0;
                break;
            case 2:
                end2 = 1;
                break;
            case 3:
                end2 = len - 1;
                break;
            case 4:
                end2 = len;
                break;
            case 5:
                end2 = len + 1;
                break;
            case 6:
                end2 = Integer.MIN_VALUE;
                break;
            case 7:
                end2 = Integer.MAX_VALUE;
                break;
            default:
                end2 = data.consumeInt();
                break;
        }

        StringUtils.join(array, charSeparator, start1, end1);
        StringUtils.join(array, stringSeparator, start1, end1);
        StringUtils.join(array, charSeparator, start2, end2);
        StringUtils.join(array, stringSeparator, start2, end2);

        if (array != null) {
            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);
            StringUtils.join(array, charSeparator, len, len);
            StringUtils.join(array, stringSeparator, len, len);
            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);
            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
            }
        }
    }
}