package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] y = {
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

        try {
            new HarmonicFitter.ParameterGuesser(points).guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() on the triangular-wave sample from HarmonicFitterTest.testMath844, but the call returned normally"
            );
        } catch (MathIllegalStateException expected) {
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() on the triangular-wave sample from HarmonicFitterTest.testMath844, but got " + other.getClass().getName(),
                other
            );
        }

        int n = data.consumeInt(8, 24);
        double amplitude = 0.5 + data.consumeInt(0, 400) / 40.0;
        double omega = 0.1 + data.consumeInt(0, 200) / 40.0;
        double phase = -Math.PI + (2.0 * Math.PI * data.consumeInt(0, 1000) / 1000.0);
        double step = 0.2 + data.consumeInt(0, 20) / 10.0;
        double start = -5.0 + data.consumeInt(0, 100) / 10.0;

        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] sorted = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            double x = start + i * step;
            sorted[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
        }

        WeightedObservedPoint[] permuted = new WeightedObservedPoint[n];
        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            for (int i = 0; i < n; i++) {
                permuted[i] = sorted[n - 1 - i];
            }
        } else if (mode == 1) {
            int shift = data.consumeInt(0, n - 1);
            for (int i = 0; i < n; i++) {
                permuted[i] = sorted[(i + shift) % n];
            }
        } else {
            int left = 0;
            int right = n - 1;
            int idx = 0;
            while (left <= right) {
                permuted[idx++] = sorted[right--];
                if (left <= right) {
                    permuted[idx++] = sorted[left++];
                }
            }
        }

        double[] g1;
        double[] g2;
        try {
            g1 = new HarmonicFitter.ParameterGuesser(sorted).guess();
            g2 = new HarmonicFitter.ParameterGuesser(permuted).guess();
        } catch (Throwable ignored) {
            return;
        }

        if (!sameBits(g1, g2)) {
            throw new RuntimeException(
                "[oracle:order-invariance] metamorphic violation: ParameterGuesser.guess() must be invariant under reordering of the same observation set because the implementation sorts observations by abscissa before computing shared state; inputSize=" + n +
                " lhs=" + arrayToString(g1) +
                " rhs=" + arrayToString(g2)
            );
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

    private static String arrayToString(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}