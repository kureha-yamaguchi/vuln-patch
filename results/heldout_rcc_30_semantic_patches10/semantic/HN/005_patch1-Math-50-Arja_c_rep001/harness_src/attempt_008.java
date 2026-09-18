package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        // Fixed, lifted oracle from RegulaFalsiSolverTest.testIssue631.
        // Contract justification: BracketedUnivariateRealSolver.solve(...) documents that it throws
        // TooManyEvaluationsException if the allowed number of evaluations is exceeded; the failing
        // test pins the exact real-library setup where that must happen.
        final int pinnedMaxEval = 3624 + data.consumeInt(0, 0);
        final UnivariateRealFunction issue631 = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        boolean oracleIssue631Violated = false;
        String oracleIssue631Msg = null;
        Throwable oracleIssue631Cause = null;
        try {
            UnivariateRealSolver solver = new RegulaFalsiSolver();
            double root = solver.solve(pinnedMaxEval, issue631, 1.0, 10.0);
            oracleIssue631Violated = true;
            oracleIssue631Msg =
                    "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException " +
                    "for RegulaFalsiSolver.solve(3624,f,1.0,10.0) but completed normally with root=" + root +
                    " expectedBuggyRoot=3.4341896575482003";
        } catch (TooManyEvaluationsException expected) {
            // expected
        } catch (Throwable t) {
            oracleIssue631Violated = true;
            oracleIssue631Cause = t;
            oracleIssue631Msg =
                    "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException " +
                    "for RegulaFalsiSolver.solve(3624,f,1.0,10.0) but got " + t.getClass().getName();
        }
        if (oracleIssue631Violated) {
            if (oracleIssue631Cause != null) {
                throw new FuzzerSecurityIssueLow(oracleIssue631Msg, oracleIssue631Cause);
            }
            throw new FuzzerSecurityIssueLow(oracleIssue631Msg);
        }

        // Metamorphic/sibling-agreement check from documented default:
        // "For backwards compatibility, all root-finding algorithms must have ANY_SIDE as default
        // for the allowed solutions." Therefore the overload without AllowedSolution and the one
        // with AllowedSolution.ANY_SIDE must agree on the same valid problem.
        // We force a unique exact root at an endpoint, so both correct calls must return that
        // endpoint and any silent state corruption of BaseSecantSolver.allowed becomes observable.
        final int c = data.consumeInt(-1000, 1000);
        final int w = data.consumeInt(1, 1000);
        final boolean leftRoot = data.consumeBoolean();
        final double min = leftRoot ? c : (c - w);
        final double max = leftRoot ? (c + w) : c;
        final UnivariateRealFunction endpointRoot = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c;
            }
        };

        Double defaultAnySideLhs = null;
        Double defaultAnySideRhs = null;
        try {
            RegulaFalsiSolver solver1 = new RegulaFalsiSolver();
            defaultAnySideLhs = solver1.solve(100, endpointRoot, min, max);
            RegulaFalsiSolver solver2 = new RegulaFalsiSolver();
            defaultAnySideRhs = solver2.solve(100, endpointRoot, min, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(defaultAnySideLhs.doubleValue()) !=
                Double.doubleToLongBits(defaultAnySideRhs.doubleValue())) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) != " +
                    "solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) inputC=" + c +
                    " min=" + min + " max=" + max +
                    " lhs=" + defaultAnySideLhs + " rhs=" + defaultAnySideRhs);
        }

        // Additional sibling-agreement check across the same-name overload taking startValue:
        // all three overloads solve the same bracketed problem; with an exact root at one endpoint,
        // the documented "If one of the bounds is the exact root, return it" path in doSolve()
        // forces the same observable answer regardless of startValue or explicit ANY_SIDE.
        final double startValue = leftRoot ? max : min;
        Double overloadNoStart = null;
        Double overloadWithAllowed = null;
        Double overloadWithStart = null;
        try {
            RegulaFalsiSolver s1 = new RegulaFalsiSolver();
            RegulaFalsiSolver s2 = new RegulaFalsiSolver();
            RegulaFalsiSolver s3 = new RegulaFalsiSolver();
            overloadNoStart = s1.solve(100, endpointRoot, min, max);
            overloadWithAllowed = s2.solve(100, endpointRoot, min, max, AllowedSolution.ANY_SIDE);
            overloadWithStart = s3.solve(100, endpointRoot, min, max, startValue);
        } catch (Throwable t) {
            return;
        }
        long bitsNoStart = Double.doubleToLongBits(overloadNoStart.doubleValue());
        long bitsWithAllowed = Double.doubleToLongBits(overloadWithAllowed.doubleValue());
        long bitsWithStart = Double.doubleToLongBits(overloadWithStart.doubleValue());
        if (bitsNoStart != bitsWithAllowed || bitsNoStart != bitsWithStart) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:solve-overloads-agree] metamorphic violation: overload disagreement " +
                    "c=" + c + " min=" + min + " max=" + max + " startValue=" + startValue +
                    " noStart=" + overloadNoStart +
                    " explicitAnySide=" + overloadWithAllowed +
                    " withStart=" + overloadWithStart);
        }

        // Construct-from-known-answer oracle:
        // we choose the exact root first (c), then build f(x)=x-c with a bracket containing c.
        // For a correct solver, the returned endpoint-root must equal c exactly by the doSolve()
        // contract branch that returns a bound whose function value is exactly zero.
        Double exactRoot = null;
        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            exactRoot = solver.solve(100, endpointRoot, min, max);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(exactRoot.doubleValue()) != Double.doubleToLongBits((double) c)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:endpoint-exact-root] semantic mismatch: expected exact endpoint root " +
                    c + " but got " + exactRoot + " min=" + min + " max=" + max);
        }
    }
}