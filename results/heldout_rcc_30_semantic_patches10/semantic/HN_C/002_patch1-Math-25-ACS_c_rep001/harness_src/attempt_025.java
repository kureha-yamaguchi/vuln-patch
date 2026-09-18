package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int len = data.consumeInt(0, 16);
        WeightedObservedPoint[] points = new WeightedObservedPoint[len];

        for (int i = 0; i < len; i++) {
            int mode = data.consumeInt(0, 9);

            double weight;
            double x;
            double y;

            switch (mode) {
                case 0:
                    weight = data.consumeInt();
                    x = data.consumeInt();
                    y = data.consumeInt();
                    break;
                case 1:
                    weight = data.consumeByte();
                    x = data.consumeByte();
                    y = data.consumeByte();
                    break;
                case 2:
                    weight = 1.0;
                    x = i;
                    y = data.consumeInt(-10, 10);
                    break;
                case 3:
                    weight = 1.0;
                    x = -i;
                    y = data.consumeInt(-10, 10);
                    break;
                case 4:
                    weight = 1.0;
                    x = 0.0;
                    y = data.consumeInt();
                    break;
                case 5:
                    weight = data.consumeBoolean() ? 1.0 : -1.0;
                    x = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    y = data.consumeInt();
                    break;
                case 6: {
                    long xb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    long yb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    long wb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    x = Double.longBitsToDouble(xb);
                    y = Double.longBitsToDouble(yb);
                    weight = Double.longBitsToDouble(wb);
                    break;
                }
                case 7:
                    weight = 1.0;
                    x = data.consumeBoolean() ? 0.0 : -0.0;
                    y = data.consumeBoolean() ? 0.0 : -0.0;
                    break;
                case 8:
                    weight = 1.0;
                    x = data.consumeInt(-3, 3);
                    y = data.consumeInt(-3, 3);
                    break;
                default:
                    weight = 1.0;
                    x = data.consumeBoolean() ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                    y = data.consumeBoolean() ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                    break;
            }

            points[i] = new WeightedObservedPoint(weight, x, y);
        }

        if (len > 1 && data.consumeBoolean()) {
            for (int i = 0, j = len - 1; i < j; i++, j--) {
                WeightedObservedPoint tmp = points[i];
                points[i] = points[j];
                points[j] = tmp;
            }
        }

        if (len > 0 && data.consumeBoolean()) {
            int idx = data.consumeInt(0, len - 1);
            points[idx] = new WeightedObservedPoint(points[idx].getWeight(), points[0].getX(), points[idx].getY());
        }

        if (len > 1 && data.consumeBoolean()) {
            for (int i = 1; i < len; i++) {
                if (data.consumeBoolean()) {
                    points[i] = new WeightedObservedPoint(points[i].getWeight(), points[i - 1].getX(), points[i].getY());
                }
            }
        }

        if (len > 0 && data.consumeBoolean()) {
            int idx = data.consumeInt(0, len - 1);
            points[idx] = new WeightedObservedPoint(points[idx].getWeight(), points[idx].getX(), points[0].getY());
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
        guesser.guess();
    }
}