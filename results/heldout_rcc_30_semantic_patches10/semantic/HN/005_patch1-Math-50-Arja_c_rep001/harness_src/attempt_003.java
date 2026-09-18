package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    private static String safe(Throwable t) {
        return t == null ? "null" : String.valueOf(t.getMessage());
    }

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final RegulaFalsiSolver issue631Solver = new RegulaFalsiSolver();
        boolean issue631Violated = false;
        String issue631Message = null;
        Throwable issue631Cause = null;
        try {
            final double root = issue631Solver.solve(3624 + data.consumeInt(0, 0), issue631Function, 1.0, 10.0);
            issue631Violated = true;
            issue631Message =
                "expected org.apache.commons.math.exception.TooManyEvaluationsException but completed normally with root=" +
                root + " pinnedBuggyRootDelta=" + Math.abs(root - 3.4341896575482003);
        } catch (TooManyEvaluationsException expected) {
            // Trusted oracle lifted verbatim from RegulaFalsiSolverTest.testIssue631:
            // on this exact problem, solve(3624, f, 1, 10) must throw TooManyEvaluationsException.
        } catch (Throwable t) {
            issue631Violated = true;
            issue631Cause = t;
            issue631Message =
                "expected org.apache.commons.math.exception.TooManyEvaluationsException but threw " +
                t.getClass().getName() + ": " + safe(t);
        }
        if (issue631Violated) {
            if (issue631Cause != null) {
                throw new FuzzerSecurityIssueLow("[oracle:issue631] semantic mismatch: " + issue631Message, issue631Cause);
            }
            throw new FuzzerSecurityIssueLow("[oracle:issue631] semantic mismatch: " + issue631Message);
        }

        final double c = (double) data.consumeInt(-1000, 1000);
        final int w = data.consumeInt(1, 1000);
        final boolean leftRoot = data.consumeBoolean();
        final double min = leftRoot ? c : c - w;
        final double max = leftRoot ? c + w : c;
        final double startValue = min + ((max - min) / 2.0);

        final UnivariateRealFunction linearAtEndpoint = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c;
            }
        };

        final RegulaFalsiSolver relationSolver = new RegulaFalsiSolver();
        boolean relation1Ready = false;
        boolean relation1Violated = false;
        String relation1Message = null;
        Throwable relation1Cause = null;
        double relation1Left = 0.0;
        double relation1Right = 0.0;
        try {
            relation1Left = relationSolver.solve(100, linearAtEndpoint, min, max);
            relation1Right = relationSolver.solve(100, linearAtEndpoint, min, max, AllowedSolution.ANY_SIDE);
            relation1Ready = true;
        } catch (Throwable t) {
            relation1Cause = t;
        }
        if (relation1Cause == null && relation1Ready &&
            Double.doubleToLongBits(relation1Left) != Double.doubleToLongBits(relation1Right)) {
            relation1Violated = true;
            relation1Message =
                "solve(maxEval,f,min,max)=" + relation1Left +
                " vs solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE)=" + relation1Right +
                " input[min=" + min + ",max=" + max + ",c=" + c + "]";
        }
        if (relation1Violated) {
            throw new FuzzerSecurityIssueLow("[oracle:default-any-side] metamorphic violation: " + relation1Message);
        }

        final RegulaFalsiSolver relationSolver2 = new RegulaFalsiSolver();
        boolean relation2Ready = false;
        boolean relation2Violated = false;
        String relation2Message = null;
        Throwable relation2Cause = null;
        double relation2Left = 0.0;
        double relation2Right = 0.0;
        try {
            relation2Left = relationSolver2.solve(100, linearAtEndpoint, min, max, AllowedSolution.ANY_SIDE);
            relation2Right = relationSolver2.solve(100, linearAtEndpoint, min, max, startValue, AllowedSolution.ANY_SIDE);
            relation2Ready = true;
        } catch (Throwable t) {
            relation2Cause = t;
        }
        if (relation2Cause == null && relation2Ready &&
            Double.doubleToLongBits(relation2Left) != Double.doubleToLongBits(relation2Right)) {
            relation2Violated = true;
            relation2Message =
                "explicit ANY_SIDE overloads disagree: withoutStart=" + relation2Left +
                " withStart=" + relation2Right +
                " input[min=" + min + ",max=" + max + ",start=" + startValue + ",c=" + c + "]";
        }
        if (relation2Violated) {
            throw new FuzzerSecurityIssueLow("[oracle:overload-agreement] metamorphic violation: " + relation2Message);
        }

        final RegulaFalsiSolver endpointSolver = new RegulaFalsiSolver();
        boolean endpointReady = false;
        boolean endpointViolated = false;
        String endpointMessage = null;
        Throwable endpointCause = null;
        double endpointRoot = 0.0;
        try {
            endpointRoot = endpointSolver.solve(100, linearAtEndpoint, min, max, AllowedSolution.ANY_SIDE);
            endpointReady = true;
        } catch (Throwable t) {
            endpointCause = t;
        }
        // Contract from doSolve shown above: if one bound is an exact root, it returns that bound immediately.
        // This post-condition remains observable through the public solve API; a patch that only suppresses
        // iteration/throw behavior but corrupts solver state or allowed-solution handling would break it.
        if (endpointCause == null && endpointReady &&
            Double.doubleToLongBits(endpointRoot) != Double.doubleToLongBits(c)) {
            endpointViolated = true;
            endpointMessage =
                "endpoint exact-root contract violated: expected " + c + " but got " + endpointRoot +
                " input[min=" + min + ",max=" + max + "]";
        }
        if (endpointViolated) {
            throw new FuzzerSecurityIssueLow("[oracle:endpoint-root] semantic mismatch: " + endpointMessage);
        }
    }
}