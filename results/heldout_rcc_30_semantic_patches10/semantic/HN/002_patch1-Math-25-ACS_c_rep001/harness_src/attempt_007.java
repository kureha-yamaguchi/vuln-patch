package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        weightedObservedPointRoundTrip(data);
        exactMath844Oracle();
        generalizedTriangularWaveOracle(data);
        harmonicKnownAnswerOracle(data);
        harmonicFitterPublicApiOracle();
    }

    private static void weightedObservedPointRoundTrip(FuzzedDataProvider data) {
        double w = data.consumeInt(-1000, 1000) / 10.0;
        double x = data.consumeInt(-1000, 1000) / 10.0;
        double y = data.consumeInt(-1000, 1000) / 10.0;
        WeightedObservedPoint p;
        try {
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }

        boolean violated =
                Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
                Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
                Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y);
        if (violated) {
            throw new FuzzerSecurityIssueLow(
                    "relation weighted_observed_point_getters_roundtrip_constructor_args violated: " +
                    "weight=" + p.getWeight() + "/" + w +
                    " x=" + p.getX() + "/" + x +
                    " y=" + p.getY() + "/" + y);
        }
    }

    private static void exactMath844Oracle() {
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
        try {
            guesser.guess();
            violated = true;
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException but got " +
                    t.getClass().getName(), t);
        }

        if (violated) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException but guess() completed normally");
        }
    }

    private static void generalizedTriangularWaveOracle(FuzzedDataProvider data) {
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

        WeightedObservedPoint[] points;
        HarmonicFitter.ParameterGuesser guesser;
        try {
            points = new WeightedObservedPoint[base.length];
            for (int i = 0; i < base.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);
            }
            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        boolean violated = false;
        String detail = "completed normally";
        Throwable wrong = null;
        try {
            guesser.guess();
            violated = true;
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable t) {
            violated = true;
            detail = "wrong exception class " + t.getClass().getName();
            wrong = t;
        }

        if (violated) {
            if (wrong != null) {
                throw new FuzzerSecurityIssueLow(
                        "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but " +
                        detail + " start=" + start + " step=" + step + " scale=" + scale, wrong);
            }
            throw new FuzzerSecurityIssueLow(
                    "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but " +
                    detail + " start=" + start + " step=" + step + " scale=" + scale);
        }
    }

    private static void harmonicKnownAnswerOracle(FuzzedDataProvider data) {
        final double amplitude = data.consumeInt(1, 5);
        final double omega = data.consumeInt(1, 10) / 10.0;
        final double phi = 0.0;
        final int n = data.consumeInt(12, 40);
        final double step = data.consumeInt(1, 5) / 10.0;
        final int start = data.consumeInt(-20, 20);

        final HarmonicOscillator osc;
        final WeightedObservedPoint[] points;
        final HarmonicFitter.ParameterGuesser guesser;
        final double[] first;
        final double[] second;
        try {
            osc = new HarmonicOscillator(amplitude, omega, phi);
            points = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double x = start + i * step;
                points[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
            }
            guesser = new HarmonicFitter.ParameterGuesser(points);
            first = guesser.guess();
            second = guesser.guess();
        } catch (Throwable t) {
            return;
        }

        // Contract justification: guess() estimates coefficients from the fixed observation sample.
        // For a sample constructed exactly from a harmonic oscillator with known parameters, a correct
        // implementation must recover those established amplitude/frequency values (oracle from input itself).
        if (!isFinite(first[0]) || !isFinite(first[1]) ||
            !close(first[0], amplitude, 1e-2) || !close(first[1], omega, 1e-2)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:known-harmonic] semantic mismatch: expected amplitude=" + amplitude +
                    " omega=" + omega + " but got amplitude=" + first[0] + " omega=" + first[1]);
        }

        // Contract justification: repeated guess() on the same immutable logical sample must agree.
        // The method may sort internally, but that normalization cannot change the coefficients it reports.
        if (!sameTriple(first, second, 1e-12)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:guess-idempotence] metamorphic violation: repeated guess() disagreed " +
                    "first=[" + first[0] + "," + first[1] + "," + first[2] + "]" +
                    " second=[" + second[0] + "," + second[1] + "," + second[2] + "]");
        }
    }

    private static void harmonicFitterPublicApiOracle() {
        final double[] y = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
        };
        final HarmonicFitter fitter;
        try {
            fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < y.length; i++) {
                fitter.addObservedPoint(1.0, i, y[i]);
            }
        } catch (Throwable t) {
            return;
        }

        boolean violated = false;
        try {
            fitter.fit();
            violated = true;
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable t) {
            return;
        }

        if (violated) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:public-fit-path] semantic mismatch: HarmonicFitter.fit() should propagate the MathIllegalStateException from parameter guessing on the Math844 sample");
        }
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static boolean close(double a, double b, double tol) {
        return isFinite(a) && isFinite(b) && Math.abs(a - b) <= tol;
    }

    private static boolean sameTriple(double[] a, double[] b, double tol) {
        return a != null && b != null && a.length == 3 && b.length == 3 &&
               close(a[0], b[0], tol) &&
               close(a[1], b[1], tol) &&
               close(a[2], b[2], tol);
    }
}