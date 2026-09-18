package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;

        if (data.consumeBoolean()) {
            array = null;
        } else {
            int length = data.consumeInt(0, 32);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                switch (data.consumeInt(0, 6)) {
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
                    default:
                        array[i] = new String(data.consumeBytes(32));
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);

        String stringSeparator;
        switch (data.consumeInt(0, 3)) {
            case 0:
                stringSeparator = null;
                break;
            case 1:
                stringSeparator = data.consumeString(32);
                break;
            case 2:
                stringSeparator = data.consumeAsciiString(32);
                break;
            default:
                stringSeparator = data.consumeRemainingAsString();
                break;
        }

        int startIndexAny = data.consumeInt();
        int endIndexAny = data.consumeInt();

        StringUtils.join(array, charSeparator, startIndexAny, endIndexAny);
        StringUtils.join(array, stringSeparator, startIndexAny, endIndexAny);

        int len = array == null ? 0 : array.length;

        int startIndexNear;
        switch (data.consumeInt(0, 7)) {
            case 0:
                startIndexNear = -1;
                break;
            case 1:
                startIndexNear = 0;
                break;
            case 2:
                startIndexNear = 1;
                break;
            case 3:
                startIndexNear = len - 1;
                break;
            case 4:
                startIndexNear = len;
                break;
            case 5:
                startIndexNear = len + 1;
                break;
            case 6:
                startIndexNear = Integer.MIN_VALUE;
                break;
            default:
                startIndexNear = Integer.MAX_VALUE;
                break;
        }

        int endIndexNear;
        switch (data.consumeInt(0, 7)) {
            case 0:
                endIndexNear = -1;
                break;
            case 1:
                endIndexNear = 0;
                break;
            case 2:
                endIndexNear = 1;
                break;
            case 3:
                endIndexNear = len - 1;
                break;
            case 4:
                endIndexNear = len;
                break;
            case 5:
                endIndexNear = len + 1;
                break;
            case 6:
                endIndexNear = Integer.MIN_VALUE;
                break;
            default:
                endIndexNear = Integer.MAX_VALUE;
                break;
        }

        StringUtils.join(array, charSeparator, startIndexNear, endIndexNear);
        StringUtils.join(array, stringSeparator, startIndexNear, endIndexNear);

        if (array != null) {
            int validStart = len == 0 ? 0 : data.consumeInt(0, len);
            int validEnd = len == 0 ? 0 : data.consumeInt(0, len);

            StringUtils.join(array, charSeparator, validStart, validEnd);
            StringUtils.join(array, stringSeparator, validStart, validEnd);

            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);

            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
            }
        }
    }
}