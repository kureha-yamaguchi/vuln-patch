package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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

        try {
            UnivariateRealSolver solver = new RegulaFalsiSolver();
            double root = solver.solve(3624, issue631Function, 1.0, 10.0);
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631] semantic mismatch: expected TooManyEvaluationsException from RegulaFalsiSolver.solve(3624,f,1,10) but got root=" +
                root + " expectedBuggyValue=3.4341896575482003");
        } catch (TooManyEvaluationsException expected) {
            // Expected per lifted test oracle.
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        final double chosenRoot = data.consumeInt(-1000, 1000);
        final double width = data.consumeInt(1, 1000);
        final double min = chosenRoot;
        final double max = chosenRoot + width;
        final double start = min + (max - min) * 0.5;

        final UnivariateRealFunction endpointRootFunction = new UnivariateRealFunction() {
            public double value(double x) {
                return x - chosenRoot;
            }
        };

        final RegulaFalsiSolver solver2 = new RegulaFalsiSolver();

        try {
            // Contract cited from BracketedUnivariateRealSolver: solvers that require bracketing
            // "should be able to handle the case where one of the endpoints is itself a root."
            // Here min is exactly a root, so every correct overload must return that exact endpoint.
            double explicitAny = solver2.solve(32, endpointRootFunction, min, max, AllowedSolution.ANY_SIDE);
            if (Double.doubleToLongBits(explicitAny) != Double.doubleToLongBits(min)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:endpoint-any] metamorphic violation: endpoint root with explicit ANY_SIDE must return min input=" +
                    min + "," + max + " actual=" + explicitAny + " expected=" + min);
            }

            // Same contract: exact endpoint root is acceptable regardless of allowed side.
            double explicitLeft = solver2.solve(32, endpointRootFunction, min, max, start, AllowedSolution.LEFT_SIDE);
            if (Double.doubleToLongBits(explicitLeft) != Double.doubleToLongBits(min)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:endpoint-left] metamorphic violation: endpoint root with explicit LEFT_SIDE must return min input=" +
                    min + "," + max + "," + start + " actual=" + explicitLeft + " expected=" + min);
            }

            // State-coupling/post-condition check:
            // BaseSecantSolver constructors establish default allowed == ANY_SIDE, and backwards-compatibility
            // contract says ANY_SIDE is the default. After a prior solve call changed receiver state via
            // solve(..., allowedSolution), the overload without allowedSolution must still behave as the
            // default ANY_SIDE overload on the same valid problem.
            double defaultOverload = solver2.solve(32, endpointRootFunction, min, max, start);
            if (Double.doubleToLongBits(defaultOverload) != Double.doubleToLongBits(min)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:default-any-after-mutation] metamorphic violation: default overload must preserve ANY_SIDE semantics after prior explicit-allowed solve input=" +
                    min + "," + max + "," + start + " actual=" + defaultOverload + " expected=" + min);
            }

            double explicitAnyWithStart = solver2.solve(32, endpointRootFunction, min, max, start, AllowedSolution.ANY_SIDE);
            if (Double.doubleToLongBits(defaultOverload) != Double.doubleToLongBits(explicitAnyWithStart)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement] metamorphic violation: equivalent overloads must agree on exact endpoint-root problem input=" +
                    min + "," + max + "," + start + " lhs=" + defaultOverload + " rhs=" + explicitAnyWithStart);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }
}