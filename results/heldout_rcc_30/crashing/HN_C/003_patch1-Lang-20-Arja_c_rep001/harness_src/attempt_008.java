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
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 3:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 4:
                        array[i] = Byte.valueOf(data.consumeByte());
                        break;
                    default:
                        array[i] = new StringBuilder(data.consumeAsciiString(32));
                        break;
                }
            }
        }

        char charSeparator = (char) data.consumeInt(0, Character.MAX_VALUE);
        String stringSeparator;
        switch (data.consumeInt(0, 2)) {
            case 0:
                stringSeparator = null;
                break;
            case 1:
                stringSeparator = "";
                break;
            default:
                stringSeparator = data.consumeString(16);
                break;
        }

        StringUtils.join((Object[]) null, charSeparator, data.consumeInt(), data.consumeInt());
        StringUtils.join((Object[]) null, stringSeparator, data.consumeInt(), data.consumeInt());

        if (array == null) {
            return;
        }

        int len = array.length;

        int safeStart = len == 0 ? 0 : data.consumeInt(0, len);
        int safeEnd = len == 0 ? 0 : data.consumeInt(safeStart, len);
        StringUtils.join(array, charSeparator, safeStart, safeEnd);
        StringUtils.join(array, stringSeparator, safeStart, safeEnd);

        int reverseA = len == 0 ? 0 : data.consumeInt(0, len);
        int reverseB = len == 0 ? 0 : data.consumeInt(0, len);
        int reverseStart = Math.max(reverseA, reverseB);
        int reverseEnd = Math.min(reverseA, reverseB);
        StringUtils.join(array, charSeparator, reverseStart, reverseEnd);
        StringUtils.join(array, stringSeparator, reverseStart, reverseEnd);

        int boundaryStart;
        switch (data.consumeInt(0, 7)) {
            case 0:
                boundaryStart = -2;
                break;
            case 1:
                boundaryStart = -1;
                break;
            case 2:
                boundaryStart = 0;
                break;
            case 3:
                boundaryStart = len == 0 ? 0 : len - 1;
                break;
            case 4:
                boundaryStart = len;
                break;
            case 5:
                boundaryStart = len + 1;
                break;
            case 6:
                boundaryStart = len + 2;
                break;
            default:
                boundaryStart = data.consumeInt();
                break;
        }

        int boundaryEnd;
        switch (data.consumeInt(0, 7)) {
            case 0:
                boundaryEnd = -2;
                break;
            case 1:
                boundaryEnd = -1;
                break;
            case 2:
                boundaryEnd = 0;
                break;
            case 3:
                boundaryEnd = len == 0 ? 0 : len - 1;
                break;
            case 4:
                boundaryEnd = len;
                break;
            case 5:
                boundaryEnd = len + 1;
                break;
            case 6:
                boundaryEnd = len + 2;
                break;
            default:
                boundaryEnd = data.consumeInt();
                break;
        }

        if (data.consumeBoolean()) {
            StringUtils.join(array, charSeparator, boundaryStart, boundaryEnd);
            StringUtils.join(array, stringSeparator, boundaryStart, boundaryEnd);
        } else {
            StringUtils.join(array, stringSeparator, boundaryStart, boundaryEnd);
            StringUtils.join(array, charSeparator, boundaryStart, boundaryEnd);
        }
    }
}