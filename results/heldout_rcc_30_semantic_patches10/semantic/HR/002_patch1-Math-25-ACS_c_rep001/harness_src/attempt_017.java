package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkWeightedObservedPointGetters(data);
        checkLiftedMath844ExceptionOracle();
        checkGuesserAmplitudeOmegaAreXTranslationInvariant(data);
    }

    private static void checkWeightedObservedPointGetters(FuzzedDataProvider data) {
        double w = Double.longBitsToDouble(data.consumeInt() ^ (((long) data.consumeInt()) << 32));
        double x = Double.longBitsToDouble(data.consumeInt() ^ (((long) data.consumeInt()) << 32));
        double y = Double.longBitsToDouble(data.consumeInt() ^ (((long) data.consumeInt()) << 32));
        WeightedObservedPoint p;
        try {
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }

        boolean bad =
                Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
                Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
                Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y);
        if (bad) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:weighted-point-immutability] semantic mismatch: constructor=("
                            + w + "," + x + "," + y + ") getters=("
                            + p.getWeight() + "," + p.getX() + "," + p.getY() + ")");
        }
    }

    private static void checkLiftedMath844ExceptionOracle() {
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

        HarmonicFitter.ParameterGuesser guesser;
        try {
            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        boolean violated = false;
        String got = "completed normally";
        try {
            guesser.guess();
            violated = true;
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable t) {
            violated = true;
            got = "wrong exception " + t.getClass().getName();
        }

        if (violated) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math844-lifted] semantic mismatch: expected MathIllegalStateException, got " + got);
        }
    }

    private static void checkGuesserAmplitudeOmegaAreXTranslationInvariant(FuzzedDataProvider data) {
        int n = data.consumeInt(8, 40);
        double amplitude = 0.25 + (data.consumeInt(1, 400) / 20.0);
        double omega = 0.05 + (data.consumeInt(1, 400) / 80.0);
        double phi = -Math.PI + (data.consumeInt(0, 6283) / 1000.0);
        double xStart = data.consumeInt(-1000, 1000) / 10.0;

        double period = (2.0 * Math.PI) / omega;
        double stepDivisor = data.consumeInt(n + 3, n + 40);
        double step = period / stepDivisor;
        double translation = (data.consumeInt(-50, 50) / 7.0) * step;
        if (translation == 0.0) {
            translation = step / 5.0;
        }

        WeightedObservedPoint[] base = new WeightedObservedPoint[n];
        WeightedObservedPoint[] shifted = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            double x1 = xStart + i * step;
            double x2 = x1 + translation;
            double y1 = amplitude * Math.cos(omega * x1 + phi);
            double y2 = amplitude * Math.cos(omega * x2 + phi);
            base[i] = new WeightedObservedPoint(1.0, x1, y1);
            shifted[i] = new WeightedObservedPoint(1.0, x2, y2);
        }

        double[] g1;
        double[] g2;
        try {
            g1 = new HarmonicFitter.ParameterGuesser(base).guess();
            g2 = new HarmonicFitter.ParameterGuesser(shifted).guess();
        } catch (Throwable t) {
            return;
        }

        if (g1 == null || g2 == null || g1.length < 2 || g2.length < 2) {
            return;
        }

        double ampTol = Math.max(1e-6, Math.max(Math.abs(g1[0]), Math.abs(g2[0])) * 1e-4);
        double omegaTol = Math.max(1e-6, Math.max(Math.abs(g1[1]), Math.abs(g2[1])) * 1e-4);

        boolean badAmp = Math.abs(g1[0] - g2[0]) > ampTol;
        boolean badOmega = Math.abs(g1[1] - g2[1]) > omegaTol;

        if (badAmp || badOmega) {
            /*
             * Contract justification: guess() estimates [Amplitude, Angular frequency, Phase]
             * for samples of a harmonic function y = A cos(omega x + phi). Translating all
             * abscissae by a constant changes only the phase term (phi' = phi + omega * delta);
             * amplitude A and angular frequency omega are invariants of the same underlying
             * harmonic signal. A patch that merely deletes/avoids the MathIllegalStateException
             * in guessAOmega can still leave the computed shared state a/omega wrong; this
             * cross-check compares those two quantities across two real calls that differ only
             * by x-translation and therefore must agree for every correct implementation.
             */
            throw new FuzzerSecurityIssueLow(
                    "[oracle:guesser-x-translation] metamorphic violation: translated exact harmonic sample changed invariant parameters"
                            + " amplitude1=" + g1[0]
                            + " amplitude2=" + g2[0]
                            + " omega1=" + g1[1]
                            + " omega2=" + g2[1]
                            + " translation=" + translation
                            + " n=" + n
                            + " step=" + step
                            + " trueAmplitude=" + amplitude
                            + " trueOmega=" + omega);
        }
    }
}