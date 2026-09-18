package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int size = data.consumeInt(0, 16);
        Object[] array = new Object[size];

        for (int i = 0; i < size; i++) {
            switch (data.consumeInt(0, 7)) {
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
                    array[i] = Long.valueOf(data.consumeInt());
                    break;
                case 5:
                    array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                    break;
                case 6:
                    array[i] = Boolean.valueOf(data.consumeBoolean());
                    break;
                default:
                    array[i] = new String(data.consumeBytes(32));
                    break;
            }
        }

        Object[] maybeNullArray = data.consumeBoolean() ? null : array;
        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator = data.consumeBoolean() ? null : data.consumeString(16);

        int rawStart1 = data.consumeInt();
        int rawEnd1 = data.consumeInt();
        StringUtils.join(maybeNullArray, charSeparator, rawStart1, rawEnd1);

        int rawStart2 = data.consumeInt();
        int rawEnd2 = data.consumeInt();
        StringUtils.join(maybeNullArray, stringSeparator, rawStart2, rawEnd2);

        int len = array.length;
        int boundedStart = data.consumeInt(-2, len + 2);
        int boundedEnd = data.consumeInt(-2, len + 2);
        StringUtils.join(array, charSeparator, boundedStart, boundedEnd);
        StringUtils.join(array, stringSeparator, boundedStart, boundedEnd);

        int startAtBoundary;
        int endAtBoundary;
        switch (data.consumeInt(0, 7)) {
            case 0:
                startAtBoundary = 0;
                endAtBoundary = len;
                break;
            case 1:
                startAtBoundary = len;
                endAtBoundary = len;
                break;
            case 2:
                startAtBoundary = 0;
                endAtBoundary = 0;
                break;
            case 3:
                startAtBoundary = -1;
                endAtBoundary = len;
                break;
            case 4:
                startAtBoundary = 0;
                endAtBoundary = len + 1;
                break;
            case 5:
                startAtBoundary = len - 1;
                endAtBoundary = len;
                break;
            case 6:
                startAtBoundary = 1;
                endAtBoundary = 0;
                break;
            default:
                startAtBoundary = len + 1;
                endAtBoundary = len + 1;
                break;
        }

        StringUtils.join(array, charSeparator, startAtBoundary, endAtBoundary);
        StringUtils.join(array, stringSeparator, startAtBoundary, endAtBoundary);
    }
}