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

        if (data.remainingBytes() > 0) {
            runSeedLikeVariant(data);
            runExactGaussianOracle(data);
        }
    }

    private static void runAnchorCrash() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR.length; i++) {
            fitter.addObservedPoint(i, ANCHOR[i]);
        }
        fitter.fit();
    }

    private static void runSeedLikeVariant(FuzzedDataProvider data) {
        int n = data.consumeInt(12, 40);
        int center = data.consumeInt(0, n - 1);
        double peak = pow10(data.consumeInt(-12, 3));
        double leftRate = 0.3 + (data.consumeInt(0, 25) / 10.0);
        double rightRate = 0.3 + (data.consumeInt(0, 25) / 10.0);
        double floor = peak * pow10(-data.consumeInt(10, 30));
        double xOffset = data.consumeInt(-5, 5);

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < n; i++) {
            int d = Math.abs(i - center);
            double rate = (i <= center) ? leftRate : rightRate;
            double y = floor + peak * Math.exp(-rate * d);
            fitter.addObservedPoint(xOffset + i, y);
        }
        fitter.fit();
    }

    private static void runExactGaussianOracle(FuzzedDataProvider data) {
        int radius = data.consumeInt(3, 8);
        double mean = data.consumeInt(-20, 20);
        double sigma = 0.5 + data.consumeInt(1, 12) / 4.0;
        double norm = 1.0 + data.consumeInt(0, 1000);

        Gaussian.Parametric g = new Gaussian.Parametric();
        double[] expected = new double[] { norm, mean, sigma };

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = -radius; i <= radius; i++) {
            double x = mean + i;
            double y;
            try {
                y = g.value(x, expected);
            } catch (RuntimeException ex) {
                return;
            }
            fitter.addObservedPoint(x, y);
        }

        final double[] fitted;
        try {
            fitted = fitter.fit();
        } catch (RuntimeException ex) {
            return;
        }

        double reconstructed;
        try {
            reconstructed = g.value(mean, fitted);
        } catch (RuntimeException ex) {
            return;
        }

        double tol = Math.max(1e-6, norm * 0.05);
        if (Math.abs(reconstructed - norm) > tol) {
            throw new RuntimeException(
                "[oracle:center-sample] metamorphic violation: exact Gaussian center sample not preserved"
                    + " norm=" + norm
                    + " mean=" + mean
                    + " sigma=" + sigma
                    + " reconstructed=" + reconstructed
                    + " p0=" + fitted[0]
                    + " p1=" + fitted[1]
                    + " p2=" + fitted[2]);
        }
    }

    private static double pow10(int e) {
        return Math.pow(10.0, e);
    }
}