package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int maxPoints = Math.min(32, Math.max(0, data.remainingBytes() / 2 + 1));
        int pointCount = data.consumeInt(0, maxPoints);

        boolean structured = data.consumeBoolean();

        if (structured) {
            long normBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long meanBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long sigmaBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

            double norm = Double.longBitsToDouble(normBits);
            double mean = Double.longBitsToDouble(meanBits);
            double sigma = Double.longBitsToDouble(sigmaBits);

            if (!Double.isFinite(norm)) {
                norm = data.consumeInt() / 16.0;
            }
            if (!Double.isFinite(mean)) {
                mean = data.consumeInt() / 16.0;
            }
            if (!Double.isFinite(sigma) || sigma == 0.0) {
                sigma = (data.consumeInt() / 1024.0);
                if (sigma == 0.0) {
                    sigma = data.consumeBoolean() ? 1.0 : -1.0;
                }
            }

            double absSigma = Math.abs(sigma);
            for (int i = 0; i < pointCount; i++) {
                double x;
                if (pointCount <= 1) {
                    x = mean;
                } else {
                    double t = -3.0 + (6.0 * i) / (pointCount - 1.0);
                    if (data.consumeBoolean()) {
                        t += data.consumeByte() / 16.0;
                    }
                    x = mean + absSigma * t;
                }

                double dx = x - mean;
                double y = norm * Math.exp(-(dx * dx) / (2.0 * absSigma * absSigma));

                if (data.consumeBoolean()) {
                    y += data.consumeByte();
                }

                if (data.consumeBoolean()) {
                    double weight;
                    if (data.consumeBoolean()) {
                        weight = data.consumeByte();
                    } else {
                        long weightBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        weight = Double.longBitsToDouble(weightBits);
                    }
                    fitter.addObservedPoint(weight, x, y);
                } else {
                    fitter.addObservedPoint(x, y);
                }
            }
        } else {
            for (int i = 0; i < pointCount; i++) {
                long xBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long wBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

                double x;
                double y;
                double w;

                switch (data.consumeInt(0, 4)) {
                    case 0:
                        x = Double.longBitsToDouble(xBits);
                        y = Double.longBitsToDouble(yBits);
                        w = Double.longBitsToDouble(wBits);
                        break;
                    case 1:
                        x = data.consumeInt() / 1024.0;
                        y = data.consumeInt() / 1024.0;
                        w = data.consumeInt() / 1024.0;
                        break;
                    case 2:
                        x = data.consumeByte();
                        y = data.consumeByte();
                        w = data.consumeByte();
                        break;
                    case 3:
                        x = data.consumeBoolean() ? 0.0 : -0.0;
                        y = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        w = data.consumeBoolean() ? 1.0 : 0.0;
                        break;
                    default:
                        x = data.consumeInt();
                        y = data.consumeInt();
                        w = data.consumeBoolean() ? -1.0 : 1.0;
                        break;
                }

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(w, x, y);
                } else {
                    fitter.addObservedPoint(x, y);
                }
            }
        }

        fitter.fit();
    }
}