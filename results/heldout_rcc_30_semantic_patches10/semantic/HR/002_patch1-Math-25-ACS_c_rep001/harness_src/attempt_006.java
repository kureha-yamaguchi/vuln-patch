package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double w = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        double x = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        double y = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
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
                    "[oracle:weighted-point-getters] consistency violation: expected=("
                            + w + "," + x + "," + y + ") actual=("
                            + p.getWeight() + "," + p.getX() + "," + p.getY() + ")");
        }

        WeightedObservedPoint[] seedPoints = buildMath844Points();
        HarmonicFitter.ParameterGuesser seedGuesser;
        try {
            seedGuesser = new HarmonicFitter.ParameterGuesser(seedPoints);
        } catch (Throwable t) {
            return;
        }
        boolean wrong = false;
        String got = "completed normally";
        try {
            seedGuesser.guess();
            wrong = true;
        } catch (MathIllegalStateException expected) {
        } catch (Throwable t) {
            wrong = true;
            got = "wrong exception " + t.getClass().getName();
        }
        if (wrong) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException, got " + got);
        }

        int n = data.consumeInt(6, 24);
        double amplitude = 0.1 + data.consumeInt(0, 4999) / 1000.0;
        double omega = 0.1 + data.consumeInt(0, 4999) / 1000.0;
        double phi = data.consumeInt(-3141, 3141) / 1000.0;
        double xStart = data.consumeInt(-1000, 1000) / 10.0;
        double step = 0.05 + data.consumeInt(0, 1999) / 1000.0;
        WeightedObservedPoint[] harmonic = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            double xi = xStart + i * step;
            double yi = amplitude * Math.cos(omega * xi + phi);
            harmonic[i] = new WeightedObservedPoint(1.0, xi, yi);
        }

        HarmonicFitter.ParameterGuesser g1;
        HarmonicFitter.ParameterGuesser g2;
        try {
            g1 = new HarmonicFitter.ParameterGuesser(harmonic);
            g2 = new HarmonicFitter.ParameterGuesser(harmonic.clone());
        } catch (Throwable t) {
            return;
        }

        double[] first;
        double[] second;
        double[] fresh;
        try {
            first = g1.guess();
            second = g1.guess();
            fresh = g2.guess();
        } catch (Throwable t) {
            return;
        }

        if (!sameArray(first, second, 1.0e-12)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:guess-idempotence] metamorphic violation: repeated guess() on the same ParameterGuesser must be stable for a fixed observation set; first="
                            + format(first) + " second=" + format(second));
        }

        if (!sameArray(first, fresh, 1.0e-12)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:guess-fresh-consistency] consistency violation: two identically-constructed guessers over the same observations must report the same guessed parameters; first="
                            + format(first) + " fresh=" + format(fresh));
        }
    }

    private static WeightedObservedPoint[] buildMath844Points() {
        final double[] y = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
        };
        WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }
        return points;
    }

    private static boolean sameArray(double[] a, double[] b, double tol) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double av = a[i];
            double bv = b[i];
            if (Double.isNaN(av) || Double.isNaN(bv) || Double.isInfinite(av) || Double.isInfinite(bv)) {
                if (Double.doubleToLongBits(av) != Double.doubleToLongBits(bv)) {
                    return false;
                }
            } else if (Math.abs(av - bv) > tol) {
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
            if (i != 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}