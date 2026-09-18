package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;

        if (data.consumeBoolean()) {
            int len = data.consumeInt(0, 16);
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
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = new String(data.consumeBytes(32));
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    default:
                        array[i] = data.consumeBytes(16);
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        switch (data.consumeInt(0, 3)) {
            case 0:
                stringSeparator = null;
                break;
            case 1:
                stringSeparator = "";
                break;
            case 2:
                stringSeparator = data.consumeString(16);
                break;
            default:
                stringSeparator = data.consumeAsciiString(16);
                break;
        }

        int anyStart = data.consumeInt();
        int anyEnd = data.consumeInt();

        StringUtils.join(array, charSeparator, anyStart, anyEnd);
        StringUtils.join(array, stringSeparator, anyStart, anyEnd);

        if (array != null) {
            int len = array.length;

            int boundedStart = data.consumeInt(-2, len + 2);
            int boundedEnd = data.consumeInt(-2, len + 2);
            StringUtils.join(array, charSeparator, boundedStart, boundedEnd);
            StringUtils.join(array, stringSeparator, boundedStart, boundedEnd);

            int orderedStart = data.consumeInt(-2, len + 2);
            int orderedEnd = data.consumeInt(-2, len + 2);
            if (orderedStart > orderedEnd) {
                int t = orderedStart;
                orderedStart = orderedEnd;
                orderedEnd = t;
            }
            StringUtils.join(array, charSeparator, orderedStart, orderedEnd);
            StringUtils.join(array, stringSeparator, orderedStart, orderedEnd);

            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);

            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);

            StringUtils.join(array, charSeparator, len, len);
            StringUtils.join(array, stringSeparator, len, len);

            StringUtils.join(array, charSeparator, -1, len);
            StringUtils.join(array, stringSeparator, -1, len);

            StringUtils.join(array, charSeparator, 0, len + 1);
            StringUtils.join(array, stringSeparator, 0, len + 1);

            if (len > 0) {
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);

                StringUtils.join(array, charSeparator, 1, len);
                StringUtils.join(array, stringSeparator, 1, len);

                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
            }
        } else {
            StringUtils.join((Object[]) null, charSeparator, 0, 1);
            StringUtils.join((Object[]) null, stringSeparator, 0, 1);
        }
    }
}