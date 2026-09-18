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
        if (data.consumeBoolean()) {
            runFit(buildExactAnchor());
            return;
        }

        double[] scenario = buildScenario(data);
        runFit(scenario);
    }

    private static void runFit(double[] values) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < values.length; i++) {
            fitter.addObservedPoint((double) i, values[i]);
        }
        fitter.fit();
    }

    private static double[] buildExactAnchor() {
        double[] copy = new double[ANCHOR.length];
        System.arraycopy(ANCHOR, 0, copy, 0, ANCHOR.length);
        return copy;
    }

    private static double[] buildScenario(FuzzedDataProvider data) {
        int mode = data.consumeInt(0, 4);
        int len = data.consumeInt(20, ANCHOR.length);
        double[] out = new double[len];

        switch (mode) {
            case 0:
                for (int i = 0; i < len; i++) {
                    out[i] = ANCHOR[i];
                }
                break;

            case 1:
                for (int i = 0; i < len; i++) {
                    double scale = 1.0 + (data.consumeInt(-3, 3) * 0.01);
                    out[i] = positive(ANCHOR[i] * scale);
                }
                break;

            case 2:
                int start = data.consumeInt(0, ANCHOR.length - len);
                for (int i = 0; i < len; i++) {
                    double scale = 1.0 + (data.consumeInt(-5, 5) * 0.02);
                    out[i] = positive(ANCHOR[start + i] * scale);
                }
                break;

            case 3:
                for (int i = 0; i < len; i++) {
                    int src = Math.min(ANCHOR.length - 1, i + data.consumeInt(0, 1));
                    double scale = 1.0 + (data.consumeInt(-2, 2) * 0.01);
                    out[i] = positive(ANCHOR[src] * scale);
                }
                break;

            default:
                for (int i = 0; i < len; i++) {
                    out[i] = ANCHOR[i];
                }
                int idx = data.consumeInt(0, len - 1);
                out[idx] = positive(out[idx] * (1.0 + data.consumeInt(-10, 10) * 0.01));
                break;
        }

        return out;
    }

    private static double positive(double v) {
        if (v > 0.0 && !Double.isNaN(v) && !Double.isInfinite(v)) {
            return v;
        }
        return Double.MIN_VALUE;
    }
}