package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1, i, y[i]);
        }

        try {
            new HarmonicFitter.ParameterGuesser(points).guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException for the exact testMath844 sample, but guess() returned normally"
            );
        } catch (MathIllegalStateException expected) {
            // Ground-truth lifted from HarmonicFitterTest.testMath844.
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        final int n = 4 + data.consumeInt(0, 12);
        final double amplitude = data.consumeInt(-20, 20);
        final double omega = (data.consumeInt(1, 1000)) / 100.0;
        final double phase = (data.consumeInt(-314, 314)) / 100.0;
        final double step = (data.consumeInt(1, 100)) / 10.0;
        final double start = data.consumeInt(-100, 100);

        if (amplitude == 0.0 || omega == 0.0 || step == 0.0) {
            return;
        }

        final HarmonicOscillator oscillator = new HarmonicOscillator(amplitude, omega, phase);
        final WeightedObservedPoint[] original = new WeightedObservedPoint[n];
        final WeightedObservedPoint[] snapshot = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            final double x = start + i * step;
            final double v = oscillator.value(x);
            final WeightedObservedPoint p = new WeightedObservedPoint(1.0, x, v);
            original[i] = p;
            snapshot[i] = p;
        }

        final HarmonicFitter.ParameterGuesser constructed;
        try {
            constructed = new HarmonicFitter.ParameterGuesser(original);
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        if (original.length == 0) {
            return;
        }

        final int mutations = 1 + data.consumeInt(0, Math.max(1, n) - 1);
        for (int m = 0; m < mutations; m++) {
            final int idx = data.consumeInt(0, n - 1);
            final double mx = start + data.consumeInt(-100, 100);
            final double my = data.consumeInt(-1000, 1000) / 10.0;
            original[idx] = new WeightedObservedPoint(1.0, mx, my);
        }

        final double[] lhs;
        final double[] rhs;
        try {
            lhs = constructed.guess();
            rhs = new HarmonicFitter.ParameterGuesser(snapshot).guess();
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        if (lhs == null || rhs == null || lhs.length != 3 || rhs.length != 3) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            if (Double.doubleToLongBits(lhs[i]) != Double.doubleToLongBits(rhs[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:constructor-clone] metamorphic violation: ParameterGuesser(WeightedObservedPoint[] observations) documents and implements cloning of the input array, so replacing slots in the caller's array after construction must not change what guess() reads from internal state; index="
                        + i + " lhs=" + lhs[i] + " rhs=" + rhs[i]
                );
            }
        }
    }
}