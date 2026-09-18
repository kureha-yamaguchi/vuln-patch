package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        boolean sawExpected = false;
        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            double[] guess = guesser.guess();
            throw new FuzzerSecurityIssueLow("[oracle:math844-parameter-guesser] semantic mismatch: expected MathIllegalStateException but guess() returned amplitude=" + guess[0] + " omega=" + guess[1] + " phase=" + guess[2]);
        } catch (MathIllegalStateException expected) {
            sawExpected = true;
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            // Other rejections are not a trusted oracle for this input.
        }

        try {
            HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                fitter.addObservedPoint(points[i]);
            }
            double[] fit = fitter.fit();
            throw new FuzzerSecurityIssueLow("[oracle:math844-fit-noarg] semantic mismatch: expected MathIllegalStateException but fit() returned amplitude=" + fit[0] + " omega=" + fit[1] + " phase=" + fit[2]);
        } catch (MathIllegalStateException expected) {
            sawExpected = true;
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            // Skip untrusted outcomes.
        }

        int n = data.consumeInt(4, 12);
        double amplitude = data.consumeInt(-20, 20);
        if (amplitude == 0.0) {
            amplitude = 1.0;
        }
        double omega = data.consumeInt(1, 20) / 5.0;
        double phase = data.consumeInt(-20, 20) / 7.0;
        double step = data.consumeInt(1, 5) / 4.0;
        double start = data.consumeInt(-10, 10);

        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] harmonicPoints = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            double x = start + i * step;
            harmonicPoints[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
        }

        try {
            HarmonicFitter.ParameterGuesser g1 = new HarmonicFitter.ParameterGuesser(harmonicPoints);
            double[] before = g1.guess();

            // Constructor body shown in the prompt clones the observations array.
            // Therefore later mutations of the caller-owned array must not affect
            // what guess() reads from the already-constructed guesser.
            int slot = data.consumeInt(0, n - 1);
            harmonicPoints[slot] = new WeightedObservedPoint(1.0, start + 123.0, amplitude * 17.0);

            double[] after = g1.guess();
            if (!sameTriple(before, after)) {
                throw new RuntimeException("[oracle:constructor-clone] metamorphic violation: ParameterGuesser must be insulated from later caller array mutations because constructor clones observations input; slot=" + slot + " before=" + tripleToString(before) + " after=" + tripleToString(after));
            }
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
        }

        try {
            HarmonicFitter.ParameterGuesser seedGuesser = new HarmonicFitter.ParameterGuesser(harmonicPoints.clone());
            double[] guessed = seedGuesser.guess();

            HarmonicFitter fitterAuto = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            HarmonicFitter fitterManual = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                fitterAuto.addObservedPoint(harmonicPoints[i]);
                fitterManual.addObservedPoint(harmonicPoints[i]);
            }

            // The no-arg overload is documented as the same fit operation with an
            // automatically computed initial guess. Supplying exactly that guessed
            // array to the other overload must therefore yield the same result on a
            // fresh fitter with the same observations.
            double[] auto = fitterAuto.fit();
            double[] manual = fitterManual.fit(guessed);
            if (!sameTriple(auto, manual)) {
                throw new RuntimeException("[oracle:fit-overload-equivalence] metamorphic violation: fit() must agree with fit(ParameterGuesser(points).guess()) on identical observations input=" + pointsToString(harmonicPoints) + " lhs=" + tripleToString(auto) + " rhs=" + tripleToString(manual));
            }
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
        }

        if (!sawExpected && data.consumeBoolean()) {
            // Keep exercising the exact regression input even when only the public API path ran.
        }
    }

    private static boolean sameTriple(double[] a, double[] b) {
        if (a == null || b == null || a.length != 3 || b.length != 3) {
            return false;
        }
        return sameDouble(a[0], b[0]) && sameDouble(a[1], b[1]) && sameDouble(a[2], b[2]);
    }

    private static boolean sameDouble(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    private static String tripleToString(double[] v) {
        if (v == null) {
            return "null";
        }
        return "[" + v[0] + "," + v[1] + "," + v[2] + "]";
    }

    private static String pointsToString(WeightedObservedPoint[] pts) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < pts.length; i++) {
            if (i != 0) {
                sb.append(";");
            }
            sb.append("(").append(pts[i].getWeight()).append(",").append(pts[i].getX()).append(",").append(pts[i].getY()).append(")");
        }
        sb.append("]");
        return sb.toString();
    }
}