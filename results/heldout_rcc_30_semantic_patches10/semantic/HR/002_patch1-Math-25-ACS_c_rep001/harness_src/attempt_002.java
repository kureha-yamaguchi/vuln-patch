package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int scaleInt = data.consumeInt(-5, 5);
        if (scaleInt == 0) {
            scaleInt = 1;
        }
        double scale = scaleInt;
        double xOffset = data.consumeInt(-20, 20);

        double[] seedY = new double[] {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };

        WeightedObservedPoint[] seedPoints = new WeightedObservedPoint[seedY.length];
        for (int i = 0; i < seedY.length; i++) {
            seedPoints[i] = new WeightedObservedPoint(1.0, xOffset + i, seedY[i] * scale);
        }

        try {
            new HarmonicFitter.ParameterGuesser(seedPoints).guess();
            throw new FuzzerSecurityIssueLow("[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException for Math844 triangular sample but guess() returned normally");
        } catch (MathIllegalStateException expected) {
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        // The constructor clones the array (`this.observations = observations.clone();`), so replacing
        // caller-side slots after construction must not affect the already-built guesser. A patch that
        // merely deletes the throw can still corrupt later behaviour; this checks an independent state
        // invariant visible from the API.
        HarmonicFitter.ParameterGuesser clonedGuesser;
        WeightedObservedPoint[] callerOwned = seedPoints.clone();
        try {
            clonedGuesser = new HarmonicFitter.ParameterGuesser(callerOwned);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        for (int i = 0; i < callerOwned.length; i++) {
            callerOwned[i] = new WeightedObservedPoint(1.0, xOffset + i, (i % 2 == 0) ? 123.0 : -456.0);
        }
        try {
            clonedGuesser.guess();
            throw new FuzzerSecurityIssueLow("[oracle:ctor-clone] semantic mismatch: ParameterGuesser observed caller array replacement after construction; expected same Math844 rejection");
        } catch (MathIllegalStateException expected) {
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        // Flip the patched condition boundary: exercise an input just off the seed by perturbing one point.
        // No assertion here because no trusted exact answer is documented for the nearby case.
        WeightedObservedPoint[] nearBoundary = seedPoints.clone();
        int idx = data.consumeInt(0, nearBoundary.length - 1);
        int deltaInt = data.consumeInt(-3, 3);
        if (deltaInt == 0) {
            deltaInt = 1;
        }
        WeightedObservedPoint p = nearBoundary[idx];
        nearBoundary[idx] = new WeightedObservedPoint(p.getWeight(), p.getX(), p.getY() + deltaInt);
        try {
            new HarmonicFitter.ParameterGuesser(nearBoundary).guess();
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
        }

        // Public-API overload agreement: fit() is documented to auto-compute the initial guess and then fit,
        // while fit(double[]) fits from the supplied initial guess. Therefore, when we explicitly supply the
        // same guess that fit() would compute, both overloads must agree on the fitted parameters.
        exerciseFitOverloadAgreement(data);
    }

    private static void exerciseFitOverloadAgreement(FuzzedDataProvider data) {
        int n = data.consumeInt(4, 10);
        double amplitude = 0.5 + data.consumeInt(0, 40) / 10.0;
        double omega = 0.2 + data.consumeInt(0, 30) / 10.0;
        double phi = data.consumeInt(-20, 20) / 10.0;
        double startX = data.consumeInt(-10, 10) / 10.0;
        double step = 0.2 + data.consumeInt(0, 8) / 10.0;

        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phi);
        WeightedObservedPoint[] points = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            double x = startX + i * step;
            points[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
        }

        final double[] guessed;
        try {
            guessed = new HarmonicFitter.ParameterGuesser(points).guess();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        final double[] autoFit;
        final double[] explicitFit;
        try {
            HarmonicFitter auto = new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            HarmonicFitter explicit = new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            for (WeightedObservedPoint point : points) {
                auto.addObservedPoint(point);
                explicit.addObservedPoint(point);
            }
            autoFit = auto.fit();
            explicitFit = explicit.fit(guessed);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (!sameArray(autoFit, explicitFit, 1e-6)) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() must agree with fit(initialGuess) when initialGuess is ParameterGuesser.guess() inputPoints=" + points.length + " auto=" + arrayToString(autoFit) + " explicit=" + arrayToString(explicitFit) + " guess=" + arrayToString(guessed));
        }
    }

    private static boolean sameArray(double[] a, double[] b, double tol) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!close(a[i], b[i], tol)) {
                return false;
            }
        }
        return true;
    }

    private static boolean close(double x, double y, double tol) {
        if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
            return true;
        }
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isInfinite(x) || Double.isInfinite(y)) {
            return false;
        }
        double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
        return Math.abs(x - y) <= tol * scale;
    }

    private static String arrayToString(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}