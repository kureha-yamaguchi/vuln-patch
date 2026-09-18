package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_Y = {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter anchor = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_Y.length; i++) {
            anchor.addObservedPoint(i, ANCHOR_Y[i]);
        }

        // Anchor: this exact public API call reproduces the known buggy failure.
        // On the fixed build it should behave like fit(ParameterGuesser(...).guess()) instead.
        anchor.fit();

        // Explore the same root-cause property with real inputs:
        // positive observed data shaped like the regression case, but with varied x scale/offset
        // and mild positive rescaling of y, then compare the documented same-name overloads.
        int copies = data.consumeInt(1, 3);
        double xScale = 0.25 + (data.consumeInt(0, 400) / 100.0);
        double xOffset = data.consumeInt(-200, 200);
        double yScale = 0.5 + (data.consumeInt(0, 300) / 100.0);
        double baseline = data.consumeInt(0, 1000) * 1e-18;

        int trimLeft = data.consumeInt(0, 2);
        int trimRight = data.consumeInt(0, 2);
        int start = trimLeft;
        int end = ANCHOR_Y.length - trimRight;
        if (end - start < 3) {
            start = 0;
            end = ANCHOR_Y.length;
        }

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int idx = 0;
        for (int c = 0; c < copies; c++) {
            for (int i = start; i < end; i++) {
                double x = xOffset + idx * xScale;
                double y = ANCHOR_Y[i] * yScale + baseline;
                fitterNoArg.addObservedPoint(x, y);
                fitterWithGuess.addObservedPoint(x, y);
                idx++;
            }
        }

        double[] guessed;
        try {
            guessed = new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations()).guess();
        } catch (RuntimeException e) {
            return;
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitterNoArg.fit();
        } catch (RuntimeException e) {
            return;
        }

        double[] viaGuess;
        try {
            viaGuess = fitterWithGuess.fit(guessed);
        } catch (RuntimeException e) {
            return;
        }

        // Contract/oracle: fit() and fit(double[] initialGuess) are same-name overloads for the
        // same fitting operation. In the fixed code, fit() delegates to fit(guessedInitialGuess).
        // Therefore, when we supply exactly the same guessed initial parameters explicitly, both
        // calls must produce the same result on any correct implementation.
        if (!sameArray(viaNoArg, viaGuess)) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: fit() != fit(guessedInitialGuess)"
                    + " start=" + start
                    + " end=" + end
                    + " copies=" + copies
                    + " xScale=" + xScale
                    + " xOffset=" + xOffset
                    + " yScale=" + yScale
                    + " baseline=" + baseline
                    + " lhs=" + format(viaNoArg)
                    + " rhs=" + format(viaGuess)
            );
        }
    }

    private static boolean sameArray(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) || Double.isNaN(b[i]) || Double.isInfinite(a[i]) || Double.isInfinite(b[i])) {
                return false;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(a[i]), Math.abs(b[i])));
            if (Math.abs(a[i] - b[i]) > 1e-8 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String format(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}