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
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 6:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 7:
                        array[i] = new String(data.consumeBytes(16));
                        break;
                    default:
                        array[i] = new StringBuilder(data.consumeAsciiString(16));
                        break;
                }
            }
        }

        int len = array == null ? 0 : array.length;

        int startIndex1;
        int endIndex1;
        switch (data.consumeInt(0, 7)) {
            case 0:
                startIndex1 = data.consumeInt();
                endIndex1 = data.consumeInt();
                break;
            case 1:
                startIndex1 = data.consumeInt(-len - 2, len + 2);
                endIndex1 = data.consumeInt(-len - 2, len + 2);
                break;
            case 2:
                startIndex1 = 0;
                endIndex1 = len;
                break;
            case 3:
                startIndex1 = len == 0 ? 0 : len - 1;
                endIndex1 = len;
                break;
            case 4:
                startIndex1 = len;
                endIndex1 = len + 1;
                break;
            case 5:
                startIndex1 = -1;
                endIndex1 = len;
                break;
            case 6:
                startIndex1 = 0;
                endIndex1 = len + data.consumeInt(0, 2);
                break;
            default:
                startIndex1 = data.consumeBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                endIndex1 = data.consumeBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                break;
        }

        int startIndex2;
        int endIndex2;
        switch (data.consumeInt(0, 7)) {
            case 0:
                startIndex2 = data.consumeInt();
                endIndex2 = data.consumeInt();
                break;
            case 1:
                startIndex2 = data.consumeInt(-len - 2, len + 2);
                endIndex2 = data.consumeInt(-len - 2, len + 2);
                break;
            case 2:
                startIndex2 = 0;
                endIndex2 = len;
                break;
            case 3:
                startIndex2 = len;
                endIndex2 = len;
                break;
            case 4:
                startIndex2 = 1;
                endIndex2 = 0;
                break;
            case 5:
                startIndex2 = 0;
                endIndex2 = len + 1;
                break;
            case 6:
                startIndex2 = -1;
                endIndex2 = -1;
                break;
            default:
                startIndex2 = data.consumeBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                endIndex2 = data.consumeBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                break;
        }

        char separatorChar = (char) (data.consumeByte() & 0xff);

        String separatorString;
        if (data.consumeBoolean()) {
            separatorString = null;
        } else if (data.consumeBoolean()) {
            separatorString = data.consumeString(32);
        } else if (data.consumeBoolean()) {
            separatorString = data.consumeAsciiString(32);
        } else {
            separatorString = data.consumeRemainingAsString();
        }

        if (data.consumeBoolean()) {
            StringUtils.join(array, separatorChar, startIndex1, endIndex1);
            StringUtils.join(array, separatorString, startIndex2, endIndex2);
        } else {
            StringUtils.join(array, separatorString, startIndex2, endIndex2);
            StringUtils.join(array, separatorChar, startIndex1, endIndex1);
        }
    }
}