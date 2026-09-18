package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        double absAcc = positiveAccuracy(data);
        double relAcc = positiveAccuracy(data);
        double fAcc = positiveAccuracy(data);

        RegulaFalsiSolver configured = new RegulaFalsiSolver(relAcc, absAcc, fAcc);

        // Writer/reader consistency: constructors store these accuracies and the public getters
        // must report the same values for every correct implementation.
        if (Double.doubleToLongBits(configured.getAbsoluteAccuracy()) != Double.doubleToLongBits(absAcc)) {
            throw new FuzzerSecurityIssueLow("[oracle:accuracy-abs] semantic mismatch: expected=" + absAcc
                    + " actual=" + configured.getAbsoluteAccuracy());
        }
        if (Double.doubleToLongBits(configured.getRelativeAccuracy()) != Double.doubleToLongBits(relAcc)) {
            throw new FuzzerSecurityIssueLow("[oracle:accuracy-rel] semantic mismatch: expected=" + relAcc
                    + " actual=" + configured.getRelativeAccuracy());
        }
        if (Double.doubleToLongBits(configured.getFunctionValueAccuracy()) != Double.doubleToLongBits(fAcc)) {
            throw new FuzzerSecurityIssueLow("[oracle:accuracy-fval] semantic mismatch: expected=" + fAcc
                    + " actual=" + configured.getFunctionValueAccuracy());
        }

        final double knownRoot = boundedRoot(data);
        final double c = Math.exp(knownRoot);
        final UnivariateRealFunction monotoneIncreasing = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - c;
            }
        };
        final double min = knownRoot - 1.0;
        final double max = knownRoot + 1.0;

        // AllowedSolution contract: for a monotone increasing function with exact root known
        // by construction, BELOW_SIDE must not return a point above the true root and
        // ABOVE_SIDE must not return a point below it. If either call rejects, the check
        // does not apply for that input and we skip it.
        try {
            double below = new RegulaFalsiSolver(relAcc, absAcc, fAcc).solve(64, monotoneIncreasing, min, max,
                    AllowedSolution.BELOW_SIDE);
            if (below > knownRoot) {
                throw new RuntimeException("[oracle:allowed-below] metamorphic violation: BELOW_SIDE returned above true root inputRoot="
                        + knownRoot + " result=" + below);
            }
        } catch (Throwable ignored) {
        }

        try {
            double above = new RegulaFalsiSolver(relAcc, absAcc, fAcc).solve(64, monotoneIncreasing, min, max,
                    AllowedSolution.ABOVE_SIDE);
            if (above < knownRoot) {
                throw new RuntimeException("[oracle:allowed-above] metamorphic violation: ABOVE_SIDE returned below true root inputRoot="
                        + knownRoot + " result=" + above);
            }
        } catch (Throwable ignored) {
        }

        final UnivariateRealFunction issue631 = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        // Faithful lift of the regression test's real API call. On the patched build this throws
        // TooManyEvaluationsException; on the buggy build it returns instead.
        try {
            new RegulaFalsiSolver().solve(3624, issue631, 1.0, 10.0);
            throw new FuzzerSecurityIssueLow("[oracle:issue631-seed] semantic mismatch: expected TooManyEvaluationsException for maxEval=3624");
        } catch (TooManyEvaluationsException expected) {
        }

        // Flip the boundary around the patched condition: values just around the seed's maxEval
        // exercise nearly identical search states where an overfit fix can diverge.
        int delta = data.consumeInt(-2, 2);
        int maxEval = 3624 + delta;
        if (maxEval > 0) {
            try {
                new RegulaFalsiSolver().solve(maxEval, issue631, 1.0, 10.0);
                if (maxEval <= 3624) {
                    throw new RuntimeException("[oracle:maxeval-boundary] metamorphic violation: solve unexpectedly succeeded near issue631 boundary maxEval="
                            + maxEval);
                }
            } catch (TooManyEvaluationsException expected) {
            } catch (Throwable ignored) {
            }
        }
    }

    private static double positiveAccuracy(FuzzedDataProvider data) {
        int raw = data.consumeInt(1, 1_000_000);
        return raw / 1_000_000.0;
    }

    private static double boundedRoot(FuzzedDataProvider data) {
        int milli = data.consumeInt(-5000, 5000);
        return milli / 1000.0;
    }
}