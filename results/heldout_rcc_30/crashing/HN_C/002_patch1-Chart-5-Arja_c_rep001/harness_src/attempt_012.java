package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);

        Number[] pool = new Number[16];
        pool[0] = Integer.valueOf(0);
        pool[1] = Integer.valueOf(1);
        pool[2] = Integer.valueOf(-1);
        pool[3] = Integer.valueOf(Integer.MIN_VALUE);
        pool[4] = Integer.valueOf(Integer.MAX_VALUE);
        pool[5] = Byte.valueOf((byte) 0);
        pool[6] = Byte.valueOf(data.consumeByte());
        pool[7] = Short.valueOf((short) data.consumeInt());
        pool[8] = Integer.valueOf(data.consumeInt());
        pool[9] = Long.valueOf((long) data.consumeInt());
        pool[10] = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        pool[11] = Float.valueOf((float) data.consumeInt());
        pool[12] = Double.valueOf((double) data.consumeInt());
        pool[13] = Double.valueOf(-0.0d);
        pool[14] = Double.valueOf(0.0d);
        pool[15] = Float.valueOf(-0.0f);

        XYSeries[] serieses = new XYSeries[] {
            new XYSeries(key, false, false),
            new XYSeries(key + "A", true, false),
            new XYSeries(key + "B", false, true),
            new XYSeries(key + "C", true, true),
            new XYSeries(key + "D", data.consumeBoolean(), data.consumeBoolean())
        };

        for (int s = 0; s < serieses.length; s++) {
            serieses[s].setMaximumItemCount(data.consumeInt(0, 16));
        }

        Number[] seen = new Number[32];
        int seenCount = 0;
        int ops = data.consumeInt(1, 24);

        for (int i = 0; i < ops; i++) {
            Number x;
            int xMode = data.consumeInt(0, 5);
            if (xMode == 0 && seenCount > 0) {
                x = seen[data.consumeInt(0, seenCount - 1)];
            } else if (xMode == 1 && seenCount > 0) {
                Number prior = seen[data.consumeInt(0, seenCount - 1)];
                if (prior == null) {
                    x = pool[data.consumeInt(0, pool.length - 1)];
                } else if (prior instanceof Integer) {
                    x = Double.valueOf(((Integer) prior).doubleValue());
                } else if (prior instanceof Long) {
                    x = Integer.valueOf((int) ((Long) prior).longValue());
                } else if (prior instanceof Byte) {
                    x = Long.valueOf(((Byte) prior).longValue());
                } else if (prior instanceof Short) {
                    x = Float.valueOf(((Short) prior).floatValue());
                } else if (prior instanceof Float) {
                    x = Double.valueOf(((Float) prior).doubleValue());
                } else if (prior instanceof Double) {
                    x = Integer.valueOf((int) ((Double) prior).doubleValue());
                } else {
                    x = pool[data.consumeInt(0, pool.length - 1)];
                }
            } else {
                x = pool[data.consumeInt(0, pool.length - 1)];
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                y = pool[data.consumeInt(0, pool.length - 1)];
            }

            XYSeries target = serieses[data.consumeInt(0, serieses.length - 1)];
            XYDataItem overwritten = target.addOrUpdate(x, y);

            if (seenCount < seen.length) {
                seen[seenCount++] = x;
            } else {
                seen[data.consumeInt(0, seen.length - 1)] = x;
            }

            if (overwritten != null) {
                overwritten.getX();
                overwritten.getY();
            }

            if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                target.setMaximumItemCount(data.consumeInt(0, 16));
            }
        }

        for (int s = 0; s < serieses.length; s++) {
            Number x = pool[data.consumeInt(0, pool.length - 1)];
            Number y = data.consumeBoolean() ? null : pool[data.consumeInt(0, pool.length - 1)];
            XYDataItem overwritten = serieses[s].addOrUpdate(x, y);
            if (overwritten != null) {
                overwritten.getX();
                overwritten.getY();
            }
        }
    }
}