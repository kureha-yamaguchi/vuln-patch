package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static Number pickNumber(FuzzedDataProvider data, boolean allowNull) {
        int choice = data.consumeInt(0, allowNull ? 15 : 14);
        switch (choice) {
            case 0:
                return allowNull ? null : Integer.valueOf(0);
            case 1:
                return Integer.valueOf(data.consumeInt());
            case 2:
                return Long.valueOf((long) data.consumeInt());
            case 3:
                return Byte.valueOf(data.consumeByte());
            case 4:
                return Short.valueOf((short) data.consumeInt());
            case 5:
                return Float.valueOf((float) data.consumeInt());
            case 6:
                return Double.valueOf((double) data.consumeInt());
            case 7:
                return Double.valueOf(Double.NaN);
            case 8:
                return Double.valueOf(Double.POSITIVE_INFINITY);
            case 9:
                return Double.valueOf(Double.NEGATIVE_INFINITY);
            case 10:
                return Double.valueOf(-0.0d);
            case 11:
                return Double.valueOf(0.0d);
            case 12:
                return Integer.valueOf(Integer.MIN_VALUE);
            case 13:
                return Integer.valueOf(Integer.MAX_VALUE);
            case 14:
                return Long.valueOf(Long.MIN_VALUE);
            case 15:
                return null;
            default:
                return Integer.valueOf(0);
        }
    }

    private static XYSeries makeSeries(FuzzedDataProvider data) {
        String key = data.consumeString(32);
        if (key == null) {
            key = "";
        }
        return new XYSeries(key, data.consumeBoolean(), data.consumeBoolean());
    }

    private static void exerciseSeries(FuzzedDataProvider data, XYSeries series) {
        if (data.consumeBoolean()) {
            series.setMaximumItemCount(data.consumeInt());
        }

        int prepopulate = data.consumeInt(0, 12);
        for (int i = 0; i < prepopulate; i++) {
            Number x = pickNumber(data, false);
            Number y = pickNumber(data, true);
            try {
                series.add(x, y);
            } catch (RuntimeException e) {
            }
        }

        int ops = data.consumeInt(1, 16);
        for (int i = 0; i < ops; i++) {
            Number x;
            if (i == 0 || data.consumeBoolean()) {
                x = pickNumber(data, true);
            } else if (series.getItemCount() > 0 && data.consumeBoolean()) {
                x = series.getX(data.consumeInt(0, series.getItemCount() - 1));
            } else {
                x = pickNumber(data, false);
            }

            Number y = pickNumber(data, true);
            series.addOrUpdate(x, y);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        XYSeries s1 = makeSeries(data);
        XYSeries s2 = makeSeries(data);
        XYSeries s3 = new XYSeries("", true, false);
        XYSeries s4 = new XYSeries("", false, false);
        XYSeries s5 = new XYSeries("", true, true);
        XYSeries s6 = new XYSeries("", false, true);

        exerciseSeries(data, s1);
        exerciseSeries(data, s2);
        exerciseSeries(data, s3);
        exerciseSeries(data, s4);
        exerciseSeries(data, s5);
        exerciseSeries(data, s6);

        XYSeries boundary = new XYSeries("boundary", true, false);
        if (data.consumeBoolean()) {
            boundary.setMaximumItemCount(data.consumeInt());
        }
        boundary.addOrUpdate(Integer.valueOf(0), null);
        boundary.addOrUpdate(Double.valueOf(-0.0d), Integer.valueOf(1));
        boundary.addOrUpdate(Double.valueOf(0.0d), Integer.valueOf(2));
        boundary.addOrUpdate(Double.valueOf(Double.NaN), Integer.valueOf(3));
        boundary.addOrUpdate(Double.valueOf(Double.POSITIVE_INFINITY), Integer.valueOf(4));
        boundary.addOrUpdate(Double.valueOf(Double.NEGATIVE_INFINITY), Integer.valueOf(5));

        if (data.remainingBytes() > 0) {
            boundary.addOrUpdate(pickNumber(data, true), pickNumber(data, true));
        }
    }
}