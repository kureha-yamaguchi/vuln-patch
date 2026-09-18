package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final RegulaFalsiSolver issue631Solver;
        final org.apache.commons.math.analysis.UnivariateRealFunction issue631Function;
        try {
            issue631Solver = new RegulaFalsiSolver();
            issue631Function = new org.apache.commons.math.analysis.UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };
        } catch (Throwable t) {
            return;
        }

        boolean issue631Violated = false;
        String issue631Message = null;
        try {
            final int maxEval = 3624 + data.consumeInt(0, 0);
            final double root = issue631Solver.solve(maxEval, issue631Function, 1.0, 10.0);
            final long actualBits = Double.doubleToLongBits(root);
            final long buggyBits = Double.doubleToLongBits(3.4341896575482003);
            if (actualBits == buggyBits) {
                issue631Violated = true;
                issue631Message =
                        "[oracle:issue631-throw] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but call completed normally with root="
                                + root;
            } else {
                issue631Violated = true;
                issue631Message =
                        "[oracle:issue631-throw] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but call completed normally with unexpected root="
                                + root + " instead of the failing test's pinned buggy value 3.4341896575482003";
            }
        } catch (org.apache.commons.math.exception.TooManyEvaluationsException expected) {
        } catch (Throwable t) {
            issue631Violated = true;
            issue631Message =
                    "[oracle:issue631-throw] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but got "
                            + t.getClass().getName() + ": " + String.valueOf(t.getMessage());
        }
        if (issue631Violated) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(issue631Message);
        }

        final RegulaFalsiSolver solver;
        final double c;
        final int w;
        final boolean leftRoot;
        final double min;
        final double max;
        final double startValue;
        final org.apache.commons.math.analysis.UnivariateRealFunction linearFunction;
        try {
            solver = new RegulaFalsiSolver();
            c = data.consumeInt(-1000, 1000);
            w = data.consumeInt(1, 1000);
            leftRoot = data.consumeBoolean();
            min = leftRoot ? c : c - w;
            max = leftRoot ? c + w : c;
            startValue = min + (max - min) / 2.0;
            linearFunction = new org.apache.commons.math.analysis.UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };
        } catch (Throwable t) {
            return;
        }

        double defaultRoot = 0.0;
        double explicitAnyRoot = 0.0;
        boolean defaultVsExplicitReady = false;
        try {
            defaultRoot = solver.solve(100, linearFunction, min, max);
            explicitAnyRoot = solver.solve(100, linearFunction, min, max, AllowedSolution.ANY_SIDE);
            defaultVsExplicitReady = true;
        } catch (Throwable t) {
            return;
        }
        if (defaultVsExplicitReady &&
                Double.doubleToLongBits(defaultRoot) != Double.doubleToLongBits(explicitAnyRoot)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must match solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) on the same valid problem; min="
                            + min + " max=" + max + " c=" + c + " lhs=" + defaultRoot + " rhs=" + explicitAnyRoot);
        }

        double startRoot = 0.0;
        double startAnyRoot = 0.0;
        boolean startAgreementReady = false;
        try {
            startRoot = solver.solve(100, linearFunction, min, max, startValue);
            startAnyRoot = solver.solve(100, linearFunction, min, max, startValue, AllowedSolution.ANY_SIDE);
            startAgreementReady = true;
        } catch (Throwable t) {
            return;
        }
        if (startAgreementReady &&
                Double.doubleToLongBits(startRoot) != Double.doubleToLongBits(startAnyRoot)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:start-overload-any-side] metamorphic violation: solve overload with default allowed side must match explicit ANY_SIDE; min="
                            + min + " max=" + max + " startValue=" + startValue + " c=" + c
                            + " lhs=" + startRoot + " rhs=" + startAnyRoot);
        }

        double postMutationDefaultRoot = 0.0;
        double postMutationExplicitAnyRoot = 0.0;
        boolean postMutationReady = false;
        try {
            final double c2 = data.consumeInt(-1000, 1000);
            final int w2 = data.consumeInt(1, 1000);
            final boolean leftRoot2 = data.consumeBoolean();
            final double min2 = leftRoot2 ? c2 : c2 - w2;
            final double max2 = leftRoot2 ? c2 + w2 : c2;
            final org.apache.commons.math.analysis.UnivariateRealFunction linearFunction2 =
                    new org.apache.commons.math.analysis.UnivariateRealFunction() {
                        public double value(double x) {
                            return x - c2;
                        }
                    };

            /* Contract justification:
             * BaseSecantSolver constructors set allowed=ANY_SIDE, and BracketedUnivariateRealSolver
             * documents ANY_SIDE as the default for backwards compatibility. The explicit-allowed
             * solve overload writes the shared `allowed` field; re-checking the default overload
             * after that state change ensures this "question-like" behaviour remains consistent and
             * catches a patch that merely suppresses the problematic branch or leaves stale state. */
            postMutationDefaultRoot = solver.solve(100, linearFunction2, min2, max2);
            postMutationExplicitAnyRoot = solver.solve(100, linearFunction2, min2, max2, AllowedSolution.ANY_SIDE);
            postMutationReady = true;
        } catch (Throwable t) {
            return;
        }
        if (postMutationReady &&
                Double.doubleToLongBits(postMutationDefaultRoot) != Double.doubleToLongBits(postMutationExplicitAnyRoot)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:post-state-default-any-side] metamorphic violation: default ANY_SIDE must still agree with explicit ANY_SIDE after prior solve calls mutated solver state; lhs="
                            + postMutationDefaultRoot + " rhs=" + postMutationExplicitAnyRoot);
        }
    }
}