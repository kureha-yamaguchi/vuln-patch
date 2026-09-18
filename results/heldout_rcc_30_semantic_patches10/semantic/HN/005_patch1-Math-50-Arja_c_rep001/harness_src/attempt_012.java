package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Lifted oracle from RegulaFalsiSolverTest.testIssue631:
        // For this exact setup, BracketedUnivariateRealSolver.solve(...) must throw
        // TooManyEvaluationsException when maxEval is exceeded. The buggy build instead
        // returns normally with the specific wrong root value from the failing test.
        UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        RegulaFalsiSolver issue631Solver = new RegulaFalsiSolver();
        boolean issue631Violated = false;
        String issue631Message = "completed normally";
        try {
            double root = issue631Solver.solve(3624 + data.consumeInt(0, 0), issue631Function, 1.0, 10.0);
            issue631Violated = true;
            issue631Message = "completed normally with root=" + root;
            if (Double.doubleToLongBits(root) == Double.doubleToLongBits(3.4341896575482003d)) {
                issue631Message += " (matches buggy-build ground-truth wrong value)";
            }
        } catch (TooManyEvaluationsException expected) {
            // expected
        } catch (Throwable t) {
            issue631Violated = true;
            issue631Message = "threw wrong exception " + t.getClass().getName() + ": " + String.valueOf(t.getMessage());
        }
        if (issue631Violated) {
            throw new FuzzerSecurityIssueLow("[oracle:issue631] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException for RegulaFalsiSolver.solve(3624,f,1,10), but " + issue631Message);
        }

        // Metamorphic relation from the documented default: "all root-finding algorithms
        // must have ANY_SIDE as default for the allowed solutions." Therefore the overload
        // without AllowedSolution must agree with the overload using AllowedSolution.ANY_SIDE
        // on the same valid problem. We construct a linear function with a root exactly at
        // one endpoint so the expected answer is forced by the API, not guessed.
        RegulaFalsiSolver anySideSolver = new RegulaFalsiSolver();
        final double c = (double) data.consumeInt(-1000, 1000);
        final int w = data.consumeInt(1, 1000);
        final boolean leftRoot = data.consumeBoolean();
        final double min = leftRoot ? c : c - w;
        final double max = leftRoot ? c + w : c;
        UnivariateRealFunction endpointRoot = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c;
            }
        };

        Double defaultResult = null;
        try {
            defaultResult = Double.valueOf(anySideSolver.solve(100, endpointRoot, min, max));
        } catch (Throwable t) {
            return;
        }

        Double explicitAnySideResult = null;
        try {
            explicitAnySideResult = Double.valueOf(anySideSolver.solve(100, endpointRoot, min, max, AllowedSolution.ANY_SIDE));
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(defaultResult.doubleValue()) != Double.doubleToLongBits(explicitAnySideResult.doubleValue())) {
            throw new FuzzerSecurityIssueLow("[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) on the same valid problem; inputRoot=" + c + " min=" + min + " max=" + max + " lhs=" + defaultResult + " rhs=" + explicitAnySideResult);
        }

        // Sibling-agreement check across same-name overloads: the overload taking startValue
        // but no AllowedSolution is documented to solve the same problem with the default
        // ANY_SIDE behavior, so on this endpoint-root problem it must return the same exact
        // root as the other overloads. This also re-probes after the previous state-changing
        // explicit-allowed call to ensure solver state does not corrupt later default solves.
        final double startValue = leftRoot ? (c + (w / 2.0)) : (c - (w / 2.0));
        Double startValueResult = null;
        try {
            startValueResult = Double.valueOf(anySideSolver.solve(100, endpointRoot, min, max, startValue));
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(defaultResult.doubleValue()) != Double.doubleToLongBits(startValueResult.doubleValue())) {
            throw new FuzzerSecurityIssueLow("[oracle:overload-agreement] metamorphic violation: default solve overloads must agree on the same valid endpoint-root problem; inputRoot=" + c + " min=" + min + " max=" + max + " startValue=" + startValue + " lhs=" + defaultResult + " rhs=" + startValueResult);
        }

        // Post-condition on the real API result: when one endpoint is an exact root, doSolve()
        // returns that bound immediately ("If one of the bounds is the exact root, return it.").
        // A patch that merely suppresses throwing or corrupts state must still preserve this
        // documented observable. Because we constructed f(x)=x-c and chose c as an endpoint,
        // the exact expected root is trusted: it is the endpoint we built first.
        if (Double.doubleToLongBits(defaultResult.doubleValue()) != Double.doubleToLongBits(c)) {
            throw new FuzzerSecurityIssueLow("[oracle:endpoint-root] semantic mismatch: endpoint exact root must be returned; expected=" + c + " actual=" + defaultResult + " min=" + min + " max=" + max);
        }
        if (Double.doubleToLongBits(explicitAnySideResult.doubleValue()) != Double.doubleToLongBits(c)) {
            throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-explicit] semantic mismatch: endpoint exact root must be returned for explicit ANY_SIDE; expected=" + c + " actual=" + explicitAnySideResult + " min=" + min + " max=" + max);
        }
        if (Double.doubleToLongBits(startValueResult.doubleValue()) != Double.doubleToLongBits(c)) {
            throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-start] semantic mismatch: endpoint exact root must be returned for startValue overload; expected=" + c + " actual=" + startValueResult + " min=" + min + " max=" + max + " startValue=" + startValue);
        }
    }
}