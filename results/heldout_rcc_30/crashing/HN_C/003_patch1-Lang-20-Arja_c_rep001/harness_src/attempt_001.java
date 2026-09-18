package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
            int len = data.consumeInt(0, 32);
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
                        array[i] = Boolean.valueOf(data.consumeBoolean());
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
                stringSeparator = data.consumeAsciiString(16);
                break;
            default:
                stringSeparator = data.consumeString(16);
                break;
        }

        int startIndexA;
        int endIndexA;
        int startIndexB;
        int endIndexB;

        if (array == null) {
            startIndexA = data.consumeInt();
            endIndexA = data.consumeInt();
            startIndexB = data.consumeInt();
            endIndexB = data.consumeInt();
        } else {
            int len = array.length;
            if (data.consumeBoolean()) {
                startIndexA = data.consumeInt(-len - 2, len + 2);
                endIndexA = data.consumeInt(-len - 2, len + 2);
            } else {
                startIndexA = data.consumeInt();
                endIndexA = data.consumeInt();
            }
            if (data.consumeBoolean()) {
                startIndexB = data.consumeInt(-len - 2, len + 2);
                endIndexB = data.consumeInt(-len - 2, len + 2);
            } else {
                startIndexB = data.consumeInt();
                endIndexB = data.consumeInt();
            }
        }

        StringUtils.join(array, charSeparator, startIndexA, endIndexA);
        StringUtils.join(array, stringSeparator, startIndexB, endIndexB);

        if (array != null) {
            StringUtils.join(array, charSeparator, 0, array.length);
            StringUtils.join(array, stringSeparator, 0, array.length);

            int len = array.length;
            if (len == 0) {
                StringUtils.join(array, charSeparator, 0, 0);
                StringUtils.join(array, stringSeparator, 0, 0);
            } else {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
                StringUtils.join(array, charSeparator, len, len);
                StringUtils.join(array, stringSeparator, len, len);
                StringUtils.join(array, charSeparator, 1, 0);
                StringUtils.join(array, stringSeparator, 1, 0);
            }
        } else {
            StringUtils.join((Object[]) null, charSeparator, 0, 0);
            StringUtils.join((Object[]) null, stringSeparator, 0, 0);
        }
    }
}