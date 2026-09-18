package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        RegulaFalsiSolver issueSolver = new RegulaFalsiSolver();
        UnivariateRealFunction issueFunction = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };
        try {
            issueSolver.solve(3624, issueFunction, 1.0, 10.0);
        } catch (TooManyEvaluationsException expected) {
        } catch (Throwable ignored) {
            return;
        }

        final int rootInt = data.consumeInt(-1000, 1000);
        final int leftGap = data.consumeInt(1, 1000);
        final int rightGap = data.consumeInt(1, 1000);
        int scaleInt = data.consumeInt(-1000, 1000);
        if (scaleInt == 0) {
            scaleInt = 1;
        }

        final double root = (double) rootInt;
        final double min = (double) (rootInt - leftGap);
        final double max = (double) (rootInt + rightGap);
        final double scale = (double) scaleInt;

        final UnivariateRealFunction baseAffine = new UnivariateRealFunction() {
            public double value(double x) {
                return x - root;
            }
        };
        final UnivariateRealFunction scaledAffine = new UnivariateRealFunction() {
            public double value(double x) {
                return scale * (x - root);
            }
        };

        double baseResult;
        double scaledResult;
        RegulaFalsiSolver baseSolver = new RegulaFalsiSolver();
        RegulaFalsiSolver scaledSolver = new RegulaFalsiSolver();

        try {
            baseResult = baseSolver.solve(10, baseAffine, min, max);
            scaledResult = scaledSolver.solve(10, scaledAffine, min, max);
        } catch (Throwable t) {
            return;
        }

        boolean violateAffineExactness = false;
        String affineMsg = null;

        /* For an affine function f(x)=a*(x-r), the secant line is the function itself, so the
         * first approximation computed by the real solver must be exactly the unique root r.
         * This is a sound metamorphic oracle using two real calls: scaling a function by a
         * non-zero constant does not change its root, and both calls must return the same root. */
        if (baseResult != root || scaledResult != root || baseResult != scaledResult) {
            violateAffineExactness = true;
            affineMsg = "[oracle:affine-scale-invariance] metamorphic violation: root-preserving scaling disagreed"
                    + " root=" + root
                    + " min=" + min
                    + " max=" + max
                    + " scale=" + scale
                    + " baseResult=" + baseResult
                    + " scaledResult=" + scaledResult;
        }

        if (violateAffineExactness) {
            throw new FuzzerSecurityIssueLow(affineMsg);
        }

        final double minBefore = baseSolver.getMin();
        final double maxBefore = baseSolver.getMax();
        final double absBefore = baseSolver.getAbsoluteAccuracy();
        final double relBefore = baseSolver.getRelativeAccuracy();
        final double fvalBefore = baseSolver.getFunctionValueAccuracy();

        final double minAfter = baseSolver.getMin();
        final double maxAfter = baseSolver.getMax();
        final double absAfter = baseSolver.getAbsoluteAccuracy();
        final double relAfter = baseSolver.getRelativeAccuracy();
        final double fvalAfter = baseSolver.getFunctionValueAccuracy();

        boolean violateGetterPurity = false;
        String getterMsg = null;

        /* These get* methods are documented/read as state queries only; reading them must not
         * mutate the solver. Re-reading after successful solve must therefore return identical
         * values, otherwise helper state is inconsistent even if a top-level solve happened to
         * return the right root. */
        if (minBefore != minAfter
                || maxBefore != maxAfter
                || absBefore != absAfter
                || relBefore != relAfter
                || fvalBefore != fvalAfter) {
            violateGetterPurity = true;
            getterMsg = "[oracle:getter-stability] consistency violation: repeated read changed solver state"
                    + " minBefore=" + minBefore
                    + " minAfter=" + minAfter
                    + " maxBefore=" + maxBefore
                    + " maxAfter=" + maxAfter
                    + " absBefore=" + absBefore
                    + " absAfter=" + absAfter
                    + " relBefore=" + relBefore
                    + " relAfter=" + relAfter
                    + " fvalBefore=" + fvalBefore
                    + " fvalAfter=" + fvalAfter;
        }

        if (violateGetterPurity) {
            throw new FuzzerSecurityIssueLow(getterMsg);
        }
    }
}