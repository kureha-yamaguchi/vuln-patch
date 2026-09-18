package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(4, 24);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        boolean makeMonotonicX = data.consumeBoolean();
        boolean forceDuplicateX = data.consumeBoolean();
        boolean forceConstantX = data.consumeBoolean();
        boolean useSimpleNumericValues = data.consumeBoolean();

        double xBase;
        {
            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            xBase = Double.longBitsToDouble(bits);
            if (useSimpleNumericValues) {
                xBase = data.consumeInt(-16, 16);
            }
        }

        double currentX = xBase;
        for (int i = 0; i < n; i++) {
            double weight;
            double x;
            double y;

            if (useSimpleNumericValues) {
                weight = data.consumeInt(-8, 8);
                y = data.consumeInt(-1024, 1024);
            } else {
                long wBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                weight = Double.longBitsToDouble(wBits);
                y = Double.longBitsToDouble(yBits);
            }

            if (forceConstantX) {
                x = xBase;
            } else if (makeMonotonicX) {
                double step;
                if (useSimpleNumericValues) {
                    step = data.consumeInt(-4, 4);
                } else {
                    long stepBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    step = Double.longBitsToDouble(stepBits);
                }
                currentX += step;
                x = currentX;
            } else {
                if (useSimpleNumericValues) {
                    x = data.consumeInt(-1024, 1024);
                } else {
                    long xBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    x = Double.longBitsToDouble(xBits);
                }
            }

            if (forceDuplicateX && i > 0 && data.consumeBoolean()) {
                x = observations[data.consumeInt(0, i - 1)].getX();
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        if (data.consumeBoolean()) {
            new HarmonicFitter.ParameterGuesser(observations).guess();
        } else {
            HarmonicFitter fitter =
                new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            for (int i = 0; i < observations.length; i++) {
                WeightedObservedPoint p = observations[i];
                fitter.addObservedPoint(p.getWeight(), p.getX(), p.getY());
            }
            fitter.fit();
        }
    }
}