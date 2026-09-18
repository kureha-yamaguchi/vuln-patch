package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
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
        runAnchorCrash();
        runExplorationAndOracle(data);
    }

    private static void runAnchorCrash() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR.length; i++) {
            fitter.addObservedPoint(i, ANCHOR[i]);
        }
        fitter.fit();
    }

    private static void runExplorationAndOracle(FuzzedDataProvider data) {
        int prefix = data.consumeInt(0, 8);
        int suffix = data.consumeInt(0, 8);
        double xStart = bounded(data.consumeInt(), -20.0, 20.0);
        double xStep = bounded(data.consumeInt(), 0.25, 3.0);

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        int x = 0;

        for (int i = 0; i < prefix; i++, x++) {
            double y = bounded(data.consumeInt(), 0.0, 1.0e-16);
            fitter.addObservedPoint(xStart + x * xStep, y);
        }

        for (int i = 0; i < ANCHOR.length; i++, x++) {
            fitter.addObservedPoint(xStart + x * xStep, ANCHOR[i]);
        }

        for (int i = 0; i < suffix; i++, x++) {
            double y = bounded(data.consumeInt(), 0.0, 1.0e-9);
            fitter.addObservedPoint(xStart + x * xStep, y);
        }

        double[] pPublic = fitter.fit();

        WeightedObservedPoint[] obs = fitter.getObservations();
        double[] guess = new GaussianFitter.ParameterGuesser(obs).guess();
        double[] pDelegate = fitter.fit(guess);

        /*
         * Contract visible in GaussianFitter.fit(): the fixed code delegates public fit()
         * to fit(double[] guess) using the ParameterGuesser result. Therefore, for the same
         * observations, public fit() and an explicit getObservations()->ParameterGuesser->fit(guess)
         * call must produce the same fitted model on any correct implementation.
         */
        compareModels(obs, pPublic, pDelegate);
    }

    private static void compareModels(WeightedObservedPoint[] obs, double[] p1, double[] p2) {
        if (p1 == null || p2 == null || p1.length != 3 || p2.length != 3) {
            throw new RuntimeException("[oracle:public-vs-delegate-model] metamorphic violation: invalid parameter vector shape");
        }

        Gaussian.Parametric g = new Gaussian.Parametric();
        double maxY = 0.0;
        double maxDiff = 0.0;

        for (int i = 0; i < obs.length; i++) {
            double x = obs[i].getX();
            double y1 = g.value(x, p1);
            double y2 = g.value(x, p2);
            double y = Math.max(Math.abs(obs[i].getY()), Math.max(Math.abs(y1), Math.abs(y2)));
            if (y > maxY) {
                maxY = y;
            }
            double diff = Math.abs(y1 - y2);
            if (diff > maxDiff) {
                maxDiff = diff;
            }
        }

        double tol = Math.max(1.0e-8, maxY * 1.0e-6);
        if (maxDiff > tol) {
            throw new RuntimeException(
                "[oracle:public-vs-delegate-model] metamorphic violation: public fit() and fit(guess) disagree"
                    + " maxDiff=" + maxDiff
                    + " tol=" + tol
                    + " pPublic=[" + p1[0] + "," + p1[1] + "," + p1[2] + "]"
                    + " pDelegate=[" + p2[0] + "," + p2[1] + "," + p2[2] + "]"
                    + " n=" + obs.length);
        }
    }

    private static double bounded(int raw, double min, double max) {
        long v = raw & 0x7fffffffL;
        double u = v / (double) Integer.MAX_VALUE;
        return min + (max - min) * u;
    }
}