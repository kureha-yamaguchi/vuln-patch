package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import java.util.Comparator;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        long baseBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        double currentX = Double.longBitsToDouble(baseBits);

        boolean incrementalX = data.consumeBoolean();
        boolean allowDuplicateX = data.consumeBoolean();
        boolean useRawBitDoublesForY = data.consumeBoolean();

        for (int i = 0; i < n; i++) {
            double x;
            if (incrementalX) {
                int stepKind = data.consumeInt(0, 5);
                double dx;
                if (stepKind == 0) {
                    dx = 0.0;
                } else if (stepKind == 1) {
                    dx = data.consumeByte();
                } else if (stepKind == 2) {
                    dx = data.consumeInt(-3, 3);
                } else if (stepKind == 3) {
                    long dxBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    dx = Double.longBitsToDouble(dxBits);
                } else if (stepKind == 4) {
                    dx = (double) data.consumeInt() / (data.consumeBoolean() ? 1.0 : 1024.0);
                } else {
                    dx = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                }

                if (allowDuplicateX && data.consumeBoolean() && i > 0) {
                    x = observations[i - 1].getX();
                } else {
                    currentX += dx;
                    x = currentX;
                }
            } else {
                long xBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                x = Double.longBitsToDouble(xBits);
                if (allowDuplicateX && data.consumeBoolean() && i > 0) {
                    x = observations[i - 1].getX();
                }
            }

            double y;
            if (useRawBitDoublesForY) {
                long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                y = Double.longBitsToDouble(yBits);
            } else {
                int yMode = data.consumeInt(0, 5);
                if (yMode == 0) {
                    y = data.consumeInt(-10, 10);
                } else if (yMode == 1) {
                    y = data.consumeByte();
                } else if (yMode == 2) {
                    y = (double) data.consumeInt() / 1024.0;
                } else if (yMode == 3) {
                    y = data.consumeBoolean() ? Double.NaN : Double.POSITIVE_INFINITY;
                } else if (yMode == 4) {
                    y = data.consumeBoolean() ? Double.NEGATIVE_INFINITY : -0.0d;
                } else {
                    long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    y = Double.longBitsToDouble(yBits);
                }
            }

            double weight;
            int wMode = data.consumeInt(0, 6);
            if (wMode == 0) {
                weight = 1.0;
            } else if (wMode == 1) {
                weight = 0.0;
            } else if (wMode == 2) {
                weight = -1.0;
            } else if (wMode == 3) {
                weight = data.consumeInt(-5, 5);
            } else if (wMode == 4) {
                weight = data.consumeBoolean() ? Double.NaN : Double.POSITIVE_INFINITY;
            } else if (wMode == 5) {
                weight = data.consumeBoolean() ? Double.NEGATIVE_INFINITY : -0.0d;
            } else {
                long wBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                weight = Double.longBitsToDouble(wBits);
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        int arrangement = data.consumeInt(0, 4);
        if (arrangement == 0) {
            Arrays.sort(observations, Comparator.comparingDouble(WeightedObservedPoint::getX));
        } else if (arrangement == 1) {
            Arrays.sort(observations, Comparator.comparingDouble(WeightedObservedPoint::getX).reversed());
        } else if (arrangement == 2 && observations.length > 1) {
            for (int i = 0, j = observations.length - 1; i < j; i++, j--) {
                WeightedObservedPoint tmp = observations[i];
                observations[i] = observations[j];
                observations[j] = tmp;
            }
        } else if (arrangement == 3 && observations.length > 2) {
            int swaps = data.consumeInt(1, observations.length);
            for (int k = 0; k < swaps; k++) {
                int i = data.consumeInt(0, observations.length - 1);
                int j = data.consumeInt(0, observations.length - 1);
                WeightedObservedPoint tmp = observations[i];
                observations[i] = observations[j];
                observations[j] = tmp;
            }
        }

        if (observations.length > 0 && data.consumeBoolean()) {
            int idx = data.consumeInt(0, observations.length - 1);
            double forcedX;
            int mode = data.consumeInt(0, 4);
            if (mode == 0) {
                forcedX = observations[0].getX();
            } else if (mode == 1) {
                forcedX = observations[observations.length - 1].getX();
            } else if (mode == 2) {
                forcedX = 0.0;
            } else if (mode == 3) {
                forcedX = Double.NaN;
            } else {
                forcedX = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            }
            observations[idx] = new WeightedObservedPoint(observations[idx].getWeight(), forcedX, observations[idx].getY());
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        double[] guess = guesser.guess();

        if (guess != null && guess.length == 3 && data.consumeBoolean()) {
            WeightedObservedPoint[] copy = observations.clone();
            Arrays.sort(copy, Comparator.comparingDouble(WeightedObservedPoint::getX));
            new HarmonicFitter.ParameterGuesser(copy).guess();
        }
    }
}