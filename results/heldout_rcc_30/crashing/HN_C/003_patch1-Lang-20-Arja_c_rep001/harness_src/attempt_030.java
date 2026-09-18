package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
            int length = data.consumeInt(0, 32);
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
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Byte.valueOf(data.consumeByte());
                        break;
                    default:
                        array[i] = data.consumeBytes(16);
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(16);
        } else {
            stringSeparator = data.consumeString(16);
        }

        int rawStart1 = data.consumeInt();
        int rawEnd1 = data.consumeInt();
        int rawStart2 = data.consumeInt();
        int rawEnd2 = data.consumeInt();

        int boundaryStart = 0;
        int boundaryEnd = 0;
        if (array != null) {
            switch (data.consumeInt(0, 8)) {
                case 0:
                    boundaryStart = -1;
                    break;
                case 1:
                    boundaryStart = 0;
                    break;
                case 2:
                    boundaryStart = array.length == 0 ? 0 : array.length - 1;
                    break;
                case 3:
                    boundaryStart = array.length;
                    break;
                case 4:
                    boundaryStart = array.length + 1;
                    break;
                default:
                    boundaryStart = rawStart1;
                    break;
            }
            switch (data.consumeInt(0, 8)) {
                case 0:
                    boundaryEnd = -1;
                    break;
                case 1:
                    boundaryEnd = 0;
                    break;
                case 2:
                    boundaryEnd = array.length == 0 ? 0 : array.length - 1;
                    break;
                case 3:
                    boundaryEnd = array.length;
                    break;
                case 4:
                    boundaryEnd = array.length + 1;
                    break;
                default:
                    boundaryEnd = rawEnd1;
                    break;
            }
        } else {
            boundaryStart = rawStart1;
            boundaryEnd = rawEnd1;
        }

        StringUtils.join(array, charSeparator, rawStart1, rawEnd1);
        StringUtils.join(array, stringSeparator, rawStart2, rawEnd2);
        StringUtils.join(array, charSeparator, boundaryStart, boundaryEnd);
        StringUtils.join(array, stringSeparator, boundaryStart, boundaryEnd);

        if (array != null) {
            StringUtils.join(array, charSeparator, 0, array.length);
            StringUtils.join(array, stringSeparator, 0, array.length);
            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);
            StringUtils.join(array, charSeparator, array.length, array.length);
            StringUtils.join(array, stringSeparator, array.length, array.length);
            if (array.length > 0) {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
                StringUtils.join(array, charSeparator, array.length - 1, array.length);
                StringUtils.join(array, stringSeparator, array.length - 1, array.length);
            }
        }
    }
}