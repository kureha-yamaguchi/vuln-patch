package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        boolean useNullArray = data.consumeBoolean();

        if (!useNullArray) {
            int length = data.consumeInt(0, 32);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                int kind = data.consumeInt(0, 6);
                switch (kind) {
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
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    default:
                        array[i] = new String(data.consumeBytes(16));
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

        if (array == null) {
            StringUtils.join((Object[]) null, charSeparator, data.consumeInt(), data.consumeInt());
            StringUtils.join((Object[]) null, stringSeparator, data.consumeInt(), data.consumeInt());
            return;
        }

        int len = array.length;

        StringUtils.join(array, charSeparator, 0, len);
        StringUtils.join(array, stringSeparator, 0, len);

        int safeStart1 = data.consumeInt(0, len);
        int safeEnd1 = data.consumeInt(0, len);
        if (safeStart1 > safeEnd1) {
            int t = safeStart1;
            safeStart1 = safeEnd1;
            safeEnd1 = t;
        }
        StringUtils.join(array, charSeparator, safeStart1, safeEnd1);
        StringUtils.join(array, stringSeparator, safeStart1, safeEnd1);

        StringUtils.join(array, charSeparator, len, len);
        StringUtils.join(array, stringSeparator, len, len);

        if (len > 0) {
            int single = data.consumeInt(0, len - 1);
            StringUtils.join(array, charSeparator, single, single + 1);
            StringUtils.join(array, stringSeparator, single, single + 1);
        }

        int startIndex;
        if (data.consumeBoolean()) {
            startIndex = data.consumeInt();
        } else {
            startIndex = data.consumeInt(-4, len + 4);
        }

        int endIndex;
        if (data.consumeBoolean()) {
            endIndex = data.consumeInt();
        } else {
            endIndex = data.consumeInt(-4, len + 4);
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