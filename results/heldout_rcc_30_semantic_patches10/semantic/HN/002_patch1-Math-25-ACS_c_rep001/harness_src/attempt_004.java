package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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

        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() for the triangular-wave sample used by HarmonicFitterTest.testMath844"
            );
        } catch (MathIllegalStateException expected) {
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        int sampleCount = data.consumeInt(8, 20);
        double amplitude = 0.5 + (data.consumeInt(0, 4500) / 1000.0);
        double omega = 0.2 + (data.consumeInt(0, 2800) / 1000.0);
        double phase = -Math.PI + (data.consumeInt(0, 6283) / 1000.0);
        double step = 0.1 + (data.consumeInt(0, 900) / 1000.0);
        double start = data.consumeInt(-1000, 1000) / 100.0;

        HarmonicOscillator oscillator = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] harmonicPoints = new WeightedObservedPoint[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double x = start + i * step;
            harmonicPoints[i] = new WeightedObservedPoint(1.0, x, oscillator.value(x));
        }

        try {
            HarmonicFitter.ParameterGuesser sortedGuesser = new HarmonicFitter.ParameterGuesser(harmonicPoints);
            double[] sortedGuess = sortedGuesser.guess();

            WeightedObservedPoint[] reversed = harmonicPoints.clone();
            for (int i = 0, j = reversed.length - 1; i < j; i++, j--) {
                WeightedObservedPoint tmp = reversed[i];
                reversed[i] = reversed[j];
                reversed[j] = tmp;
            }
            HarmonicFitter.ParameterGuesser reversedGuesser = new HarmonicFitter.ParameterGuesser(reversed);
            double[] reversedGuess = reversedGuesser.guess();

            if (sortedGuess.length != reversedGuess.length) {
                throw new RuntimeException(
                    "[oracle:guess-permutation] metamorphic violation: guess() length changed under permutation lhs="
                        + Arrays.toString(sortedGuess) + " rhs=" + Arrays.toString(reversedGuess)
                );
            }

            for (int i = 0; i < sortedGuess.length; i++) {
                double a = sortedGuess[i];
                double b = reversedGuess[i];
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                if (Double.isNaN(a) != Double.isNaN(b) || Math.abs(a - b) > 1.0e-7 * scale) {
                    throw new RuntimeException(
                        "[oracle:guess-permutation] metamorphic violation: guess() should be permutation-invariant because ParameterGuesser sorts observations with respect to abscissa before computing a and omega; input="
                            + Arrays.toString(harmonicPoints) + " lhs=" + Arrays.toString(sortedGuess)
                            + " rhs=" + Arrays.toString(reversedGuess)
                    );
                }
            }
        } catch (RuntimeException ex) { /*__vpRepair*/ if (ex instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ex;
            if (ex.getMessage() != null && ex.getMessage().startsWith("[oracle:")) {
                throw ex;
            }
            return;
        }

        try {
            double[] initialGuess = new HarmonicFitter.ParameterGuesser(harmonicPoints).guess();

            HarmonicFitter fitterAuto = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            HarmonicFitter fitterExplicit = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < harmonicPoints.length; i++) {
                WeightedObservedPoint p = harmonicPoints[i];
                fitterAuto.addObservedPoint(p.getWeight(), p.getX(), p.getY());
                fitterExplicit.addObservedPoint(p.getWeight(), p.getX(), p.getY());
            }

            double[] autoFit = fitterAuto.fit();
            double[] explicitFit = fitterExplicit.fit(initialGuess);

            if (autoFit.length != explicitFit.length) {
                throw new RuntimeException(
                    "[oracle:fit-overloads] metamorphic violation: fit() and fit(initialGuess) returned different lengths input="
                        + Arrays.toString(initialGuess) + " lhs=" + Arrays.toString(autoFit)
                        + " rhs=" + Arrays.toString(explicitFit)
                );
            }

            for (int i = 0; i < autoFit.length; i++) {
                double a = autoFit[i];
                double b = explicitFit[i];
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                if (Double.isNaN(a) != Double.isNaN(b) || Math.abs(a - b) > 1.0e-6 * scale) {
                    throw new RuntimeException(
                        "[oracle:fit-overloads] metamorphic violation: the public overloads are documented as equivalent when fit(initialGuess) is given the same first guess that fit() computes internally; input="
                            + Arrays.toString(initialGuess) + " lhs=" + Arrays.toString(autoFit)
                            + " rhs=" + Arrays.toString(explicitFit)
                    );
                }
            }
        } catch (RuntimeException ex) { /*__vpRepair*/ if (ex instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ex;
            if (ex.getMessage() != null && ex.getMessage().startsWith("[oracle:")) {
                throw ex;
            }
        }
    }
}