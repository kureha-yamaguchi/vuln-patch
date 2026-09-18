package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TWO_PI = 2.0 * Math.PI;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Invariant: WeightedObservedPoint is an immutable value holder whose getters
        // must return the constructor arguments.
        {
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
                throw new FuzzerSecurityIssueLow(
                        "relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                                + "weight=" + p.getWeight() + "/" + w
                                + " x=" + p.getX() + "/" + x
                                + " y=" + p.getY() + "/" + y);
            }
        }

        // Lifted oracle from HarmonicFitterTest.testMath844: this exact sample must
        // throw MathIllegalStateException from ParameterGuesser.guess().
        {
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

            HarmonicFitter.ParameterGuesser guesser;
            try {
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            boolean violated = false;
            String detail = "completed normally";
            Throwable cause = null;
            try {
                guesser.guess();
                violated = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                detail = "wrong exception class " + t.getClass().getName();
                cause = t;
            }
            if (violated) {
                if (cause != null) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but " + detail,
                            cause);
                }
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but " + detail);
            }
        }

        // Generalized trusted oracle: same non-harmonic triangular-wave shape as the
        // failing test, with fuzzed positive scale, start, and step. The test's reason
        // still applies: this sample is far from harmonic, so guess() must reject with
        // MathIllegalStateException rather than silently returning coefficients.
        {
            final int[] base = {
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1, 0
            };
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);
            WeightedObservedPoint[] points = new WeightedObservedPoint[base.length];
            for (int i = 0; i < base.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);
            }

            HarmonicFitter.ParameterGuesser guesser;
            try {
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            boolean violated = false;
            String detail = "completed normally";
            Throwable cause = null;
            try {
                guesser.guess();
                violated = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                detail = "wrong exception class " + t.getClass().getName();
                cause = t;
            }
            if (violated) {
                if (cause != null) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:triangular-wave] semantic mismatch: expected MathIllegalStateException, but " + detail,
                            cause);
                }
                throw new FuzzerSecurityIssueLow(
                        "[oracle:triangular-wave] semantic mismatch: expected MathIllegalStateException, but " + detail);
            }

            // Drive the same state through the real public API entry point: HarmonicFitter.fit()
            // without an explicit initial guess internally uses ParameterGuesser.guess().
            HarmonicFitter fitter;
            try {
                fitter = new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
                int mutations = data.consumeInt(1, 3);
                for (int m = 0; m < mutations; m++) {
                    int idx = data.consumeInt(0, points.length - 1);
                    fitter.addObservedPoint(points[idx]);
                }
                for (WeightedObservedPoint p : points) {
                    fitter.addObservedPoint(p);
                }
            } catch (Throwable t) {
                return;
            }

            violated = false;
            detail = "completed normally";
            cause = null;
            try {
                fitter.fit();
                violated = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                detail = "wrong exception class " + t.getClass().getName();
                cause = t;
            }
            if (violated) {
                if (cause != null) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:harmonic-fitter-fit] semantic mismatch: expected MathIllegalStateException, but " + detail,
                            cause);
                }
                throw new FuzzerSecurityIssueLow(
                        "[oracle:harmonic-fitter-fit] semantic mismatch: expected MathIllegalStateException, but " + detail);
            }
        }

        // Hidden-state / state-coupling check:
        // ParameterGuesser(WeightedObservedPoint[] observations) clones the observations array.
        // Therefore, replacing entries in the caller's array after construction must not change
        // what the later reader guess() reports. This directly checks that the constructor's
        // established state and the guess() reader agree on the same internal observations.
        {
            int n = data.consumeInt(8, 20);
            int period = data.consumeInt(6, 20);
            double omega = TWO_PI / period;
            double amplitude = data.consumeInt(1, 5);
            double phi = data.consumeInt(-30, 30) / 10.0;
            int start = data.consumeInt(-10, 10);
            int step = data.consumeInt(1, 3);
            WeightedObservedPoint[] original = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double x = start + (double) i * step;
                double y = amplitude * Math.cos(omega * x + phi);
                original[i] = new WeightedObservedPoint(1.0, x, y);
            }
            WeightedObservedPoint[] snapshot = original.clone();

            HarmonicFitter.ParameterGuesser constructedBeforeMutation;
            HarmonicFitter.ParameterGuesser expectedFromSnapshot;
            try {
                constructedBeforeMutation = new HarmonicFitter.ParameterGuesser(original);
                expectedFromSnapshot = new HarmonicFitter.ParameterGuesser(snapshot);
            } catch (Throwable t) {
                return;
            }

            int mutations = data.consumeInt(1, Math.max(1, n));
            for (int i = 0; i < mutations; i++) {
                int idx = data.consumeInt(0, n - 1);
                double mx = data.consumeInt(-50, 50);
                double my = data.consumeInt(-50, 50);
                original[idx] = new WeightedObservedPoint(1.0, mx, my);
            }

            double[] lhs;
            double[] rhs;
            try {
                lhs = constructedBeforeMutation.guess();
                rhs = expectedFromSnapshot.guess();
            } catch (Throwable t) {
                return;
            }

            if (!sameTriplet(lhs, rhs, 1e-8, 1e-6)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:constructor-clone] metamorphic violation: guess() changed after caller mutated original observations array "
                                + "lhs=" + formatArray(lhs) + " rhs=" + formatArray(rhs));
            }
        }

        // Metamorphic relation over real library calls:
        // guess() returns [Amplitude, Angular frequency, Phase]. Shifting every x by the
        // same constant changes only the phase of a harmonic function, not its amplitude
        // or angular frequency. A correct implementation of the same estimation procedure
        // must therefore report the same a and omega on these two equivalent inputs.
        {
            int n = data.consumeInt(8, 24);
            int period = data.consumeInt(6, 18);
            double omega = TWO_PI / period;
            double amplitude = data.consumeInt(1, 5);
            double phi = data.consumeInt(-25, 25) / 10.0;
            int start = data.consumeInt(-10, 10);
            int shift = data.consumeInt(-10, 10);
            int step = data.consumeInt(1, 3);

            WeightedObservedPoint[] p1 = new WeightedObservedPoint[n];
            WeightedObservedPoint[] p2 = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double x1 = start + (double) i * step;
                double x2 = x1 + shift;
                double y1 = amplitude * Math.cos(omega * x1 + phi);
                double y2 = amplitude * Math.cos(omega * x2 + (phi - omega * shift));
                p1[i] = new WeightedObservedPoint(1.0, x1, y1);
                p2[i] = new WeightedObservedPoint(1.0, x2, y2);
            }

            double[] g1;
            double[] g2;
            try {
                g1 = new HarmonicFitter.ParameterGuesser(p1).guess();
                g2 = new HarmonicFitter.ParameterGuesser(p2).guess();
            } catch (Throwable t) {
                return;
            }

            if (!finite(g1) || !finite(g2)) {
                return;
            }

            if (!close(g1[0], g2[0], 1e-8, 1e-5) || !close(g1[1], g2[1], 1e-8, 1e-5)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:x-shift-a-omega] metamorphic violation: shifting x changed guessed amplitude/frequency "
                                + "g1=" + formatArray(g1) + " g2=" + formatArray(g2));
            }
        }
    }

    private static boolean finite(double[] v) {
        if (v == null) {
            return false;
        }
        for (double d : v) {
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return false;
            }
        }
        return true;
    }

    private static boolean close(double a, double b, double abs, double rel) {
        double diff = Math.abs(a - b);
        double scale = Math.max(Math.abs(a), Math.abs(b));
        return diff <= abs || diff <= rel * Math.max(1.0, scale);
    }

    private static boolean sameTriplet(double[] a, double[] b, double abs, double rel) {
        return a != null && b != null
                && a.length == 3 && b.length == 3
                && close(a[0], b[0], abs, rel)
                && close(a[1], b[1], abs, rel)
                && closeAngle(a[2], b[2], 1e-5);
    }

    private static boolean closeAngle(double a, double b, double tol) {
        double d = a - b;
        while (d <= -Math.PI) {
            d += TWO_PI;
        }
        while (d > Math.PI) {
            d -= TWO_PI;
        }
        return Math.abs(d) <= tol;
    }

    private static String formatArray(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}