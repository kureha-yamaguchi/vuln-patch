package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double[] ANCHOR = {
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
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter =
            new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR.length; i++) {
            fitter.addObservedPoint(i, ANCHOR[i]);
        }
        fitter.fit();
    }

    private static void runExplore(FuzzedDataProvider data) {
        int prefix = data.consumeInt(0, 4);
        int suffix = data.consumeInt(0, 4);
        int start = data.consumeInt(0, ANCHOR.length - 3);
        int len = data.consumeInt(3, ANCHOR.length - start);

        double xOffset = data.consumeInt(-100, 100);
        double xStep = 0.5 + (data.consumeInt(0, 200) / 100.0);
        double yScale = 0.5 + (data.consumeInt(0, 400) / 100.0);

        double minPositive = ANCHOR[0];
        for (int i = 1; i < ANCHOR.length; i++) {
            if (ANCHOR[i] > 0.0 && ANCHOR[i] < minPositive) {
                minPositive = ANCHOR[i];
            }
        }

        GaussianFitter fitter =
            new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        int index = 0;
        for (int i = 0; i < prefix; i++) {
            fitter.addObservedPoint(xOffset + index * xStep, minPositive * yScale * (1.0 + i));
            index++;
        }
        for (int i = 0; i < len; i++) {
            fitter.addObservedPoint(xOffset + index * xStep, ANCHOR[start + i] * yScale);
            index++;
        }
        for (int i = 0; i < suffix; i++) {
            fitter.addObservedPoint(xOffset + index * xStep, minPositive * yScale * (1.0 + i));
            index++;
        }

        fitter.fit();
    }
}