package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        RegulaFalsiSolver issue631Solver = new RegulaFalsiSolver();
        boolean violated = false;
        String violationMessage = "completed normally";
        try {
            double root = issue631Solver.solve(3624 + data.consumeInt(0, 0), issue631Function, 1.0, 10.0);
            violated = true;
            violationMessage = "completed normally with root=" + root + " expectedException=" + TooManyEvaluationsException.class.getName() + " pinnedBuggyRoot=3.4341896575482003";
        } catch (TooManyEvaluationsException expected) {
            // Correct behavior pinned by the failing test.
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:issue631-throw] semantic mismatch: expected TooManyEvaluationsException", t);
        }
        if (violated) {
            throw new FuzzerSecurityIssueLow("[oracle:issue631-throw] semantic mismatch: " + violationMessage);
        }

        /* Documented backwards-compatibility guarantee: default allowed solution is ANY_SIDE,
           so these sibling overloads must agree on the same valid-by-construction problem. */
        RegulaFalsiSolver solver1 = new RegulaFalsiSolver();
        double c1 = data.consumeInt(-1000, 1000);
        int w1 = data.consumeInt(1, 1000);
        boolean leftRoot1 = data.consumeBoolean();
        double min1 = leftRoot1 ? c1 : c1 - w1;
        double max1 = leftRoot1 ? c1 + w1 : c1;
        final double endpointRoot1 = c1;
        UnivariateRealFunction f1 = new UnivariateRealFunction() {
            public double value(double x) {
                return x - endpointRoot1;
            }
        };
        double rDefault;
        double rAnySide;
        try {
            rDefault = solver1.solve(100, f1, min1, max1);
            rAnySide = solver1.solve(100, f1, min1, max1, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rAnySide)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-any-side-agreement] metamorphic violation: min=" + min1 +
                " max=" + max1 + " c=" + c1 + " lhs=" + rDefault + " rhs=" + rAnySide);
        }

        /* Overloads with and without startValue solve the same problem; for a unique endpoint root
           on f(x)=x-c, both real API calls must return that exact root. */
        RegulaFalsiSolver solver2 = new RegulaFalsiSolver();
        double c2 = data.consumeInt(-1000, 1000);
        int w2 = data.consumeInt(1, 1000);
        boolean leftRoot2 = data.consumeBoolean();
        double min2 = leftRoot2 ? c2 : c2 - w2;
        double max2 = leftRoot2 ? c2 + w2 : c2;
        double start2 = min2 + (max2 - min2) / 2.0;
        final double endpointRoot2 = c2;
        UnivariateRealFunction f2 = new UnivariateRealFunction() {
            public double value(double x) {
                return x - endpointRoot2;
            }
        };
        double rNoStart;
        double rWithStart;
        try {
            rNoStart = solver2.solve(100, f2, min2, max2);
            rWithStart = solver2.solve(100, f2, min2, max2, start2);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(rNoStart) != Double.doubleToLongBits(rWithStart)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:start-overload-agreement] metamorphic violation: min=" + min2 +
                " max=" + max2 + " start=" + start2 + " c=" + c2 +
                " lhs=" + rNoStart + " rhs=" + rWithStart);
        }

        /* Post-condition from the solver contract and method body: if one endpoint is an exact root,
           solve returns that endpoint immediately. This observable result would be broken by a patch
           that merely avoids the problematic path by returning some other value. */
        RegulaFalsiSolver solver3 = new RegulaFalsiSolver();
        double c3 = data.consumeInt(-1000, 1000);
        int w3 = data.consumeInt(1, 1000);
        boolean leftRoot3 = data.consumeBoolean();
        double min3 = leftRoot3 ? c3 : c3 - w3;
        double max3 = leftRoot3 ? c3 + w3 : c3;
        final double endpointRoot3 = c3;
        UnivariateRealFunction f3 = new UnivariateRealFunction() {
            public double value(double x) {
                return x - endpointRoot3;
            }
        };
        double expected3 = leftRoot3 ? min3 : max3;
        double actual3;
        try {
            actual3 = solver3.solve(100, f3, min3, max3, startValueInside(min3, max3), AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(actual3) != Double.doubleToLongBits(expected3)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:endpoint-root] semantic mismatch: min=" + min3 +
                " max=" + max3 + " expected=" + expected3 + " actual=" + actual3);
        }
    }

    private static double startValueInside(double min, double max) {
        return min + (max - min) / 2.0;
    }
}