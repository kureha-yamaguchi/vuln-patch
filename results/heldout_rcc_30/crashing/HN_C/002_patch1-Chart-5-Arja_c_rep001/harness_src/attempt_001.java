package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key1 = data.consumeString(20);
        XYSeries series = new XYSeries(key1, data.consumeBoolean(), data.consumeBoolean());
        series.setMaximumItemCount(data.consumeInt(0, 8));

        int ops = data.consumeInt(0, 16);
        for (int i = 0; i < ops; i++) {
            Number x;
            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                x = series.getX(data.consumeInt(0, series.getItemCount() - 1));
            } else {
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        x = Long.valueOf((long) data.consumeInt());
                        break;
                    case 2:
                        x = Double.valueOf((double) data.consumeInt());
                        break;
                    case 3:
                        x = Float.valueOf((float) data.consumeInt());
                        break;
                    case 4:
                        x = Short.valueOf((short) data.consumeInt());
                        break;
                    case 5:
                        x = Byte.valueOf(data.consumeByte());
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
                        x = Integer.valueOf(0);
                        break;
                }
            }

            Number y;
            switch (data.consumeInt(0, 10)) {
                case 0:
                    y = null;
                    break;
                case 1:
                    y = Integer.valueOf(data.consumeInt());
                    break;
                case 2:
                    y = Long.valueOf((long) data.consumeInt());
                    break;
                case 3:
                    y = Double.valueOf((double) data.consumeInt());
                    break;
                case 4:
                    y = Float.valueOf((float) data.consumeInt());
                    break;
                case 5:
                    y = Short.valueOf((short) data.consumeInt());
                    break;
                case 6:
                    y = Byte.valueOf(data.consumeByte());
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
                default:
                    y = Integer.valueOf(0);
                    break;
            }

            series.addOrUpdate(x, y);

            if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 8));
            }
        }

        String key2 = data.consumeString(20);
        XYSeries bugSeries = new XYSeries(key2, true, true);
        bugSeries.setMaximumItemCount(data.consumeInt(1, 8));

        Number bugX;
        switch (data.consumeInt(0, 9)) {
            case 0:
                bugX = Integer.valueOf(data.consumeInt());
                break;
            case 1:
                bugX = Long.valueOf((long) data.consumeInt());
                break;
            case 2:
                bugX = Double.valueOf((double) data.consumeInt());
                break;
            case 3:
                bugX = Float.valueOf((float) data.consumeInt());
                break;
            case 4:
                bugX = Short.valueOf((short) data.consumeInt());
                break;
            case 5:
                bugX = Byte.valueOf(data.consumeByte());
                break;
            case 6:
                bugX = Double.valueOf(Double.NaN);
                break;
            case 7:
                bugX = Double.valueOf(Double.POSITIVE_INFINITY);
                break;
            case 8:
                bugX = Double.valueOf(Double.NEGATIVE_INFINITY);
                break;
            default:
                bugX = Integer.valueOf(0);
                break;
        }

        Number bugY1;
        switch (data.consumeInt(0, 10)) {
            case 0:
                bugY1 = null;
                break;
            case 1:
                bugY1 = Integer.valueOf(data.consumeInt());
                break;
            case 2:
                bugY1 = Long.valueOf((long) data.consumeInt());
                break;
            case 3:
                bugY1 = Double.valueOf((double) data.consumeInt());
                break;
            case 4:
                bugY1 = Float.valueOf((float) data.consumeInt());
                break;
            case 5:
                bugY1 = Short.valueOf((short) data.consumeInt());
                break;
            case 6:
                bugY1 = Byte.valueOf(data.consumeByte());
                break;
            case 7:
                bugY1 = Double.valueOf(Double.NaN);
                break;
            case 8:
                bugY1 = Double.valueOf(Double.POSITIVE_INFINITY);
                break;
            case 9:
                bugY1 = Double.valueOf(Double.NEGATIVE_INFINITY);
                break;
            default:
                bugY1 = Integer.valueOf(0);
                break;
        }

        Number bugY2;
        switch (data.consumeInt(0, 10)) {
            case 0:
                bugY2 = null;
                break;
            case 1:
                bugY2 = Integer.valueOf(data.consumeInt());
                break;
            case 2:
                bugY2 = Long.valueOf((long) data.consumeInt());
                break;
            case 3:
                bugY2 = Double.valueOf((double) data.consumeInt());
                break;
            case 4:
                bugY2 = Float.valueOf((float) data.consumeInt());
                break;
            case 5:
                bugY2 = Short.valueOf((short) data.consumeInt());
                break;
            case 6:
                bugY2 = Byte.valueOf(data.consumeByte());
                break;
            case 7:
                bugY2 = Double.valueOf(Double.NaN);
                break;
            case 8:
                bugY2 = Double.valueOf(Double.POSITIVE_INFINITY);
                break;
            case 9:
                bugY2 = Double.valueOf(Double.NEGATIVE_INFINITY);
                break;
            default:
                bugY2 = Integer.valueOf(0);
                break;
        }

        bugSeries.addOrUpdate(bugX, bugY1);

        if (data.consumeBoolean()) {
            bugSeries.addOrUpdate(bugX, bugY2);
        } else {
            Number otherX;
            switch (data.consumeInt(0, 9)) {
                case 0:
                    otherX = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    otherX = Long.valueOf((long) data.consumeInt());
                    break;
                case 2:
                    otherX = Double.valueOf((double) data.consumeInt());
                    break;
                case 3:
                    otherX = Float.valueOf((float) data.consumeInt());
                    break;
                case 4:
                    otherX = Short.valueOf((short) data.consumeInt());
                    break;
                case 5:
                    otherX = Byte.valueOf(data.consumeByte());
                    break;
                case 6:
                    otherX = Double.valueOf(Double.NaN);
                    break;
                case 7:
                    otherX = Double.valueOf(Double.POSITIVE_INFINITY);
                    break;
                case 8:
                    otherX = Double.valueOf(Double.NEGATIVE_INFINITY);
                    break;
                default:
                    otherX = Integer.valueOf(1);
                    break;
                }
            bugSeries.addOrUpdate(otherX, bugY2);
        }
    }
}