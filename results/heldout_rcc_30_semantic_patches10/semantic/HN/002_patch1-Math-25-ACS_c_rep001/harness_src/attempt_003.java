package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final int[] TRIANGULAR_BASE = new int[] {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Trusted lifted oracle from HarmonicFitterTest.testMath844:
        // for this triangular-wave sample, ParameterGuesser.guess() must throw MathIllegalStateException.
        WeightedObservedPoint[] exactTestPoints = new WeightedObservedPoint[TRIANGULAR_BASE.length];
        for (int i = 0; i < TRIANGULAR_BASE.length; i++) {
            exactTestPoints[i] = new WeightedObservedPoint(1.0, i, TRIANGULAR_BASE[i]);
        }
        HarmonicFitter.ParameterGuesser exactGuesser = new HarmonicFitter.ParameterGuesser(exactTestPoints);

        // Also drive the same data through the real public API path.
        try {
            HarmonicFitter publicApiFitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < exactTestPoints.length; i++) {
                publicApiFitter.addObservedPoint(exactTestPoints[i]);
            }
            try {
                publicApiFitter.fit();
            } catch (Throwable ignored) {
                // This call is for reachability through the real public API.
            }
        } catch (Throwable ignored) {
            // If optimizer setup fails in this environment, keep the trusted direct oracle below.
        }

        boolean exactOracleViolated = false;
        String exactDetail = "completed normally";
        Throwable exactCause = null;
        try {
            exactGuesser.guess();
            exactOracleViolated = true;
        } catch (MathIllegalStateException expected) {
            // Expected per the trusted failing test.
        } catch (Throwable t) {
            exactOracleViolated = true;
            exactDetail = "threw wrong exception " + t.getClass().getName();
            exactCause = t;
        }
        if (exactOracleViolated) {
            if (exactCause != null) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but " + exactDetail,
                    exactCause);
            }
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but " + exactDetail);
        }

        // Generalized trusted family from the same seed properties: same triangular waveform shape,
        // with fuzzed positive scale, strictly increasing x, and non-zero range.
        // For this ill-conditioned non-harmonic sample family, guess() must reject with MathIllegalStateException.
        WeightedObservedPoint[] generalizedPoints;
        HarmonicFitter.ParameterGuesser generalizedGuesser;
        Throwable generalizedUnexpected = null;
        String generalizedDetail = "completed normally";
        boolean generalizedViolated = false;
        try {
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);

            generalizedPoints = new WeightedObservedPoint[TRIANGULAR_BASE.length];
            for (int i = 0; i < TRIANGULAR_BASE.length; i++) {
                generalizedPoints[i] = new WeightedObservedPoint(
                    1.0,
                    start + (double) i * step,
                    TRIANGULAR_BASE[i] * (double) scale);
            }
            generalizedGuesser = new HarmonicFitter.ParameterGuesser(generalizedPoints);
        } catch (Throwable e) {
            return;
        }
        try {
            generalizedGuesser.guess();
            generalizedViolated = true;
        } catch (MathIllegalStateException expected) {
            // Expected for this seeded family.
        } catch (Throwable t) {
            generalizedViolated = true;
            generalizedDetail = "threw wrong exception " + t.getClass().getName();
            generalizedUnexpected = t;
        }
        if (generalizedViolated) {
            if (generalizedUnexpected != null) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:triangular-family] semantic mismatch: expected MathIllegalStateException, but " + generalizedDetail,
                    generalizedUnexpected);
            }
            throw new FuzzerSecurityIssueLow(
                "[oracle:triangular-family] semantic mismatch: expected MathIllegalStateException, but " + generalizedDetail);
        }

        // Invariant from WeightedObservedPoint's immutable container contract:
        // getters must return exactly the constructor arguments.
        double w;
        double x;
        double y;
        WeightedObservedPoint p;
        try {
            w = data.consumeInt(-1000, 1000) / 10.0;
            x = data.consumeInt(-1000, 1000) / 10.0;
            y = data.consumeInt(-1000, 1000) / 10.0;
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable e) {
            return;
        }
        if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
            Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
            Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:wop-roundtrip] semantic mismatch: weight/x/y getters did not return constructor arguments"
                    + " expected=(" + w + "," + x + "," + y + ")"
                    + " actual=(" + p.getWeight() + "," + p.getX() + "," + p.getY() + ")");
        }

        // Post-condition / hidden-state check:
        // ParameterGuesser(WeightedObservedPoint[]) clones the input array ("this.observations = observations.clone();").
        // Therefore replacing elements in the caller's original array after construction must not change what guess() reads.
        // A patch that silently corrupts shared state or stops using the constructor-established observation set breaks this.
        try {
            int n = data.consumeInt(6, 14);
            int xStep = data.consumeInt(1, 4);
            int xStart = data.consumeInt(-10, 10);
            double amplitude = data.consumeInt(1, 5);
            double omega = data.consumeInt(1, 5) / 5.0;
            double phi = data.consumeInt(-15, 15) / 10.0;

            HarmonicOscillator oscillator = new HarmonicOscillator(amplitude, omega, phi);
            WeightedObservedPoint[] original = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double xi = xStart + i * xStep;
                original[i] = new WeightedObservedPoint(1.0, xi, oscillator.value(xi));
            }

            HarmonicFitter.ParameterGuesser cloneCheckGuesser = new HarmonicFitter.ParameterGuesser(original);
            double[] before = cloneCheckGuesser.guess();
            double[] beforeAgain = cloneCheckGuesser.guess();

            if (!sameBits(before, beforeAgain)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:guess-idempotent] metamorphic violation: repeated guess() on the same receiver changed result"
                        + " first=" + format(before) + " second=" + format(beforeAgain));
            }

            int mutations = data.consumeInt(1, n);
            for (int i = 0; i < mutations; i++) {
                int idx = data.consumeInt(0, n - 1);
                double xi = xStart + (n + i + 1) * xStep;
                original[idx] = new WeightedObservedPoint(
                    7.0,
                    xi,
                    data.consumeInt(-50, 50));
            }

            double[] after = cloneCheckGuesser.guess();
            if (!sameBits(before, after)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:constructor-clone] metamorphic violation: mutating caller array after ParameterGuesser construction changed guess()"
                        + " before=" + format(before) + " after=" + format(after));
            }
        } catch (Throwable e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw (FuzzerSecurityIssueLow) e;
            }
            return;
        }

        // Sibling-agreement check on the real public API:
        // fit() and fit(double[] initialGuess) do the same fitting job; using the guess produced from the same observations
        // should make the overloads agree. If either side throws, the check does not apply and we skip.
        try {
            int n = data.consumeInt(6, 16);
            int xStep = data.consumeInt(1, 3);
            int xStart = data.consumeInt(-8, 8);
            double amplitude = data.consumeInt(1, 4);
            double omega = data.consumeInt(1, 6) / 4.0;
            double phi = data.consumeInt(-20, 20) / 10.0;
            HarmonicOscillator oscillator = new HarmonicOscillator(amplitude, omega, phi);

            WeightedObservedPoint[] pts = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double xi = xStart + i * xStep;
                pts[i] = new WeightedObservedPoint(1.0, xi, oscillator.value(xi));
            }

            HarmonicFitter.ParameterGuesser pg = new HarmonicFitter.ParameterGuesser(pts);
            double[] initialGuess = pg.guess();

            HarmonicFitter fitterA = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            HarmonicFitter fitterB = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < pts.length; i++) {
                fitterA.addObservedPoint(pts[i]);
                fitterB.addObservedPoint(pts[i]);
            }

            double[] fitNoArg = fitterA.fit();
            double[] fitWithGuess = fitterB.fit(initialGuess);

            if (!approximatelyEqual(fitNoArg, fitWithGuess, 1e-6)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:fit-overloads] metamorphic violation: fit() and fit(initialGuess) disagreed"
                        + " fit()=" + format(fitNoArg)
                        + " fit(initialGuess)=" + format(fitWithGuess));
            }
        } catch (Throwable e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw (FuzzerSecurityIssueLow) e;
            }
            return;
        }
    }

    private static boolean sameBits(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.doubleToLongBits(a[i]) != Double.doubleToLongBits(b[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean approximatelyEqual(double[] a, double[] b, double tol) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) || Double.isNaN(b[i]) || Double.isInfinite(a[i]) || Double.isInfinite(b[i])) {
                return false;
            }
            if (Math.abs(a[i] - b[i]) > tol) {
                return false;
            }
        }
        return true;
    }

    private static String format(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}