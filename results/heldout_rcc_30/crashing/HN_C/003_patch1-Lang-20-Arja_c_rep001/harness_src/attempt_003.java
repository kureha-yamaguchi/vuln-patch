package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
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
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
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
                stringSeparator = "";
                break;
            case 2:
                stringSeparator = data.consumeAsciiString(16);
                break;
            default:
                stringSeparator = data.consumeString(16);
                break;
        }

        int start1 = data.consumeInt();
        int end1 = data.consumeInt();
        StringUtils.join(array, charSeparator, start1, end1);

        int start2 = data.consumeInt();
        int end2 = data.consumeInt();
        StringUtils.join(array, stringSeparator, start2, end2);

        if (array != null) {
            int len = array.length;

            int validStart = len == 0 ? 0 : data.consumeInt(0, len);
            int validEnd = len == 0 ? 0 : data.consumeInt(validStart, len);
            StringUtils.join(array, charSeparator, validStart, validEnd);

            String sep2;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    sep2 = null;
                    break;
                case 1:
                    sep2 = "";
                    break;
                default:
                    sep2 = data.consumeString(8);
                    break;
            }
            StringUtils.join(array, sep2, validStart, validEnd);

            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, len);
                StringUtils.join(array, stringSeparator, 0, len);

                int boundaryStart = data.consumeBoolean() ? -1 : len;
                int boundaryEnd = data.consumeBoolean() ? len + 1 : -1;
                StringUtils.join(array, charSeparator, boundaryStart, boundaryEnd);
                StringUtils.join(array, stringSeparator, boundaryStart, boundaryEnd);
            }
        } else {
            StringUtils.join((Object[]) null, charSeparator, data.consumeInt(), data.consumeInt());
            StringUtils.join((Object[]) null, stringSeparator, data.consumeInt(), data.consumeInt());
        }
    }
}