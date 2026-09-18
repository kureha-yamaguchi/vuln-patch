package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        int len = data.consumeInt(0, 16);
        if (data.consumeBoolean()) {
            array = null;
        } else {
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
                        array[i] = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
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

        char separatorChar = (char) (data.consumeByte() & 0xff);

        String separatorString;
        switch (data.consumeInt(0, 4)) {
            case 0:
                separatorString = null;
                break;
            case 1:
                separatorString = "";
                break;
            case 2:
                separatorString = String.valueOf(separatorChar);
                break;
            case 3:
                separatorString = data.consumeAsciiString(8);
                break;
            default:
                separatorString = data.consumeString(16);
                break;
        }

        if (array == null) {
            StringUtils.join((Object[]) null, separatorChar, data.consumeInt(), data.consumeInt());
            StringUtils.join((Object[]) null, separatorString, data.consumeInt(), data.consumeInt());
            return;
        }

        int n = array.length;

        StringUtils.join(array, separatorChar, 0, n);
        StringUtils.join(array, separatorString, 0, n);

        int safeStart = data.consumeInt(0, n);
        int safeEnd = data.consumeInt(safeStart, n);
        StringUtils.join(array, separatorChar, safeStart, safeEnd);
        StringUtils.join(array, separatorString, safeStart, safeEnd);

        StringUtils.join(array, separatorChar, safeStart, safeStart);
        StringUtils.join(array, separatorString, safeStart, safeStart);

        if (n > 0) {
            int oneStart = data.consumeInt(0, n - 1);
            int oneEnd = Math.min(n, oneStart + 1);
            StringUtils.join(array, separatorChar, oneStart, oneEnd);
            StringUtils.join(array, separatorString, oneStart, oneEnd);
        }

        int startIndex;
        switch (data.consumeInt(0, 9)) {
            case 0:
                startIndex = -1;
                break;
            case 1:
                startIndex = 0;
                break;
            case 2:
                startIndex = 1;
                break;
            case 3:
                startIndex = n - 1;
                break;
            case 4:
                startIndex = n;
                break;
            case 5:
                startIndex = n + 1;
                break;
            case 6:
                startIndex = -n - 1;
                break;
            case 7:
                startIndex = data.consumeInt(-n - 2, n + 2);
                break;
            case 8:
                startIndex = Integer.MIN_VALUE;
                break;
            default:
                startIndex = data.consumeInt();
                break;
        }

        int endIndex;
        switch (data.consumeInt(0, 9)) {
            case 0:
                endIndex = -1;
                break;
            case 1:
                endIndex = 0;
                break;
            case 2:
                endIndex = 1;
                break;
            case 3:
                endIndex = n - 1;
                break;
            case 4:
                endIndex = n;
                break;
            case 5:
                endIndex = n + 1;
                break;
            case 6:
                endIndex = -n - 1;
                break;
            case 7:
                endIndex = data.consumeInt(-n - 2, n + 2);
                break;
            case 8:
                endIndex = Integer.MAX_VALUE;
                break;
            default:
                endIndex = data.consumeInt();
                break;
        }

        if (data.consumeBoolean()) {
            StringUtils.join(array, separatorChar, startIndex, endIndex);
            StringUtils.join(array, separatorString, startIndex, endIndex);
        } else {
            StringUtils.join(array, separatorString, startIndex, endIndex);
            StringUtils.join(array, separatorChar, startIndex, endIndex);
        }
    }
}