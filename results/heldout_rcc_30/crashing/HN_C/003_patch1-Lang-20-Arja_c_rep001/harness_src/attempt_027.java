package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int len = data.consumeInt(0, 32);
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
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Byte.valueOf(data.consumeByte());
                        break;
                    case 6:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
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

        int len = array == null ? 0 : array.length;

        int rawA = data.consumeInt();
        int rawB = data.consumeInt();

        int startIndex;
        switch (data.consumeInt(0, 9)) {
            case 0:
                startIndex = rawA;
                break;
            case 1:
                startIndex = rawA % 64;
                break;
            case 2:
                startIndex = -1;
                break;
            case 3:
                startIndex = 0;
                break;
            case 4:
                startIndex = 1;
                break;
            case 5:
                startIndex = len == 0 ? 0 : len - 1;
                break;
            case 6:
                startIndex = len;
                break;
            case 7:
                startIndex = len + 1;
                break;
            case 8:
                startIndex = -len - 1;
                break;
            default:
                startIndex = data.consumeInt(-16, 16);
                break;
        }

        int endIndex;
        switch (data.consumeInt(0, 9)) {
            case 0:
                endIndex = rawB;
                break;
            case 1:
                endIndex = rawB % 64;
                break;
            case 2:
                endIndex = -1;
                break;
            case 3:
                endIndex = 0;
                break;
            case 4:
                endIndex = 1;
                break;
            case 5:
                endIndex = len == 0 ? 0 : len - 1;
                break;
            case 6:
                endIndex = len;
                break;
            case 7:
                endIndex = len + 1;
                break;
            case 8:
                endIndex = startIndex;
                break;
            default:
                endIndex = startIndex + data.consumeInt(-16, 16);
                break;
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);

        String stringSeparator;
        switch (data.consumeInt(0, 4)) {
            case 0:
                stringSeparator = null;
                break;
            case 1:
                stringSeparator = "";
                break;
            case 2:
                stringSeparator = String.valueOf((char) (data.consumeByte() & 0xFF));
                break;
            case 3:
                stringSeparator = data.consumeAsciiString(16);
                break;
            default:
                stringSeparator = data.consumeString(16);
                break;
        }

        StringUtils.join(array, charSeparator, startIndex, endIndex);
        StringUtils.join(array, stringSeparator, startIndex, endIndex);

        if (array != null) {
            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);
            StringUtils.join(array, charSeparator, len, len);
            StringUtils.join(array, stringSeparator, len, len);

            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
            }
        }
    }
}