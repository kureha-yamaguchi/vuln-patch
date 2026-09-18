package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        reachPatchedPath(data);
        checkPostReturnBudgetProbeOnIssue631();
        checkProtectedStateAndObjectiveAgreement(data);
    }

    private static void reachPatchedPath(FuzzedDataProvider data) {
        final double shift = data.consumeInt(-3, 3);
        final int maxEval = data.consumeInt(1000, 5000);
        final double min = 1.0 + shift;
        final double max = 10.0 + shift;
        final double target = Math.pow(Math.PI, 3.0) * Math.exp(shift);
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - target;
            }
        };
        final RegulaFalsiSolver solver = new RegulaFalsiSolver();
        try {
            solver.solve(maxEval, f, min, max);
        } catch (Throwable ignored) {
        }
    }

    private static void checkPostReturnBudgetProbeOnIssue631() {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();
        double root;
        try {
            root = solver.solve(3624, f, 1.0, 10.0);
        } catch (TooManyEvaluationsException expectedOnFixedBuild) {
            return;
        } catch (Throwable otherRejection) {
            return;
        }

        final double min = solver.getMin();
        final double max = solver.getMax();
        final int evals = solver.getEvaluations();
        try {
            solver.verifyBracketing(min, max);
        } catch (Throwable ignored) {
            return;
        }

        try {
            solver.computeObjectiveValue(root);
        } catch (TooManyEvaluationsException exhaustedAfterNormalReturn) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:post-return-budget-probe] semantic mismatch: solve(3624,exp(x)-PI^3,[1,10]) returned normally with root="
                    + root
                    + " min=" + min
                    + " max=" + max
                    + " evalsAtReturn=" + evals
                    + " but an immediate protected computeObjectiveValue(root) was rejected for exhausted evaluations",
                exhaustedAfterNormalReturn);
        } catch (Throwable ignored) {
            return;
        }
    }

    private static void checkProtectedStateAndObjectiveAgreement(FuzzedDataProvider data) {
        final int ai = data.consumeInt(-1000, 1000);
        final int di = data.consumeInt(1, 1000);
        final double a = ai;
        final double min = a;
        final double max = ai + di;
        final double expectedAbs = Math.max(1e-12, Math.abs(data.consumeInt(-1000, 1000)) * 1e-9 + 1e-12);
        final double expectedRel = Math.max(1e-12, Math.abs(data.consumeInt(-1000, 1000)) * 1e-9 + 1e-12);
        final double expectedFv = Math.max(1e-12, Math.abs(data.consumeInt(-1000, 1000)) * 1e-9 + 1e-12);

        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return x - a;
            }
        };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver(expectedRel, expectedAbs, expectedFv);
        double root;
        try {
            root = solver.solve(50, f, min, max);
        } catch (Throwable rejected) {
            return;
        }

        // Contract used: getters report the state established by construction/solve,
        // and computeObjectiveValue delegates to function.value(point) while incrementing
        // the evaluation count. A throw-deleting or state-corrupting patch in the same
        // region can leave these exposed quantities inconsistent even when solve returns.
        final double reportedMin = solver.getMin();
        final double reportedMax = solver.getMax();
        final double reportedAbs = solver.getAbsoluteAccuracy();
        final double reportedRel = solver.getRelativeAccuracy();
        final double reportedFv = solver.getFunctionValueAccuracy();

        if (reportedMin != min || reportedMax != max) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:reported-search-interval] consistency violation: expectedMin=" + min
                    + " actualMin=" + reportedMin
                    + " expectedMax=" + max
                    + " actualMax=" + reportedMax
                    + " root=" + root);
        }
        if (reportedAbs != expectedAbs || reportedRel != expectedRel || reportedFv != expectedFv) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:reported-configured-accuracies] consistency violation: expectedAbs=" + expectedAbs
                    + " actualAbs=" + reportedAbs
                    + " expectedRel=" + expectedRel
                    + " actualRel=" + reportedRel
                    + " expectedFv=" + expectedFv
                    + " actualFv=" + reportedFv);
        }

        try {
            solver.verifyBracketing(reportedMin, reportedMax);
        } catch (Throwable rejected) {
            return;
        }

        final int before = solver.getEvaluations();
        final double objective;
        try {
            objective = solver.computeObjectiveValue(root);
        } catch (Throwable rejected) {
            return;
        }
        final int after = solver.getEvaluations();
        final double direct = f.value(root);

        if (Double.doubleToLongBits(objective) != Double.doubleToLongBits(direct)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:protected-objective-delegation] consistency violation: computeObjectiveValue=" + objective
                    + " directFunctionValue=" + direct
                    + " root=" + root);
        }
        if (after != before + 1) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:protected-objective-increments-count] consistency violation: evaluationsBefore=" + before
                    + " evaluationsAfter=" + after
                    + " root=" + root
                    + " objective=" + objective);
        }
    }
}