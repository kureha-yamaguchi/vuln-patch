package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int len = data.consumeInt(0, 32);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
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
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 7:
                        array[i] = new StringBuffer(data.consumeAsciiString(32));
                        break;
                    default:
                        array[i] = data.consumeBytes(16);
                        break;
                }
            }
        }

        char charSep = (char) (data.consumeByte() & 0xff);
        String stringSep;
        if (data.consumeBoolean()) {
            stringSep = null;
        } else if (data.consumeBoolean()) {
            stringSep = data.consumeAsciiString(8);
        } else {
            stringSep = data.consumeString(8);
        }

        int startIndex;
        int endIndex;
        if (data.consumeBoolean()) {
            startIndex = data.consumeInt();
            endIndex = data.consumeInt();
        } else {
            int len = array == null ? 0 : array.length;
            int min = -2;
            int max = len + 2;
            startIndex = data.consumeInt(min, max);
            endIndex = data.consumeInt(min, max);
        }

        StringUtils.join(array, charSep, startIndex, endIndex);
        StringUtils.join(array, stringSep, startIndex, endIndex);

        if (array != null) {
            int len = array.length;

            StringUtils.join(array, charSep, 0, len);
            StringUtils.join(array, stringSep, 0, len);

            StringUtils.join(array, charSep, 0, 0);
            StringUtils.join(array, stringSep, 0, 0);

            StringUtils.join(array, charSep, len, len);
            StringUtils.join(array, stringSep, len, len);

            if (len > 0) {
                StringUtils.join(array, charSep, 0, 1);
                StringUtils.join(array, stringSep, 0, 1);

                StringUtils.join(array, charSep, len - 1, len);
                StringUtils.join(array, stringSep, len - 1, len);
            }
        } else {
            StringUtils.join((Object[]) null, charSep, 0, 0);
            StringUtils.join((Object[]) null, stringSep, 0, 0);
        }
    }
}