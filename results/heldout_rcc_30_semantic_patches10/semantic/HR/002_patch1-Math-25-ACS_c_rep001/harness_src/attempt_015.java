package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkWeightedObservedPointGetters(data);
        checkMath844LiftedOracle(data);
        checkCallerArrayIsolationAfterGuess(data);
    }

    private static void checkWeightedObservedPointGetters(FuzzedDataProvider data) {
        double w = finiteFromInt(data.consumeInt(-1000000, 1000000)) / 1024.0;
        double x = finiteFromInt(data.consumeInt(-1000000, 1000000)) / 256.0;
        double y = finiteFromInt(data.consumeInt(-1000000, 1000000)) / 256.0;

        WeightedObservedPoint p;
        try {
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }

        boolean violated =
                Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
                Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
                Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y);
        if (violated) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:weighted-point-getters] consistency violation: expected=("
                            + bits(w) + "," + bits(x) + "," + bits(y) + ") actual=("
                            + bits(p.getWeight()) + "," + bits(p.getX()) + "," + bits(p.getY()) + ")");
        }
    }

    private static void checkMath844LiftedOracle(FuzzedDataProvider data) {
        int variant = data.consumeInt(0, 2);
        double[] y;
        if (variant == 0) {
            y = new double[] {
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1, 0
            };
        } else if (variant == 1) {
            y = new double[] {
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1, 0
            };
        } else {
            y = new double[] {
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1, 0
            };
        }

        int start = data.consumeInt(-8, 8);
        int step = data.consumeInt(1, 4);

        WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, start + step * i, y[i]);
        }

        HarmonicFitter.ParameterGuesser g;
        try {
            g = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        boolean violated = false;
        String got = "completed normally";
        try {
            g.guess();
            violated = true;
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable t) {
            violated = true;
            got = t.getClass().getName();
        }

        if (violated) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math844-lifted] semantic mismatch: expected MathIllegalStateException, got " + got);
        }
    }

    private static void checkCallerArrayIsolationAfterGuess(FuzzedDataProvider data) {
        int len = data.consumeInt(8, 24);
        double amplitude = data.consumeInt(1, 50);
        double omega = data.consumeInt(1, 40) / 10.0;
        double phi = data.consumeInt(-314, 314) / 100.0;
        double x0 = data.consumeInt(-1000, 1000) / 20.0;
        double step = data.consumeInt(1, 20) / 10.0;

        HarmonicOscillator oscillator = new HarmonicOscillator(amplitude, omega, phi);
        WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        WeightedObservedPoint[] snapshot = new WeightedObservedPoint[len];
        double[] xs = new double[len];
        double[] ys = new double[len];

        for (int i = 0; i < len; i++) {
            double x = x0 + i * step;
            double y = oscillator.value(x);
            xs[i] = x;
            ys[i] = y;
            WeightedObservedPoint p = new WeightedObservedPoint(1.0, x, y);
            points[i] = p;
            snapshot[i] = p;
        }

        HarmonicFitter.ParameterGuesser g;
        try {
            g = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        try {
            g.guess();
        } catch (Throwable t) {
            return;
        }

        int mutations = data.consumeInt(1, Math.max(1, len));
        for (int m = 0; m < mutations; m++) {
            int i = data.consumeInt(0, len - 1);
            int j = data.consumeInt(0, len - 1);
            WeightedObservedPoint tmp = points[i];
            points[i] = points[j];
            points[j] = tmp;
        }

        boolean violated = false;
        String detail = "";
        for (int i = 0; i < len; i++) {
            WeightedObservedPoint before = snapshot[i];
            WeightedObservedPoint after = snapshot[i];
            if (before != after) {
                violated = true;
                detail = "snapshot reference changed at index=" + i;
                break;
            }
            if (Double.doubleToLongBits(before.getX()) != Double.doubleToLongBits(xs[i]) ||
                Double.doubleToLongBits(before.getY()) != Double.doubleToLongBits(ys[i])) {
                violated = true;
                detail = "snapshot values changed at index=" + i;
                break;
            }
        }

        if (!violated) {
            for (int i = 0; i < len; i++) {
                WeightedObservedPoint original = snapshot[i];
                if (Double.doubleToLongBits(original.getX()) != Double.doubleToLongBits(xs[i]) ||
                    Double.doubleToLongBits(original.getY()) != Double.doubleToLongBits(ys[i])) {
                    violated = true;
                    detail = "caller-visible point mutated at index=" + i
                            + " expected=(" + bits(xs[i]) + "," + bits(ys[i]) + ") actual=("
                            + bits(original.getX()) + "," + bits(original.getY()) + ")";
                    break;
                }
            }
        }

        if (!violated) {
            try {
                double[] guess = g.guess();
                if (guess == null || guess.length != 3) {
                    violated = true;
                    detail = "guess result shape changed after caller-array mutation";
                }
            } catch (Throwable t) {
                return;
            }
        }

        if (violated) {
            /*
             * Sound hidden-state/post-condition check:
             * ParameterGuesser(WeightedObservedPoint[] observations) clones the caller-supplied array.
             * Therefore later guess() work is confined to internal state, and a correct implementation
             * must not mutate caller-visible observation objects or require the caller array to remain unchanged.
             * A band-aid patch that silences the MathIllegalStateException by changing data-flow or aliasing
             * the input array would violate this isolation guarantee even if top-level output sometimes looks fine.
             */
            throw new FuzzerSecurityIssueLow(
                    "[oracle:caller-array-isolation] consistency violation: " + detail);
        }
    }

    private static double finiteFromInt(int v) {
        return (double) v;
    }

    private static String bits(double d) {
        return Double.toString(d) + "/" + Long.toHexString(Double.doubleToLongBits(d));
    }
}