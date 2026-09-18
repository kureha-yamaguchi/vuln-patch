package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.util.FastMath;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkExactHarmonicReplayConsistency(data);
    }

    private static void checkExactHarmonicReplayConsistency(FuzzedDataProvider data) {
        int rawSamplesPerPeriod = data.consumeInt(8, 32);
        int samplesPerPeriod = ((rawSamplesPerPeriod + 3) / 4) * 4;
        int periods = data.consumeInt(2, 4);

        double amplitude = 0.5 + (data.consumeInt(0, 9500) / 1000.0);
        double omega = 0.2 + (data.consumeInt(0, 1800) / 1000.0);
        double phase = 0.0;

        double period = 2.0 * Math.PI / omega;
        double step = period / samplesPerPeriod;
        int len = samplesPerPeriod * periods + 1;

        WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        HarmonicOscillator source = new HarmonicOscillator(amplitude, omega, phase);
        double empiricalMin = Double.POSITIVE_INFINITY;
        double empiricalMax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < len; i++) {
            double x = i * step;
            double y = source.value(x);
            if (y < empiricalMin) {
                empiricalMin = y;
            }
            if (y > empiricalMax) {
                empiricalMax = y;
            }
            points[i] = new WeightedObservedPoint(1.0, x, y);
        }

        HarmonicFitter.ParameterGuesser guesser;
        double[] guessed;
        try {
            guesser = new HarmonicFitter.ParameterGuesser(points);
            guessed = guesser.guess();
        } catch (MathIllegalStateException e) {
            return;
        } catch (Throwable t) {
            return;
        }

        if (guessed == null || guessed.length != 3) {
            throw new FuzzerSecurityIssueLow("[oracle:exact-harmonic-shape] semantic mismatch: unexpected guess result length");
        }

        double guessedAmplitude = guessed[0];
        double guessedOmega = guessed[1];
        double guessedPhi = guessed[2];

        // Contract/justification:
        // ParameterGuesser.guess() is documented to "Estimate a first guess of the coefficients"
        // (amplitude, angular frequency, phase). Here the observations are constructed exactly
        // from a real HarmonicOscillator with known amplitude and omega, sampled over multiple
        // full periods with quarter-period-aligned points. On such non-degenerate exact harmonic
        // data, a correct implementation's reported coefficients must be consistent with the sample:
        // replaying the guessed oscillator over the same observations should closely reproduce them.
        // A throw-deleting or masked helper bug in guessAOmega can leave guess() returning values,
        // but those returned coefficients will not match the very sample they claim to summarize.
        double maxAbsReplayErr = 0.0;
        HarmonicOscillator replay = new HarmonicOscillator(guessedAmplitude, guessedOmega, guessedPhi);
        for (int i = 0; i < len; i++) {
            double x = points[i].getX();
            double expectedY = points[i].getY();
            double actualY;
            try {
                actualY = replay.value(x);
            } catch (Throwable t) {
                return;
            }
            double err = FastMath.abs(actualY - expectedY);
            if (err > maxAbsReplayErr) {
                maxAbsReplayErr = err;
            }
        }
        double replayTol = amplitude * 1.0e-2 + 1.0e-8;
        if (maxAbsReplayErr > replayTol) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:exact-harmonic-replay] consistency violation: amplitude=" + amplitude +
                " omega=" + omega +
                " guessedA=" + guessedAmplitude +
                " guessedOmega=" + guessedOmega +
                " guessedPhi=" + guessedPhi +
                " maxAbsReplayErr=" + maxAbsReplayErr +
                " tol=" + replayTol);
        }

        // Independent cross-check of the same quantity family obtained a second way:
        // because the sample includes exact maxima and minima (phase 0, samplesPerPeriod multiple of 4),
        // the empirical half-range of the observed y values equals the true amplitude for every correct
        // implementation of this construction. The reported guessed amplitude should agree with that
        // independently recomputed amplitude summary.
        double empiricalAmplitude = 0.5 * (empiricalMax - empiricalMin);
        double ampTol = amplitude * 1.0e-2 + 1.0e-8;
        if (FastMath.abs(guessedAmplitude - empiricalAmplitude) > ampTol) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:empirical-amplitude] consistency violation: empiricalAmplitude=" + empiricalAmplitude +
                " guessedAmplitude=" + guessedAmplitude +
                " sourceAmplitude=" + amplitude +
                " tol=" + ampTol);
        }

        // Second independently known quantity from the constructed input:
        // the period between repeated maxima is samplesPerPeriod * step, so angular frequency must be 2*pi/period.
        double empiricalOmega = 2.0 * Math.PI / (samplesPerPeriod * step);
        double omegaTol = omega * 1.0e-2 + 1.0e-8;
        if (FastMath.abs(guessedOmega - empiricalOmega) > omegaTol) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:empirical-omega] consistency violation: empiricalOmega=" + empiricalOmega +
                " guessedOmega=" + guessedOmega +
                " sourceOmega=" + omega +
                " tol=" + omegaTol);
        }
    }
}