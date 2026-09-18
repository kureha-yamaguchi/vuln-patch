package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int len = data.consumeInt(0, 8);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
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
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 5:
                        array[i] = new StringBuilder(data.consumeString(16));
                        break;
                    case 6:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator = data.consumeBoolean() ? null : data.consumeString(16);

        StringUtils.join((Object[]) null, charSeparator, 0, 0);
        StringUtils.join((Object[]) null, stringSeparator, 0, 0);

        if (array != null) {
            int len = array.length;
            int safeStart = data.consumeInt(0, len);
            int safeEnd = data.consumeInt(safeStart, len);
            StringUtils.join(array, charSeparator, safeStart, safeEnd);
            StringUtils.join(array, stringSeparator, safeStart, safeEnd);

            int[] candidates = new int[] {
                -1,
                0,
                1,
                len - 1,
                len,
                len + 1,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE
            };

            int startIndex;
            int endIndex;
            if (data.consumeBoolean()) {
                startIndex = data.consumeInt();
                endIndex = data.consumeInt();
            } else {
                startIndex = candidates[data.consumeInt(0, candidates.length - 1)];
                endIndex = candidates[data.consumeInt(0, candidates.length - 1)];
            }

            if (data.consumeBoolean()) {
                StringUtils.join(array, charSeparator, startIndex, endIndex);
                StringUtils.join(array, stringSeparator, startIndex, endIndex);
            } else {
                StringUtils.join(array, stringSeparator, startIndex, endIndex);
                StringUtils.join(array, charSeparator, startIndex, endIndex);
            }
        }
    }
}