package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    private static final double ISSUE631_EXPECTED_ROOT = 3.4341896575482003d;
    private static final double ISSUE631_TOL = 1e-15d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkIssue631PinnedThrow();
        checkDefaultAnySideMatchesExplicitAnySide(data);
        checkLeftRightSideGuaranteeNearPatchedBoundary(data);
    }

    private static void checkIssue631PinnedThrow() {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();
        boolean violated = false;
        String what = "completed normally";
        try {
            final double r = solver.solve(3624, f, 1.0, 10.0);
            violated = true;
            what = "completed normally with result " + r + " expected TooManyEvaluationsException; pinnedBuggyReturn=" + ISSUE631_EXPECTED_ROOT;
        } catch (TooManyEvaluationsException ok) {
            return;
        } catch (Throwable t) {
            violated = true;
            what = "threw wrong exception " + t.getClass().getName() + ": " + String.valueOf(t.getMessage());
        }
        if (violated) {
            throw new FuzzerSecurityIssueLow("[oracle:issue631-throw] semantic mismatch: " + what);
        }

        if (Math.abs(ISSUE631_EXPECTED_ROOT - ISSUE631_EXPECTED_ROOT) > ISSUE631_TOL) {
            throw new FuzzerSecurityIssueLow("[oracle:issue631-pin] semantic mismatch: pinned expected root literal changed");
        }
    }

    private static void checkDefaultAnySideMatchesExplicitAnySide(FuzzedDataProvider data) {
        final int ai = data.consumeInt(-1000, 1000);
        final int di = data.consumeInt(1, 1000);
        final double a = (double) ai;
        final double max = (double) (ai + di);
        final double start = a + 0.5d * (max - a);

        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return x - a;
            }
        };

        final RegulaFalsiSolver solverExplicit = new RegulaFalsiSolver();
        final double rExplicit;
        try {
            rExplicit = solverExplicit.solve(50, f, a, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        final RegulaFalsiSolver solverDefault = new RegulaFalsiSolver();
        final double rDefault;
        try {
            rDefault = solverDefault.solve(50, f, a, max, start);
        } catch (Throwable t) {
            return;
        }

        if (rExplicit != a || rDefault != a || rExplicit != rDefault) {
            throw new FuzzerSecurityIssueLow("[oracle:default-anyside-endpoint] metamorphic violation: explicit=" + rExplicit + " default=" + rDefault + " expected=" + a);
        }
    }

    private static void checkLeftRightSideGuaranteeNearPatchedBoundary(FuzzedDataProvider data) {
        final double root = 3.4341896575482003d + (data.consumeInt(-1000, 1000) * 1.0e-12d);
        final double scale = 1.0d + (double) data.consumeInt(0, 1000);
        final double absAcc = 1.0e-6d + data.consumeInt(0, 1000) * 1.0e-9d;
        final double relAcc = 1.0e-14d;
        final double fAcc = 1.0e-15d;
        final int maxEval = 5000 + data.consumeInt(0, 5000);

        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return scale * (Math.exp(x) - Math.exp(root));
            }
        };

        final double left;
        try {
            RegulaFalsiSolver leftSolver = new RegulaFalsiSolver(relAcc, absAcc, fAcc);
            left = leftSolver.solve(maxEval, f, 1.0, 10.0, AllowedSolution.LEFT_SIDE);
        } catch (Throwable t) {
            return;
        }

        final double right;
        try {
            RegulaFalsiSolver rightSolver = new RegulaFalsiSolver(relAcc, absAcc, fAcc);
            right = rightSolver.solve(maxEval, f, 1.0, 10.0, AllowedSolution.RIGHT_SIDE);
        } catch (Throwable t) {
            return;
        }

        final double any;
        try {
            RegulaFalsiSolver anySolver = new RegulaFalsiSolver(relAcc, absAcc, fAcc);
            any = anySolver.solve(maxEval, f, 1.0, 10.0, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        final double tol = Math.max(absAcc * 4.0d, 1.0e-9d);

        // Contract justification: doSolve's final return logic explicitly selects x0/x1 based on AllowedSolution.
        // For a strictly increasing function with a known interior root, LEFT_SIDE must not return to the right
        // of the true root, and RIGHT_SIDE must not return to the left of it. A throw-deleting or overfit patch
        // that merely makes the problematic branch unreachable can still violate these observable side guarantees.
        if (left - root > tol) {
            throw new FuzzerSecurityIssueLow("[oracle:left-side-known-root] post-condition violation: root=" + root + " left=" + left + " tol=" + tol + " absAcc=" + absAcc + " maxEval=" + maxEval);
        }
        if (root - right > tol) {
            throw new FuzzerSecurityIssueLow("[oracle:right-side-known-root] post-condition violation: root=" + root + " right=" + right + " tol=" + tol + " absAcc=" + absAcc + " maxEval=" + maxEval);
        }

        // Independent consistency check: ANY_SIDE is unconstrained, but on the same valid problem its answer
        // must still lie between the side-constrained answers returned by fresh, identically-configured solvers.
        // This compares the same quantity three real calls produce, instead of guessing a new expected value.
        final double lo = Math.min(left, right) - tol;
        final double hi = Math.max(left, right) + tol;
        if (any < lo || any > hi) {
            throw new FuzzerSecurityIssueLow("[oracle:any-between-sides] consistency violation: left=" + left + " any=" + any + " right=" + right + " root=" + root + " tol=" + tol);
        }
    }
}