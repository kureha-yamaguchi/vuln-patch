package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(chooseMaxItemCount(data));

        java.util.ArrayList<Number> seenX = new java.util.ArrayList<Number>();

        int operations = data.consumeInt(0, 32);
        for (int i = 0; i < operations; i++) {
            if (data.consumeBoolean()) {
                series.setMaximumItemCount(chooseMaxItemCount(data));
            }

            Number x = chooseX(data, seenX);
            Number y = chooseY(data);

            series.addOrUpdate(x, y);

            if (x != null && (allowDuplicateXValues || !containsEquivalent(seenX, x))) {
                seenX.add(x);
            }

            if (data.consumeBoolean() && series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                XYDataItem item = series.getDataItem(idx);
                if (item != null && item.getX() != null) {
                    seenX.add(item.getX());
                }
            }
        }

        if (data.consumeBoolean()) {
            Number finalX = chooseX(data, seenX);
            Number finalY = chooseY(data);
            series.addOrUpdate(finalX, finalY);
        }
    }

    private static int chooseMaxItemCount(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 7);
        switch (choice) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 10;
            case 5:
                return 100;
            case 6:
                return Integer.MAX_VALUE;
            default:
                return data.consumeInt(0, 32);
        }
    }

    private static Number chooseX(FuzzedDataProvider data, java.util.List<Number> seenX) {
        int choice = data.consumeInt(0, 12);
        if (!seenX.isEmpty() && choice == 0) {
            return seenX.get(data.consumeInt(0, seenX.size() - 1));
        }
        switch (choice) {
            case 1:
                return Integer.valueOf(0);
            case 2:
                return Integer.valueOf(1);
            case 3:
                return Integer.valueOf(-1);
            case 4:
                return Integer.valueOf(Integer.MIN_VALUE);
            case 5:
                return Integer.valueOf(Integer.MAX_VALUE);
            case 6:
                return Long.valueOf(0L);
            case 7:
                return Double.valueOf(-0.0d);
            case 8:
                return Double.valueOf(Double.NaN);
            case 9:
                return Double.valueOf(Double.NEGATIVE_INFINITY);
            case 10:
                return Double.valueOf(Double.POSITIVE_INFINITY);
            case 11:
                return Double.valueOf((double) data.consumeInt());
            case 12:
                return Float.valueOf((float) data.consumeInt());
            default:
                return Byte.valueOf(data.consumeByte());
        }
    }

    private static Number chooseY(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 12);
        switch (choice) {
            case 0:
                return null;
            case 1:
                return Integer.valueOf(0);
            case 2:
                return Integer.valueOf(1);
            case 3:
                return Integer.valueOf(-1);
            case 4:
                return Integer.valueOf(Integer.MIN_VALUE);
            case 5:
                return Integer.valueOf(Integer.MAX_VALUE);
            case 6:
                return Long.valueOf(data.consumeInt());
            case 7:
                return Double.valueOf(-0.0d);
            case 8:
                return Double.valueOf(Double.NaN);
            case 9:
                return Double.valueOf(Double.NEGATIVE_INFINITY);
            case 10:
                return Double.valueOf(Double.POSITIVE_INFINITY);
            case 11:
                return Double.valueOf((double) data.consumeInt());
            case 12:
                return Float.valueOf((float) data.consumeInt());
            default:
                return Byte.valueOf(data.consumeByte());
        }
    }

    private static boolean containsEquivalent(java.util.List<Number> values, Number candidate) {
        for (int i = 0; i < values.size(); i++) {
            Number n = values.get(i);
            if (n == candidate) {
                return true;
            }
            if (n != null && candidate != null && n.equals(candidate)) {
                return true;
            }
        }
        return false;
    }
}