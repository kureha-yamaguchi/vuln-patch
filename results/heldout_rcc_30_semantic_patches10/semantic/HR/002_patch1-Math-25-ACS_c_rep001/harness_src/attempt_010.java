package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedMath844Oracle();
        checkScaledXFitCurveEquivalence(data);
    }

    private static void checkLiftedMath844Oracle() {
        double[] y = new double[] {
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
        String observed = "completed normally";
        try {
            guesser.guess();
            violated = true;
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable t) {
            violated = true;
            observed = t.getClass().getName();
        }

        if (violated) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844-lifted] semantic mismatch: expected MathIllegalStateException, got " + observed);
        }
    }

    private static void checkScaledXFitCurveEquivalence(FuzzedDataProvider data) {
        // Contract justification: HarmonicFitter fits the real-valued harmonic model a * cos(omega * x + phi).
        // For the same underlying signal, rescaling abscissae x' = s*x with s>0 yields another valid harmonic
        // model whose values satisfy f(x) == g(s*x). A correct implementation fitting exact noiseless samples
        // from these two equivalent datasets must therefore produce fitted curves that agree on corresponding
        // points. A patch that merely deletes the MathIllegalStateException path or otherwise corrupts the
        // guesser's internal state can leave top-level execution non-throwing while returning parameters whose
        // fitted curve disagrees under this exact change-of-units relation.
        int n = data.consumeInt(8, 24);
        int scaleInt = data.consumeInt(2, 8);
        double scale = (double) scaleInt;

        double amplitude = 0.25 + data.consumeInt(0, 400) / 80.0;
        double omega = 0.15 + data.consumeInt(0, 185) / 100.0;
        double phi = -Math.PI + (2.0 * Math.PI * data.consumeInt(0, 10_000) / 10_000.0);
        double step = 0.10 + data.consumeInt(0, 90) / 100.0;

        HarmonicFitter f1;
        HarmonicFitter f2;
        double[] p1;
        double[] p2;
        double[] xs = new double[n];

        try {
            f1 = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            f2 = new HarmonicFitter(new LevenbergMarquardtOptimizer());

            HarmonicOscillator generator1 = new HarmonicOscillator(amplitude, omega, phi);
            HarmonicOscillator generator2 = new HarmonicOscillator(amplitude, omega / scale, phi);

            for (int i = 0; i < n; i++) {
                double x = i * step;
                xs[i] = x;
                double y1 = generator1.value(x);
                double y2 = generator2.value(scale * x);
                f1.addObservedPoint(1.0, x, y1);
                f2.addObservedPoint(1.0, scale * x, y2);
            }

            p1 = f1.fit();
            p2 = f2.fit();
        } catch (Throwable t) {
            return;
        }

        boolean violated = false;
        String detail = "";
        try {
            HarmonicOscillator fit1 = new HarmonicOscillator(p1[0], p1[1], p1[2]);
            HarmonicOscillator fit2 = new HarmonicOscillator(p2[0], p2[1], p2[2]);

            double maxErr = 0.0;
            double maxRef = 0.0;
            double worstX = 0.0;
            double worstL = 0.0;
            double worstR = 0.0;

            for (int i = 0; i < n; i++) {
                double x = xs[i];
                double lhs = fit1.value(x);
                double rhs = fit2.value(scale * x);
                double err = Math.abs(lhs - rhs);
                double ref = Math.max(Math.abs(lhs), Math.abs(rhs));
                if (err > maxErr) {
                    maxErr = err;
                    maxRef = ref;
                    worstX = x;
                    worstL = lhs;
                    worstR = rhs;
                }
            }

            double tol = 1.0e-5 + 1.0e-3 * maxRef;
            if (maxErr > tol) {
                violated = true;
                detail =
                    "scale=" + scale +
                    " amplitude=" + amplitude +
                    " omega=" + omega +
                    " phi=" + phi +
                    " maxErr=" + maxErr +
                    " tol=" + tol +
                    " worstX=" + worstX +
                    " lhs=" + worstL +
                    " rhs=" + worstR +
                    " fit1=[" + p1[0] + "," + p1[1] + "," + p1[2] + "]" +
                    " fit2=[" + p2[0] + "," + p2[1] + "," + p2[2] + "]";
            }
        } catch (Throwable t) {
            return;
        }

        if (violated) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:x-scale-fit-curve] metamorphic violation: fitted curves disagree under positive x scaling: " + detail);
        }
    }
}