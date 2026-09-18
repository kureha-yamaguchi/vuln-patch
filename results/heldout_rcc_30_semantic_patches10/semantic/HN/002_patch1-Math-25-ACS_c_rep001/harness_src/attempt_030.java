package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            double w = data.consumeInt(-1000, 1000) / 10.0;
            double x = data.consumeInt(-1000, 1000) / 10.0;
            double y = data.consumeInt(-1000, 1000) / 10.0;
            WeightedObservedPoint p = new WeightedObservedPoint(w, x, y);
            if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                    || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                    || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                throw new FuzzerSecurityIssueLow(
                        "relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                                + "weight=" + p.getWeight() + "/" + w
                                + " x=" + p.getX() + "/" + x
                                + " y=" + p.getY() + "/" + y);
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        final double[] exactY = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
        };

        {
            WeightedObservedPoint[] points = new WeightedObservedPoint[exactY.length];
            for (int i = 0; i < exactY.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, i, exactY[i]);
            }

            HarmonicFitter.ParameterGuesser guesser;
            try {
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }

            int mutations = data.consumeInt(1, Math.max(1, points.length));
            for (int m = 0; m < mutations; m++) {
                int idx = data.consumeInt(0, points.length - 1);
                double mx = data.consumeInt(-100, 100);
                double my = data.consumeInt(-100, 100);
                double mw = data.consumeInt(-10, 10);
                points[idx] = new WeightedObservedPoint(mw, mx, my);
            }

            boolean violated = false;
            String detail = "completed normally";
            try {
                guesser.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                violated = true;
                detail = "wrong exception " + t.getClass().getName();
            }
            if (violated) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testMath844-parameter-guesser] semantic mismatch: expected MathIllegalStateException but "
                                + detail);
            }
        }

        {
            HarmonicFitter fitter;
            try {
                fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < exactY.length; i++) {
                    fitter.addObservedPoint(1.0, i, exactY[i]);
                }
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }

            boolean violated = false;
            String detail = "completed normally";
            try {
                fitter.fit();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                violated = true;
                detail = "wrong exception " + t.getClass().getName();
            }
            if (violated) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testMath844-harmonic-fitter-fit] semantic mismatch: expected MathIllegalStateException but "
                                + detail);
            }
        }

        {
            WeightedObservedPoint[] points;
            HarmonicFitter.ParameterGuesser guesser;
            try {
                int scale = data.consumeInt(1, 5);
                int step = data.consumeInt(1, 4);
                int start = data.consumeInt(-20, 20);
                points = new WeightedObservedPoint[exactY.length];
                for (int i = 0; i < exactY.length; i++) {
                    points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, exactY[i] * (double) scale);
                }
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }

            boolean violated = false;
            String detail = "completed normally";
            try {
                guesser.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                violated = true;
                detail = "wrong exception " + t.getClass().getName();
            }
            if (violated) {
                throw new FuzzerSecurityIssueLow(
                        "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException but "
                                + detail);
            }
        }

        {
            try {
                int count = data.consumeInt(8, 20);
                double amplitude = data.consumeInt(1, 50) / 10.0;
                double omega = data.consumeInt(1, 30) / 10.0;
                double phase = data.consumeInt(-30, 30) / 10.0;
                double start = data.consumeInt(-20, 20) / 10.0;
                double step = data.consumeInt(1, 10) / 10.0;
                HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);

                WeightedObservedPoint[] points = new WeightedObservedPoint[count];
                HarmonicFitter.ParameterGuesser guesser = null;
                try {
                    for (int i = 0; i < count; i++) {
                        double px = start + i * step;
                        points[i] = new WeightedObservedPoint(1.0, px, osc.value(px));
                    }
                    guesser = new HarmonicFitter.ParameterGuesser(points);
                } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                    return;
                }

                double[] g1;
                double[] g2;
                try {
                    g1 = guesser.guess();
                    g2 = guesser.guess();
                } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                    return;
                }

                // The constructor clones observations and guess() only estimates coefficients
                // from that fixed sample; calling guess() twice on the same instance should
                // therefore be stable. A patch that silently corrupts shared state instead of
                // rejecting bad data would violate this read-only/idempotent observable.
                boolean same = g1 != null && g2 != null && g1.length == g2.length;
                if (same) {
                    for (int i = 0; i < g1.length; i++) {
                        if (Double.doubleToLongBits(g1[i]) != Double.doubleToLongBits(g2[i])) {
                            same = false;
                            break;
                        }
                    }
                }
                if (!same) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:guess-idempotence] metamorphic violation: repeated guess() changed result "
                                    + "first=[" + arrayToString(g1) + "] second=[" + arrayToString(g2) + "]");
                }

                HarmonicFitter fitter1;
                HarmonicFitter fitter2;
                double[] fitNoArg;
                double[] fitWithGuess;
                try {
                    fitter1 = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                    fitter2 = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                    for (int i = 0; i < count; i++) {
                        fitter1.addObservedPoint(points[i]);
                        fitter2.addObservedPoint(points[i]);
                    }
                    fitNoArg = fitter1.fit();
                    fitWithGuess = fitter2.fit(g1);
                } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                    return;
                }

                // The two fit overloads are documented to perform the same fitting job; on the
                // same exact harmonic sample, using fit() or fit(initialGuess) where the initial
                // guess comes from the library's own ParameterGuesser must agree up to numerical
                // tolerance. This exercises the patched path through the public HarmonicFitter API.
                if (!closeArrays(fitNoArg, fitWithGuess, 1e-6)) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:fit-overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagree "
                                    + "fit()=[" + arrayToString(fitNoArg) + "] fit(guess)=[" + arrayToString(fitWithGuess) + "]");
                }
            } catch (FuzzerSecurityIssueLow e) {
                throw e;
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }
        }
    }

    private static boolean closeArrays(double[] a, double[] b, double tol) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double da = a[i];
            double db = b[i];
            if (Double.isNaN(da) || Double.isNaN(db) || Double.isInfinite(da) || Double.isInfinite(db)) {
                return false;
            }
            if (Math.abs(da - db) > tol) {
                return false;
            }
        }
        return true;
    }

    private static String arrayToString(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        return sb.toString();
    }
}