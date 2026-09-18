package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();

            double mean = bounded(data.consumeInt(), 100.0);
            double sd = positive(data.consumeInt());

            NormalDistributionImpl dist = new NormalDistributionImpl(mean, sd);

            // Root-cause property from the diff:
            // bracket() is buggy when one endpoint is an exact root, i.e. fa * fb == 0.
            // For NormalDistributionImpl.inverseCumulativeProbability, choosing
            // p = CDF(mean + 2*sd) makes the solver search for x = mean + 2*sd,
            // and on the buggy build the bracketing phase can land on an endpoint
            // with exact zero and wrongly throw.
            double x = mean + 2.0 * sd;
            double p = dist.cumulativeProbability(x);
            double recovered = dist.inverseCumulativeProbability(p);

            // Post-condition / metamorphic check:
            // For a correct inverse CDF implementation, inverseCumulativeProbability(CDF(x)) == x
            // for this valid, constructed x and p.
            if (Math.abs(recovered - x) > 1.0e-12) {
                throw new RuntimeException(
                    "[oracle:inv-cdf-roundtrip] metamorphic violation: x=" + x +
                    " p=" + p +
                    " recovered=" + recovered +
                    " mean=" + mean +
                    " sd=" + sd);
            }
        } catch (MathException e) {
            throw new RuntimeException(e);
        }
    }

    private static void runAnchor() throws MathException {
        NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
        double result = normal.inverseCumulativeProbability(0.9772498680518209d);
        if (Math.abs(result - 2.0d) > 1.0e-12d) {
            throw new RuntimeException(
                "[oracle:anchor-exact] metamorphic violation: expected=2.0 actual=" + result);
        }
    }

    private static double bounded(int raw, double limit) {
        return (raw % 200001) * (limit / 100000.0) - limit;
    }

    private static double positive(int raw) {
        int v = raw % 100000;
        if (v < 0) {
            v = -v;
        }
        return 0.1 + (v / 100000.0) * 50.0;
    }
}