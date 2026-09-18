package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double w = data.consumeInt(-1000, 1000) / 10.0;
        double x = data.consumeInt(-1000, 1000) / 10.0;
        double y = data.consumeInt(-1000, 1000) / 10.0;
        WeightedObservedPoint p;
        try {
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                            + "expected=(" + w + "," + x + "," + y + ")"
                            + " actual=(" + p.getWeight() + "," + p.getX() + "," + p.getY() + ")");
        }

        {
            final double[] exactY = { 0, 1, 2, 3, 2, 1,
                                      0, -1, -2, -3, -2, -1,
                                      0, 1, 2, 3, 2, 1,
                                      0, -1, -2, -3, -2, -1,
                                      0, 1, 2, 3, 2, 1, 0 };
            final int len = exactY.length;
            final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
            for (int i = 0; i < len; i++) {
                points[i] = new WeightedObservedPoint(1.0, i, exactY[i]);
            }

            HarmonicFitter.ParameterGuesser guesser;
            try {
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            boolean violated = false;
            String detail = "completed normally";
            try {
                guesser.guess();
                violated = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:math844-exact] semantic mismatch: expected MathIllegalStateException but " + detail);
            }
        }

        int scale = data.consumeInt(1, 5);
        int step = data.consumeInt(1, 4);
        int start = data.consumeInt(-20, 20);
        int repetitions = data.consumeInt(2, 4);
        int[] period = new int[] { 0, 1, 2, 3, 2, 1, 0, -1, -2, -3, -2, -1 };
        WeightedObservedPoint[] triPoints;
        HarmonicFitter.ParameterGuesser triGuesser;
        Throwable triangularWrong = null;
        boolean triangularCompletedNormally = false;
        try {
            int len = repetitions * period.length + 1;
            triPoints = new WeightedObservedPoint[len];
            for (int i = 0; i < len - 1; i++) {
                triPoints[i] = new WeightedObservedPoint(1.0, start + (double) i * step, period[i % period.length] * (double) scale);
            }
            triPoints[len - 1] = new WeightedObservedPoint(1.0, start + (double) (len - 1) * step, 0.0);
            triGuesser = new HarmonicFitter.ParameterGuesser(triPoints);
            try {
                triGuesser.guess();
                triangularCompletedNormally = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                triangularWrong = t;
            }
        } catch (Throwable t) {
            return;
        }
        if (triangularWrong != null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException on valid triangular sample",
                    triangularWrong);
        }
        if (triangularCompletedNormally) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException but completed normally"
                            + " scale=" + scale + " step=" + step + " start=" + start + " repetitions=" + repetitions);
        }

        {
            double amp = data.consumeInt(1, 5);
            double omega = data.consumeInt(1, 20) / 10.0;
            double phi = data.consumeInt(-30, 30) / 10.0;
            double x0 = data.consumeInt(-20, 20) / 10.0;
            double dx = data.consumeInt(1, 10) / 10.0;
            int n = data.consumeInt(8, 20);

            WeightedObservedPoint[] harmonicPoints = new WeightedObservedPoint[n];
            org.apache.commons.math3.analysis.function.HarmonicOscillator oscillator =
                    new org.apache.commons.math3.analysis.function.HarmonicOscillator(amp, omega, phi);
            for (int i = 0; i < n; i++) {
                double xi = x0 + i * dx;
                harmonicPoints[i] = new WeightedObservedPoint(1.0, xi, oscillator.value(xi));
            }

            double[] g1;
            double[] g2;
            try {
                HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(harmonicPoints);
                g1 = guesser.guess();
                g2 = guesser.guess();
            } catch (Throwable t) {
                return;
            }

            boolean mismatch = g1.length != g2.length;
            if (!mismatch) {
                for (int i = 0; i < g1.length; i++) {
                    if (Double.doubleToLongBits(g1[i]) != Double.doubleToLongBits(g2[i])) {
                        mismatch = true;
                        break;
                    }
                }
            }
            if (mismatch) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:guess-idempotence] metamorphic violation: repeated guess() on the same immutable observations must agree"
                                + " first=[" + g1[0] + "," + g1[1] + "," + g1[2] + "]"
                                + " second=[" + g2[0] + "," + g2[1] + "," + g2[2] + "]");
            }

            double[] fitNoArg;
            double[] fitWithGuess;
            try {
                HarmonicFitter f1 = new HarmonicFitter(
                        new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
                HarmonicFitter f2 = new HarmonicFitter(
                        new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
                for (int i = 0; i < harmonicPoints.length; i++) {
                    WeightedObservedPoint pt = harmonicPoints[i];
                    f1.addObservedPoint(pt.getWeight(), pt.getX(), pt.getY());
                    f2.addObservedPoint(pt.getWeight(), pt.getX(), pt.getY());
                }

                double[] initialGuess = new HarmonicFitter.ParameterGuesser(harmonicPoints).guess();
                fitNoArg = f1.fit();
                fitWithGuess = f2.fit(initialGuess);
            } catch (Throwable t) {
                return;
            }

            boolean fitMismatch = fitNoArg.length != fitWithGuess.length;
            if (!fitMismatch) {
                for (int i = 0; i < fitNoArg.length; i++) {
                    double a = fitNoArg[i];
                    double b = fitWithGuess[i];
                    double diff = Math.abs(a - b);
                    double tol = 1.0e-6 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                    if (!(diff <= tol || (Double.isNaN(a) && Double.isNaN(b)))) {
                        fitMismatch = true;
                        break;
                    }
                }
            }
            if (fitMismatch) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:fit-overloads-agree] metamorphic violation: fit() and fit(initialGuess) document the same fitting job and must agree when initialGuess is exactly the one fit() derives"
                                + " fit()=[" + fitNoArg[0] + "," + fitNoArg[1] + "," + fitNoArg[2] + "]"
                                + " fit(initialGuess)=[" + fitWithGuess[0] + "," + fitWithGuess[1] + "," + fitWithGuess[2] + "]");
            }
        }
    }
}