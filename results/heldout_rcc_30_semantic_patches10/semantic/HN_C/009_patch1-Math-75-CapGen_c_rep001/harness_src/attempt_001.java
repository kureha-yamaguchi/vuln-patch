package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency = new Frequency();

        int type = data.consumeInt(0, 4);
        int count = data.consumeInt(0, 32);
        Comparable[] values = new Comparable[count];

        for (int i = 0; i < count; i++) {
            Comparable v;
            switch (type) {
                case 0:
                    v = Long.valueOf(data.consumeInt());
                    break;
                case 1:
                    v = Integer.valueOf(data.consumeInt());
                    break;
                case 2:
                    v = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
                case 3:
                    v = data.consumeString(32);
                    break;
                default:
                    v = Boolean.valueOf(data.consumeBoolean());
                    break;
            }
            values[i] = v;
            frequency.addValue(v);
        }

        Comparable query;
        if (count > 0 && data.consumeBoolean()) {
            query = values[data.consumeInt(0, count - 1)];
        } else {
            switch (type) {
                case 0:
                    query = Long.valueOf(data.consumeInt());
                    break;
                case 1:
                    query = Integer.valueOf(data.consumeInt());
                    break;
                case 2:
                    query = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
                case 3:
                    query = data.consumeString(32);
                    break;
                default:
                    query = Boolean.valueOf(data.consumeBoolean());
                    break;
            }
        }

        double pctViaObject = frequency.getPct((Object) query);
        double pctViaComparable = frequency.getPct(query);

        if (Double.compare(pctViaObject, pctViaComparable) != 0) {
            throw new IllegalStateException(
                "Inconsistent getPct overloads: object=" + pctViaObject
                    + ", comparable=" + pctViaComparable
                    + ", query=" + query
                    + ", count=" + count
                    + ", type=" + type);
        }

        if (data.consumeBoolean()) {
            Object maybeNull = data.consumeBoolean() ? null : query;
            double pct = frequency.getPct(maybeNull);
            double cumPct = frequency.getCumPct((Comparable) maybeNull);
            if (Double.compare(pct, cumPct) != 0 && maybeNull == null) {
                throw new IllegalStateException("Unexpected null overload behavior");
            }
        }
    }
}