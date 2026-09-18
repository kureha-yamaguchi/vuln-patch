package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        driveThroughRealHarmonicFitter(data);
        checkBoundaryFlipWithXScalingConsistency(data);
    }

    private static void driveThroughRealHarmonicFitter(FuzzedDataProvider data) {
        double[] y = triangularSeed();
        int idx = data.consumeInt(0, y.length - 1);
        double eps = boundedNonZeroFinite(data);
        y[idx] += eps;

        HarmonicFitter fitter;
        try {
            fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
        } catch (Throwable t) {
            return;
        }

        double start = finiteBounded(data.consumeInt(-1000, 1000));
        double step = positiveBoundedStep(data);
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(1.0, start + step * i, y[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkBoundaryFlipWithXScalingConsistency(FuzzedDataProvider data) {
        double[] baseY = triangularSeed();
        int idx = data.consumeInt(0, baseY.length - 1);
        double eps = boundedNonZeroFinite(data);

        WeightedObservedPoint[] p1 = new WeightedObservedPoint[baseY.length];
        WeightedObservedPoint[] p2 = new WeightedObservedPoint[baseY.length];

        double start1 = finiteBounded(data.consumeInt(-1000, 1000));
        double scale = positiveBoundedStep(data);
        double start2 = finiteBounded(data.consumeInt(-1000, 1000));

        for (int i = 0; i < baseY.length; i++) {
            double yi = baseY[i];
            if (i == idx) {
                yi += eps;
            }
            p1[i] = new WeightedObservedPoint(1.0, start1 + i, yi);
            p2[i] = new WeightedObservedPoint(1.0, start2 + scale * i, yi);
        }

        HarmonicFitter.ParameterGuesser g1;
        HarmonicFitter.ParameterGuesser g2;
        try {
            g1 = new HarmonicFitter.ParameterGuesser(p1);
            g2 = new HarmonicFitter.ParameterGuesser(p2);
        } catch (Throwable t) {
            return;
        }

        double[] guess1;
        double[] guess2;
        try {
            guess1 = g1.guess();
            guess2 = g2.guess();
        } catch (MathIllegalStateException ok) {
            return;
        } catch (Throwable t) {
            return;
        }

        if (guess1 == null || guess2 == null || guess1.length < 3 || guess2.length < 3) {
            return;
        }

        HarmonicOscillator h1 = new HarmonicOscillator(guess1[0], guess1[1], guess1[2]);
        HarmonicOscillator h2 = new HarmonicOscillator(guess2[0], guess2[1], guess2[2]);

        double maxAbs = 0.0;
        double maxDiff = 0.0;
        for (int i = 0; i < baseY.length; i++) {
            double x1 = start1 + i;
            double x2 = start2 + scale * i;
            double v1;
            double v2;
            try {
                v1 = h1.value(x1);
                v2 = h2.value(x2);
            } catch (Throwable t) {
                return;
            }
            if (!Double.isFinite(v1) || !Double.isFinite(v2)) {
                return;
            }
            double abs1 = Math.abs(v1);
            double abs2 = Math.abs(v2);
            if (abs1 > maxAbs) {
                maxAbs = abs1;
            }
            if (abs2 > maxAbs) {
                maxAbs = abs2;
            }
            double diff = Math.abs(v1 - v2);
            if (diff > maxDiff) {
                maxDiff = diff;
            }
        }

        double ampDiff = Math.abs(guess1[0] - guess2[0]);
        double ampScale = Math.max(1.0, Math.max(Math.abs(guess1[0]), Math.abs(guess2[0])));
        double omegaScaled1 = guess1[1] * scale;
        double omegaDiff = Math.abs(omegaScaled1 - guess2[1]);
        double omegaScale = Math.max(1.0, Math.max(Math.abs(omegaScaled1), Math.abs(guess2[1])));

        boolean violation = maxDiff > Math.max(1e-6, 1e-4 * Math.max(1.0, maxAbs))
                || ampDiff > 1e-4 * ampScale
                || omegaDiff > 1e-4 * omegaScale;

        if (violation) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:x-scale-near-c2-zero] metamorphic violation: "
                            + "For the same y-values, changing x from start+i to start+scale*i is a pure reparameterization. "
                            + "The algorithm shown computes omega from c2/c3, which scales inversely with x-scale, while amplitude stays the same; "
                            + "therefore the two guessed oscillators must agree when evaluated at corresponding abscissae. "
                            + "A patch that merely deletes/avoids the c2==0 guard can return inconsistent parameters near that boundary. "
                            + "idx=" + idx
                            + " eps=" + eps
                            + " start1=" + start1
                            + " start2=" + start2
                            + " scale=" + scale
                            + " guess1[a,omega,phi]=" + tripleToString(guess1)
                            + " guess2[a,omega,phi]=" + tripleToString(guess2)
                            + " maxDiff=" + maxDiff
                            + " maxAbs=" + maxAbs
                            + " ampDiff=" + ampDiff
                            + " omegaScaled1=" + omegaScaled1
                            + " omegaDiff=" + omegaDiff);
        }
    }

    private static double[] triangularSeed() {
        return new double[] {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
        };
    }

    private static double boundedNonZeroFinite(FuzzedDataProvider data) {
        double raw = Double.longBitsToDouble(data.consumeInt() ^ (((long) data.consumeInt()) << 32));
        if (!Double.isFinite(raw) || raw == 0.0d) {
            raw = (double) data.consumeInt(-1000, 1000) / 1024.0d;
        }
        if (raw == 0.0d) {
            raw = 1.0d / 1024.0d;
        }
        if (raw > 8.0d) {
            raw = 8.0d;
        } else if (raw < -8.0d) {
            raw = -8.0d;
        }
        return raw;
    }

    private static double finiteBounded(int v) {
        return (double) v;
    }

    private static double positiveBoundedStep(FuzzedDataProvider data) {
        return (double) data.consumeInt(1, 64);
    }

    private static String tripleToString(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}