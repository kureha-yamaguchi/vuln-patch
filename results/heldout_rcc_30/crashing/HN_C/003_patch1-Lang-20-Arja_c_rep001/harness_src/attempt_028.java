package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
            int len = data.consumeInt(0, 32);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                if (data.consumeBoolean()) {
                    array[i] = null;
                    continue;
                }
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        array[i] = data.consumeString(32);
                        break;
                    case 1:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 2:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 3:
                        array[i] = Long.valueOf((long) data.consumeInt());
                        break;
                    case 4:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 6:
                        array[i] = data.consumeBytes(16);
                        break;
                    case 7:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeString(16);
        } else {
            stringSeparator = data.consumeAsciiString(16);
        }

        StringUtils.join((Object[]) null, charSeparator, data.consumeInt(), data.consumeInt());
        StringUtils.join((Object[]) null, stringSeparator, data.consumeInt(), data.consumeInt());

        if (array == null) {
            return;
        }

        int len = array.length;

        int safeStart = data.consumeInt(0, len);
        int safeEnd = data.consumeInt(0, len);
        if (data.consumeBoolean() && safeStart > safeEnd) {
            int tmp = safeStart;
            safeStart = safeEnd;
            safeEnd = tmp;
        }

        StringUtils.join(array, charSeparator, 0, len);
        StringUtils.join(array, stringSeparator, 0, len);
        StringUtils.join(array, charSeparator, len, len);
        StringUtils.join(array, stringSeparator, len, len);
        StringUtils.join(array, charSeparator, 0, 0);
        StringUtils.join(array, stringSeparator, 0, 0);
        StringUtils.join(array, charSeparator, safeStart, safeEnd);
        StringUtils.join(array, stringSeparator, safeStart, safeEnd);

        int startIndex;
        int endIndex;
        if (data.consumeBoolean()) {
            startIndex = data.consumeInt();
            endIndex = data.consumeInt();
        } else {
            int span = len + 4;
            startIndex = data.consumeInt(-span, span);
            endIndex = data.consumeInt(-span, span);
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