package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.ZeroException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] MATH844_Y = {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        WeightedObservedPoint[] exactSeed = buildPoints(MATH844_Y);
        exercisePublicFit(exactSeed);

        int idx = data.consumeInt(1, MATH844_Y.length - 2);
        int delta = data.consumeInt(-2, 2);
        if (delta == 0) {
            delta = 1;
        }

        double[] nearby = MATH844_Y.clone();
        nearby[idx] += delta;
        WeightedObservedPoint[] nearbyPoints = buildPoints(nearby);
        exercisePublicFit(nearbyPoints);

        checkStrictlyPositiveOmegaIfGuessReturns("seed-boundary", exactSeed);
        checkStrictlyPositiveOmegaIfGuessReturns("nearby-boundary", nearbyPoints);
    }

    private static WeightedObservedPoint[] buildPoints(double[] y) {
        WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }
        return points;
    }

    private static void exercisePublicFit(WeightedObservedPoint[] points) {
        try {
            HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < points.length; i++) {
                fitter.addObservedPoint(points[i]);
            }
            fitter.fit();
        } catch (MathIllegalStateException e) {
        } catch (ZeroException e) {
        } catch (NumberIsTooSmallException e) {
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
        }
    }

    private static void checkStrictlyPositiveOmegaIfGuessReturns(String oracleId, WeightedObservedPoint[] points) {
        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            double[] guess = guesser.guess();

            double omega = guess[1];
            double xRange = points[points.length - 1].getX() - points[0].getX();

            /*
             * Sound post-condition from the real implementation:
             * - For xRange > 0, the fallback branch sets omega = 2 * PI / xRange, which is strictly positive.
             * - The non-fallback branch sets omega = sqrt(c2 / c3). If guess() returns normally in a correct build,
             *   the patched c2 == 0 case is not allowed to slip through, so omega must also be strictly positive.
             * Therefore, for these non-degenerate observations (strictly increasing x, positive xRange),
             * any normal return with omega <= 0 indicates the same root cause was masked instead of fixed.
             */
            if (!(xRange > 0.0)) {
                return;
            }
            if (!(omega > 0.0)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] post-condition violation: guess() returned non-positive omega"
                        + " omega=" + omega
                        + " xRange=" + xRange
                        + " firstX=" + points[0].getX()
                        + " lastX=" + points[points.length - 1].getX()
                        + " amplitude=" + guess[0]
                        + " phase=" + guess[2]);
            }
        } catch (MathIllegalStateException e) {
            return;
        } catch (ZeroException e) {
            return;
        } catch (NumberIsTooSmallException e) {
            return;
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }
    }
}