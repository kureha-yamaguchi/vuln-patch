package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency = new Frequency();

        int family = data.consumeInt(0, 6);
        int count = data.consumeInt(0, 32);
        Object[] inserted = new Object[count];

        for (int i = 0; i < count; i++) {
            Comparable value = makeComparableValue(data, family);
            inserted[i] = value;
            frequency.addValue(value);
        }

        if (count > 0 && data.consumeBoolean()) {
            frequency.getPct(inserted[data.consumeInt(0, count - 1)]);
        }

        if (data.consumeBoolean()) {
            frequency.getPct(makeComparableValue(data, family));
        }

        if (data.consumeBoolean()) {
            frequency.getPct(makeComparableValue(data, data.consumeInt(0, 6)));
        }

        Object query;
        switch (data.consumeInt(0, 6)) {
            case 0:
                query = count > 0 ? inserted[data.consumeInt(0, count - 1)] : makeComparableValue(data, family);
                break;
            case 1:
                query = makeComparableValue(data, family);
                break;
            case 2:
                query = makeComparableValue(data, data.consumeInt(0, 6));
                break;
            case 3:
                query = null;
                break;
            case 4:
                query = data.consumeBytes(data.consumeInt(0, 32));
                break;
            case 5:
                query = new Object();
                break;
            default:
                query = data.consumeRemainingAsString();
                break;
        }

        frequency.getPct(query);
    }

    private static Comparable makeComparableValue(FuzzedDataProvider data, int family) {
        switch (family) {
            case 0:
                return Integer.valueOf(data.consumeInt());
            case 1:
                return Long.valueOf((long) data.consumeInt());
            case 2:
                return Character.valueOf((char) (data.consumeByte() & 0xff));
            case 3:
                return data.consumeString(32);
            case 4:
                return Boolean.valueOf(data.consumeBoolean());
            case 5:
                return Byte.valueOf(data.consumeByte());
            default:
                return Short.valueOf((short) data.consumeInt());
        }
    }
}