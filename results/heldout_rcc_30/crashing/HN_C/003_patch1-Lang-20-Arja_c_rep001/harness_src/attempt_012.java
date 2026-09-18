package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
            int length = data.consumeInt(0, 16);
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
                        array[i] = Long.valueOf((long) data.consumeInt());
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 7:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 8:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
                        break;
                }
            }
        }

        int len = array == null ? 0 : array.length;

        int safeStart = data.consumeInt(0, len);
        int safeEnd = data.consumeInt(0, len);
        if (data.consumeBoolean() && safeStart > safeEnd) {
            int tmp = safeStart;
            safeStart = safeEnd;
            safeEnd = tmp;
        }

        int boundedStart = data.consumeInt(-2, len + 2);
        int boundedEnd = data.consumeInt(-2, len + 2);

        int rawStart = data.consumeBoolean() ? data.consumeInt() : boundedStart;
        int rawEnd = data.consumeBoolean() ? data.consumeInt() : boundedEnd;

        char charSeparator = (char) (data.consumeByte() & 0xff);

        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeString(16);
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(16);
        } else {
            stringSeparator = data.consumeRemainingAsString();
        }

        StringUtils.join(array, charSeparator, safeStart, safeEnd);
        StringUtils.join(array, stringSeparator, safeStart, safeEnd);

        StringUtils.join(array, charSeparator, boundedStart, boundedEnd);
        StringUtils.join(array, stringSeparator, boundedStart, boundedEnd);

        StringUtils.join(array, charSeparator, rawStart, rawEnd);
        StringUtils.join(array, stringSeparator, rawStart, rawEnd);
    }
}