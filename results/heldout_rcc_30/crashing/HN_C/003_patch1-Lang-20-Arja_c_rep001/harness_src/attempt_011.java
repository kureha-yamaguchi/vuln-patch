package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        int length = 0;

        if (data.consumeBoolean()) {
            array = null;
        } else {
            length = data.consumeInt(0, 16);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                switch (data.consumeInt(0, 9)) {
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
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 7:
                        array[i] = new StringBuffer(data.consumeAsciiString(32));
                        break;
                    case 8:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xff);
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

        int startIndex;
        int endIndex;

        if (array == null) {
            startIndex = data.consumeInt();
            endIndex = data.consumeInt();
        } else {
            switch (data.consumeInt(0, 5)) {
                case 0:
                    startIndex = data.consumeInt();
                    endIndex = data.consumeInt();
                    break;
                case 1:
                    startIndex = data.consumeInt(-2, length + 2);
                    endIndex = data.consumeInt(-2, length + 2);
                    break;
                case 2:
                    startIndex = 0;
                    endIndex = length;
                    break;
                case 3:
                    startIndex = length;
                    endIndex = length + 1;
                    break;
                case 4:
                    startIndex = data.consumeInt(0, length);
                    endIndex = data.consumeInt(startIndex, length);
                    break;
                default:
                    startIndex = data.consumeInt(-1, length);
                    endIndex = data.consumeInt(startIndex, length + 1);
                    break;
            }
        }

        StringUtils.join(array, charSeparator, startIndex, endIndex);
        StringUtils.join(array, stringSeparator, startIndex, endIndex);
    }
}