package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        java.util.function.Supplier<Number> nextNumber = () -> {
            switch (data.consumeInt(0, 7)) {
                case 0:
                    return Integer.valueOf(data.consumeInt());
                case 1:
                    return Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                case 2:
                    return Double.valueOf(Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                case 3:
                    return Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                case 4:
                    return Short.valueOf((short) data.consumeInt());
                case 5:
                    return Byte.valueOf(data.consumeByte());
                case 6:
                    return new java.math.BigDecimal(data.consumeAsciiString(12).isEmpty() ? "0" : data.consumeAsciiString(12));
                default:
                    return new java.math.BigInteger(data.consumeAsciiString(12).isEmpty() ? "0" : data.consumeAsciiString(12), 36);
            }
        };

        java.util.function.Supplier<Number> nextNullableNumber = () -> {
            if (data.consumeBoolean()) {
                return null;
            }
            return nextNumber.get();
        };

        String key = data.consumeString(32);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();
        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 16));

        Number[] seenX = new Number[32];
        int seenCount = 0;

        int initialOps = data.consumeInt(0, 12);
        for (int i = 0; i < initialOps; i++) {
            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                x = nextNumber.get();
            }

            Number y = nextNullableNumber.get();
            XYDataItem overwritten = series.addOrUpdate(x, y);

            if (seenCount < seenX.length) {
                seenX[seenCount++] = x;
            }

            if (overwritten != null && data.consumeBoolean()) {
                overwritten.getX();
                overwritten.getY();
            }
        }

        int targetOps = data.consumeInt(1, 10);
        for (int i = 0; i < targetOps; i++) {
            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                x = nextNumber.get();
                if (seenCount < seenX.length) {
                    seenX[seenCount++] = x;
                }
            }

            Number y = nextNullableNumber.get();
            XYDataItem overwritten = series.addOrUpdate(x, y);

            if (overwritten != null) {
                overwritten.getX();
                overwritten.getY();
            }

            if (seenCount > 0 && data.consumeBoolean()) {
                series.indexOf(seenX[data.consumeInt(0, seenCount - 1)]);
            } else {
                series.indexOf(nextNumber.get());
            }
        }

        if (series.getItemCount() > 0 && data.consumeBoolean()) {
            int idx = data.consumeInt(0, series.getItemCount() - 1);
            series.getDataItem(idx);
            series.getX(idx);
            series.getY(idx);
        }
    }
}