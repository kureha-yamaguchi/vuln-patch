package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR = new double[] {
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
        runFit(ANCHOR);

        double[] variant = buildVariant(data);
        runFit(variant);
    }

    private static void runFit(double[] y) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(i, y[i]);
        }
        fitter.fit();
    }

    private static double[] buildVariant(FuzzedDataProvider data) {
        int n = ANCHOR.length;
        double[] out = new double[n];

        double scale = Math.pow(10.0, data.consumeInt(-3, 3));
        double floor = Math.pow(10.0, -30 + data.consumeInt(0, 12));

        int peakShift = data.consumeInt(-2, 2);
        int stretchPct = data.consumeInt(80, 125);

        for (int i = 0; i < n; i++) {
            int src = i - peakShift;
            if (src < 0) {
                src = 0;
            } else if (src >= n) {
                src = n - 1;
            }

            int stretched = (src * stretchPct) / 100;
            if (stretched < 0) {
                stretched = 0;
            } else if (stretched >= n) {
                stretched = n - 1;
            }

            double v = ANCHOR[stretched] * scale;
            int ppm = data.consumeInt(-20000, 20000);
            v *= 1.0 + (ppm / 1000000.0);
            if (v <= 0.0 || Double.isNaN(v) || Double.isInfinite(v)) {
                v = floor;
            }
            out[i] = v + floor;
        }

        if (data.consumeBoolean()) {
            for (int i = 1; i < n; i++) {
                if (out[i] < out[i - 1] * 0.9) {
                    out[i] = out[i - 1] * 0.9 + floor;
                }
            }
        }

        return out;
    }
}