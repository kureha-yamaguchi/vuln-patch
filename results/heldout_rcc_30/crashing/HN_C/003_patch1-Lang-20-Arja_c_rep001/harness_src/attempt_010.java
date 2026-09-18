package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
            int size = data.consumeInt(0, 16);
            array = new Object[size];
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
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = new String(data.consumeBytes(32));
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xff);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(16);
        } else {
            stringSeparator = data.consumeString(16);
        }

        int length = array == null ? 0 : array.length;

        int[][] boundaryPairs = new int[][] {
            {0, 0},
            {0, length},
            {length, length},
            {-1, length},
            {0, length + 1},
            {-1, -1},
            {1, 0},
            {length - 1, length},
            {length, length + 1}
        };

        for (int i = 0; i < boundaryPairs.length; i++) {
            StringUtils.join(array, charSeparator, boundaryPairs[i][0], boundaryPairs[i][1]);
            StringUtils.join(array, stringSeparator, boundaryPairs[i][0], boundaryPairs[i][1]);
        }

        int extraCalls = data.remainingBytes() > 0 ? data.consumeInt(1, 8) : 1;
        for (int i = 0; i < extraCalls; i++) {
            int startIndex;
            int endIndex;

            if (data.consumeBoolean()) {
                startIndex = data.consumeInt();
                endIndex = data.consumeInt();
            } else {
                int span = length + 4;
                startIndex = data.consumeInt(-span, span);
                endIndex = data.consumeInt(-span, span);
            }

            if (data.consumeBoolean()) {
                StringUtils.join(array, charSeparator, startIndex, endIndex);
            } else {
                StringUtils.join(array, stringSeparator, startIndex, endIndex);
            }
        }
    }
}