package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            double w = data.consumeInt(-1000, 1000) / 10.0;
            double x = data.consumeInt(-1000, 1000) / 10.0;
            double y = data.consumeInt(-1000, 1000) / 10.0;
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
                        "relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                                + "w=" + w + " gotW=" + p.getWeight()
                                + " x=" + x + " gotX=" + p.getX()
                                + " y=" + y + " gotY=" + p.getY());
            }
        }

        {
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

            boolean violated = false;
            Throwable wrong = null;
            String detail = "completed normally";
            try {
                HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
                guesser.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but " + detail;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }

            violated = false;
            wrong = null;
            detail = "completed normally";
            try {
                HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < len; i++) {
                    fitter.addObservedPoint(points[i]);
                }
                fitter.fit();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:harmonicfitter-fit-seed] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit(), but " + detail;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }
        }

        {
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);

            final int[] base = new int[] { 0, 1, 2, 3, 2, 1,
                                           0, -1, -2, -3, -2, -1,
                                           0, 1, 2, 3, 2, 1,
                                           0, -1, -2, -3, -2, -1,
                                           0, 1, 2, 3, 2, 1, 0 };
            final WeightedObservedPoint[] points = new WeightedObservedPoint[base.length];
            for (int i = 0; i < base.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);
            }

            boolean violated = false;
            Throwable wrong = null;
            String detail = "completed normally";
            try {
                HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
                guesser.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:triangular-family-guesser] semantic mismatch: expected MathIllegalStateException on valid triangular-wave family input, but " + detail
                        + " scale=" + scale + " step=" + step + " start=" + start;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }

            violated = false;
            wrong = null;
            detail = "completed normally";
            try {
                HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < points.length; i++) {
                    fitter.addObservedPoint(points[i]);
                }
                fitter.fit();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:triangular-family-fit] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on valid triangular-wave family input, but " + detail
                        + " scale=" + scale + " step=" + step + " start=" + start;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }
        }

        {
            int amplitudeInt = data.consumeInt(1, 8);
            int omegaInt = data.consumeInt(1, 5);
            int phaseInt = data.consumeInt(-3, 3);
            int sampleCount = data.consumeInt(8, 20);

            double amplitude = amplitudeInt;
            double omega = omegaInt / 5.0;
            double phase = phaseInt / 4.0;

            WeightedObservedPoint[] points = new WeightedObservedPoint[sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                double x = i;
                double y = amplitude * Math.cos(omega * x + phase);
                points[i] = new WeightedObservedPoint(1.0, x, y);
            }

            double[] guessed;
            double[] guessedAgain;
            try {
                HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
                guessed = guesser.guess();
                guessedAgain = guesser.guess();
            } catch (Throwable t) {
                return;
            }

            if (guessed == null || guessedAgain == null || guessed.length != 3 || guessedAgain.length != 3) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:guess-shape] semantic mismatch: guess() must return 3 coefficients, got "
                                + (guessed == null ? "null" : guessed.length) + " and "
                                + (guessedAgain == null ? "null" : guessedAgain.length));
            }

            /* Documented contract: ParameterGuesser.guess() estimates coefficients from the stored observations.
               Constructor clones the observations array, and guess() is a reader of that fixed sample.
               Therefore repeated guess() calls on the same object and unchanged state must agree; a throw-deleting
               or state-corrupting patch in guessAOmega can silently change shared fields a/omega between reads. */
            for (int i = 0; i < 3; i++) {
                long a = Double.doubleToLongBits(guessed[i]);
                long b = Double.doubleToLongBits(guessedAgain[i]);
                if (a != b) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:guess-idempotence] metamorphic violation: repeated guess() on identical state disagreed"
                                    + " idx=" + i
                                    + " first=" + guessed[i]
                                    + " second=" + guessedAgain[i]
                                    + " amplitude=" + amplitude
                                    + " omega=" + omega
                                    + " phase=" + phase
                                    + " sampleCount=" + sampleCount);
                }
            }

            double[] fitAuto;
            double[] fitManual;
            try {
                HarmonicFitter fitterAuto = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                HarmonicFitter fitterManual = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < points.length; i++) {
                    fitterAuto.addObservedPoint(points[i]);
                    fitterManual.addObservedPoint(points[i]);
                }
                fitAuto = fitterAuto.fit();
                fitManual = fitterManual.fit(guessed.clone());
            } catch (Throwable t) {
                return;
            }

            if (fitAuto == null || fitManual == null || fitAuto.length != 3 || fitManual.length != 3) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:fit-shape] semantic mismatch: fit overloads must return 3 coefficients, got "
                                + (fitAuto == null ? "null" : fitAuto.length) + " and "
                                + (fitManual == null ? "null" : fitManual.length));
            }

            /* Sibling-agreement check: fit() and fit(double[] initialGuess) are same-name overloads documented to do
               the same fitting job, differing only in whether the initial guess is supplied. Using the library's own
               ParameterGuesser output as the explicit initialGuess reproduces fit()'s public setup, so both overloads
               should agree on the same logical input. */
            for (int i = 0; i < 3; i++) {
                double lhs = fitAuto[i];
                double rhs = fitManual[i];
                if (Double.isNaN(lhs) || Double.isNaN(rhs) || Math.abs(lhs - rhs) > 1.0e-6) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:fit-overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagreed"
                                    + " idx=" + i
                                    + " lhs=" + lhs
                                    + " rhs=" + rhs
                                    + " guess0=" + guessed[0]
                                    + " guess1=" + guessed[1]
                                    + " guess2=" + guessed[2]
                                    + " amplitude=" + amplitude
                                    + " omega=" + omega
                                    + " phase=" + phase
                                    + " sampleCount=" + sampleCount);
                }
            }
        }
    }
}