package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static WeightedObservedPoint[] exactMath844Points() {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }
        return points;
    }

    private static WeightedObservedPoint[] generalizedTriangularPoints(FuzzedDataProvider data) {
        final int[] base = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final int scale = data.consumeInt(1, 5);
        final int step = data.consumeInt(1, 4);
        final int start = data.consumeInt(-20, 20);
        final WeightedObservedPoint[] points = new WeightedObservedPoint[base.length];
        for (int i = 0; i < base.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);
        }
        return points;
    }

    private static WeightedObservedPoint[] exactHarmonicPoints(FuzzedDataProvider data, double[] paramsOut) {
        final double a = data.consumeInt(1, 5);
        final double omega = data.consumeInt(1, 4) / 3.0;
        final double phi = data.consumeInt(-20, 20) / 10.0;
        final int n = data.consumeInt(8, 16);
        final int step = data.consumeInt(1, 3);
        final int start = data.consumeInt(-10, 10);

        paramsOut[0] = a;
        paramsOut[1] = omega;
        paramsOut[2] = phi;

        final org.apache.commons.math3.analysis.function.HarmonicOscillator osc =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(a, omega, phi);
        final WeightedObservedPoint[] points = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            final double x = start + (double) i * step;
            points[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
        }
        return points;
    }

    private static double sse(double[] params, WeightedObservedPoint[] points) {
        final org.apache.commons.math3.analysis.function.HarmonicOscillator osc =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(params[0], params[1], params[2]);
        double sum = 0.0;
        for (WeightedObservedPoint p : points) {
            final double d = osc.value(p.getX()) - p.getY();
            sum += d * d;
        }
        return sum;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            double w;
            double x;
            double y;
            WeightedObservedPoint p;
            try {
                w = data.consumeInt(-1000, 1000) / 10.0;
                x = data.consumeInt(-1000, 1000) / 10.0;
                y = data.consumeInt(-1000, 1000) / 10.0;
                p = new WeightedObservedPoint(w, x, y);
            } catch (Throwable t) {
                p = null;
                w = x = y = 0.0;
            }
            if (p != null) {
                if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
                    Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
                    Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "relation weighted_observed_point_getters_roundtrip_constructor_args violated: " +
                            "weight=" + p.getWeight() + "/" + w +
                            " x=" + p.getX() + "/" + x +
                            " y=" + p.getY() + "/" + y);
                }
            }
        }

        {
            Throwable wrong = null;
            boolean completedNormally = false;
            try {
                final HarmonicFitter.ParameterGuesser guesser =
                        new HarmonicFitter.ParameterGuesser(exactMath844Points());
                guesser.guess();
                completedNormally = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                wrong = t;
            }
            if (completedNormally) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException, but guess() completed normally");
            }
            if (wrong != null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException, but got " +
                        wrong.getClass().getName(), wrong);
            }
        }

        {
            Throwable wrong = null;
            boolean completedNormally = false;
            try {
                final WeightedObservedPoint[] points = generalizedTriangularPoints(data);
                final HarmonicFitter.ParameterGuesser guesser =
                        new HarmonicFitter.ParameterGuesser(points);
                guesser.guess();
                completedNormally = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                wrong = t;
            }
            if (completedNormally) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but completed normally");
            }
            if (wrong != null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but got " +
                        wrong.getClass().getName(), wrong);
            }
        }

        {
            Throwable wrong = null;
            boolean completedNormally = false;
            try {
                final HarmonicFitter fitter =
                        new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
                for (WeightedObservedPoint p : exactMath844Points()) {
                    fitter.addObservedPoint(p);
                }
                // Contract we assert: no-arg fit() computes its starting point via the real HarmonicFitter
                // guesser path. For the trusted MATH-844 triangular sample that path must reject with
                // MathIllegalStateException; a patch that merely deletes the throw would let fit() continue.
                fitter.fit();
                completedNormally = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                wrong = t;
            }
            if (completedNormally) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:fit-propagates-guess-failure] semantic mismatch: expected MathIllegalStateException from no-arg fit(), but it completed normally");
            }
            if (wrong != null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:fit-propagates-guess-failure] semantic mismatch: expected MathIllegalStateException from no-arg fit(), but got " +
                        wrong.getClass().getName(), wrong);
            }
        }

        {
            final double[] knownParams = new double[3];
            final WeightedObservedPoint[] points;
            try {
                points = exactHarmonicPoints(data, knownParams);
            } catch (Throwable t) {
                return;
            }

            double[] fitAuto;
            double[] fitSeeded;
            try {
                final HarmonicFitter fitterAuto =
                        new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
                final HarmonicFitter fitterSeeded =
                        new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
                for (WeightedObservedPoint p : points) {
                    fitterAuto.addObservedPoint(p);
                    fitterSeeded.addObservedPoint(p);
                }

                // Documented sibling-agreement check: fit() and fit(double[] initialGuess) are same-name
                // overloads documented to do the same fitting job on the same observations. On a noiseless
                // harmonic sample constructed from a known answer, both real-library calls should fit the
                // same curve; compare observable residuals rather than raw parameters because harmonic
                // parameters have equivalent representations (phase shifts/sign changes).
                fitAuto = fitterAuto.fit();
                fitSeeded = fitterSeeded.fit(new double[] { knownParams[0], knownParams[1], knownParams[2] });
            } catch (Throwable t) {
                return;
            }

            final double autoSse;
            final double seededSse;
            try {
                autoSse = sse(fitAuto, points);
                seededSse = sse(fitSeeded, points);
            } catch (Throwable t) {
                return;
            }

            if (!(Double.isFinite(autoSse) && Double.isFinite(seededSse))) {
                return;
            }

            final double tolerance = 1.0e-6;
            if (autoSse > tolerance || seededSse > tolerance) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:fit-overloads-agree-on-noiseless-harmonic] metamorphic violation: " +
                        "both overloads should fit the exact harmonic sample with near-zero residuals " +
                        "autoSse=" + autoSse + " seededSse=" + seededSse +
                        " a=" + knownParams[0] + " omega=" + knownParams[1] + " phi=" + knownParams[2] +
                        " n=" + points.length);
            }
        }
    }
}