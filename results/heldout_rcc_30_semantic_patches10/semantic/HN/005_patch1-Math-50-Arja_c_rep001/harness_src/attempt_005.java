package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        data.consumeInt(0, 0);

        RegulaFalsiSolver fixedSolver = new RegulaFalsiSolver();
        UnivariateRealFunction fixedFunction = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        boolean fixedOracleViolation = false;
        Throwable fixedOracleCause = null;
        String fixedOracleMessage = null;
        try {
            double root = fixedSolver.solve(3624, fixedFunction, 1.0, 10.0);
            fixedOracleViolation = true;
            fixedOracleMessage =
                    "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but solve completed normally with root="
                            + root + " (buggy build ground-truth wrong value is 3.4341896575482003)";
        } catch (TooManyEvaluationsException expected) {
        } catch (Throwable t) {
            fixedOracleViolation = true;
            fixedOracleCause = t;
            fixedOracleMessage =
                    "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but got "
                            + t.getClass().getName();
        }
        if (fixedOracleViolation) {
            if (fixedOracleCause != null) {
                throw new FuzzerSecurityIssueLow(fixedOracleMessage, fixedOracleCause);
            }
            throw new FuzzerSecurityIssueLow(fixedOracleMessage);
        }

        RegulaFalsiSolver anySideSolver = new RegulaFalsiSolver();
        final double c = data.consumeInt(-1000, 1000);
        final int w = data.consumeInt(1, 1000);
        final boolean leftRoot = data.consumeBoolean();
        final double min = leftRoot ? c : c - w;
        final double max = leftRoot ? c + w : c;
        UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c;
            }
        };

        boolean relationViolation = false;
        Throwable relationCause = null;
        String relationMessage = null;
        double rDefault = 0.0;
        double rExplicit = 0.0;
        boolean relationApplicable = true;
        try {
            rDefault = anySideSolver.solve(100, linear, min, max);
            rExplicit = anySideSolver.solve(100, linear, min, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            relationApplicable = false;
            relationCause = t;
        }
        if (relationApplicable && Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicit)) {
            relationViolation = true;
            relationMessage =
                    "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE)"
                            + " inputC=" + c + " min=" + min + " max=" + max
                            + " lhs=" + rDefault + " rhs=" + rExplicit;
        }
        if (relationViolation) {
            if (relationCause != null) {
                throw new FuzzerSecurityIssueLow(relationMessage, relationCause);
            }
            throw new FuzzerSecurityIssueLow(relationMessage);
        }

        RegulaFalsiSolver siblingSolver = new RegulaFalsiSolver();
        final double c2 = data.consumeInt(-1000, 1000);
        final int w2 = data.consumeInt(1, 1000);
        final boolean rootAtLeft = data.consumeBoolean();
        final double min2 = rootAtLeft ? c2 : c2 - w2;
        final double max2 = rootAtLeft ? c2 + w2 : c2;
        final double start2 = rootAtLeft ? min2 : max2;
        UnivariateRealFunction linear2 = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c2;
            }
        };

        boolean siblingViolation = false;
        String siblingMessage = null;
        double rNoStart = 0.0;
        double rWithStart = 0.0;
        double rWithStartAny = 0.0;
        boolean siblingApplicable = true;
        try {
            rNoStart = siblingSolver.solve(100, linear2, min2, max2);
            rWithStart = siblingSolver.solve(100, linear2, min2, max2, start2);
            rWithStartAny = siblingSolver.solve(100, linear2, min2, max2, start2, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            siblingApplicable = false;
        }
        if (siblingApplicable) {
            if (Double.doubleToLongBits(rNoStart) != Double.doubleToLongBits(rWithStart)
                    || Double.doubleToLongBits(rNoStart) != Double.doubleToLongBits(rWithStartAny)) {
                siblingViolation = true;
                siblingMessage =
                        "[oracle:sibling-overloads] metamorphic violation: equivalent solve overloads must agree on a valid problem with exact endpoint root"
                                + " c=" + c2 + " min=" + min2 + " max=" + max2 + " start=" + start2
                                + " noStart=" + rNoStart + " withStart=" + rWithStart + " withStartAny=" + rWithStartAny;
            }
        }
        if (siblingViolation) {
            throw new FuzzerSecurityIssueLow(siblingMessage);
        }

        RegulaFalsiSolver endpointSolver = new RegulaFalsiSolver();
        final double c3 = data.consumeInt(-1000, 1000);
        final int w3 = data.consumeInt(1, 1000);
        final boolean left = data.consumeBoolean();
        final double min3 = left ? c3 : c3 - w3;
        final double max3 = left ? c3 + w3 : c3;
        final double expectedRoot = c3;
        UnivariateRealFunction linear3 = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c3;
            }
        };

        boolean postViolation = false;
        String postMessage = null;
        boolean postApplicable = true;
        double postRoot = 0.0;
        try {
            postRoot = endpointSolver.solve(100, linear3, min3, max3, AllowedSolution.LEFT_SIDE);
        } catch (Throwable t) {
            postApplicable = false;
        }
        if (postApplicable && Double.doubleToLongBits(postRoot) != Double.doubleToLongBits(expectedRoot)) {
            postViolation = true;
            postMessage =
                    "[oracle:endpoint-root] semantic mismatch: doSolve contract returns an endpoint immediately when its function value is exactly zero"
                            + " expected=" + expectedRoot + " actual=" + postRoot
                            + " min=" + min3 + " max=" + max3;
        }
        if (postViolation) {
            throw new FuzzerSecurityIssueLow(postMessage);
        }
    }
}