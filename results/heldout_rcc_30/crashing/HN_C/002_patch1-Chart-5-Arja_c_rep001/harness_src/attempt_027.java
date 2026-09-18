package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Comparable key;
        if (data.consumeBoolean()) {
            key = data.consumeAsciiString(32);
        } else {
            key = Integer.valueOf(data.consumeInt());
        }

        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();
        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);

        int maxCountChoice = data.consumeInt(0, 6);
        if (maxCountChoice == 0) {
            series.setMaximumItemCount(0);
        } else if (maxCountChoice == 1) {
            series.setMaximumItemCount(1);
        } else if (maxCountChoice == 2) {
            series.setMaximumItemCount(2);
        } else if (maxCountChoice == 3) {
            series.setMaximumItemCount(3);
        } else if (maxCountChoice == 4) {
            series.setMaximumItemCount(10);
        } else if (maxCountChoice == 5) {
            series.setMaximumItemCount(Math.abs(data.consumeInt()));
        } else {
            series.setMaximumItemCount(Integer.MAX_VALUE);
        }

        int preload = data.consumeInt(0, 16);
        for (int i = 0; i < preload && data.remainingBytes() > 0; i++) {
            int xKind = data.consumeInt(0, 9);
            Number x;
            switch (xKind) {
                case 0:
                    x = Integer.valueOf(0);
                    break;
                case 1:
                    x = Integer.valueOf(1);
                    break;
                case 2:
                    x = Integer.valueOf(-1);
                    break;
                case 3:
                    x = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    x = Double.valueOf(0.0d);
                    break;
                case 5:
                    x = Double.valueOf(-0.0d);
                    break;
                case 6:
                    x = Double.valueOf(Double.NaN);
                    break;
                case 7:
                    x = Double.valueOf(Double.POSITIVE_INFINITY);
                    break;
                case 8:
                    x = Double.valueOf(Double.NEGATIVE_INFINITY);
                    break;
                default:
                    x = Double.valueOf((double) data.consumeInt() / (double) (data.consumeInt(1, Integer.MAX_VALUE)));
                    break;
            }

            int yKind = data.consumeInt(0, 9);
            Number y;
            switch (yKind) {
                case 0:
                    y = null;
                    break;
                case 1:
                    y = Integer.valueOf(0);
                    break;
                case 2:
                    y = Integer.valueOf(1);
                    break;
                case 3:
                    y = Integer.valueOf(-1);
                    break;
                case 4:
                    y = Integer.valueOf(data.consumeInt());
                    break;
                case 5:
                    y = Double.valueOf(0.0d);
                    break;
                case 6:
                    y = Double.valueOf(Double.NaN);
                    break;
                case 7:
                    y = Double.valueOf(Double.POSITIVE_INFINITY);
                    break;
                case 8:
                    y = Double.valueOf(Double.NEGATIVE_INFINITY);
                    break;
                default:
                    y = Double.valueOf((double) data.consumeInt() / (double) (data.consumeInt(1, Integer.MAX_VALUE)));
                    break;
            }

            if (data.consumeBoolean()) {
                series.addOrUpdate(x, y);
            } else {
                series.add(x, y);
            }
        }

        int ops = data.consumeInt(1, 32);
        for (int i = 0; i < ops && data.remainingBytes() > 0; i++) {
            int xKind = data.consumeInt(0, 11);
            Number x;
            switch (xKind) {
                case 0:
                    x = Integer.valueOf(0);
                    break;
                case 1:
                    x = Integer.valueOf(1);
                    break;
                case 2:
                    x = Integer.valueOf(-1);
                    break;
                case 3:
                    x = Integer.valueOf(Integer.MIN_VALUE);
                    break;
                case 4:
                    x = Integer.valueOf(Integer.MAX_VALUE);
                    break;
                case 5:
                    x = Double.valueOf(0.0d);
                    break;
                case 6:
                    x = Double.valueOf(-0.0d);
                    break;
                case 7:
                    x = Double.valueOf(Double.NaN);
                    break;
                case 8:
                    x = Double.valueOf(Double.POSITIVE_INFINITY);
                    break;
                case 9:
                    x = Double.valueOf(Double.NEGATIVE_INFINITY);
                    break;
                case 10:
                    x = Double.valueOf((double) data.consumeInt() / (double) (data.consumeInt(1, Integer.MAX_VALUE)));
                    break;
                default:
                    if (series.getItemCount() > 0 && data.consumeBoolean()) {
                        x = series.getX(data.consumeInt(0, series.getItemCount() - 1));
                    } else {
                        x = Integer.valueOf(data.consumeInt());
                    }
                    break;
            }

            int yKind = data.consumeInt(0, 11);
            Number y;
            switch (yKind) {
                case 0:
                    y = null;
                    break;
                case 1:
                    y = Integer.valueOf(0);
                    break;
                case 2:
                    y = Integer.valueOf(1);
                    break;
                case 3:
                    y = Integer.valueOf(-1);
                    break;
                case 4:
                    y = Integer.valueOf(Integer.MIN_VALUE);
                    break;
                case 5:
                    y = Integer.valueOf(Integer.MAX_VALUE);
                    break;
                case 6:
                    y = Double.valueOf(0.0d);
                    break;
                case 7:
                    y = Double.valueOf(Double.NaN);
                    break;
                case 8:
                    y = Double.valueOf(Double.POSITIVE_INFINITY);
                    break;
                case 9:
                    y = Double.valueOf(Double.NEGATIVE_INFINITY);
                    break;
                case 10:
                    y = Double.valueOf((double) data.consumeInt() / (double) (data.consumeInt(1, Integer.MAX_VALUE)));
                    break;
                default:
                    y = Double.valueOf(data.consumeInt());
                    break;
            }

            XYDataItem old = series.addOrUpdate(x, y);

            if (old != null && data.consumeBoolean()) {
                series.addOrUpdate(old.getX(), old.getY());
            }

            if (data.consumeBoolean() && series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                series.indexOf(series.getX(idx));
            }

            if (data.consumeBoolean()) {
                int newMaxMode = data.consumeInt(0, 4);
                if (newMaxMode == 0) {
                    series.setMaximumItemCount(0);
                } else if (newMaxMode == 1) {
                    series.setMaximumItemCount(1);
                } else if (newMaxMode == 2) {
                    series.setMaximumItemCount(2);
                } else if (newMaxMode == 3) {
                    series.setMaximumItemCount(3);
                } else {
                    series.setMaximumItemCount(Math.abs(data.consumeInt()));
                }
            }
        }
    }
}