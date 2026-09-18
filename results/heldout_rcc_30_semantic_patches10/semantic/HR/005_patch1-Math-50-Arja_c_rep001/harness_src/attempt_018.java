package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static String oneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Reach the patched REGULA_FALSI path through the real public API using the exact issue631 setup.
        try {
            org.apache.commons.math.analysis.UnivariateRealFunction seedFunction =
                new org.apache.commons.math.analysis.UnivariateRealFunction() {
                    public double value(double x) {
                        return Math.exp(x) - Math.pow(Math.PI, 3.0);
                    }
                };
            RegulaFalsiSolver seedSolver = new RegulaFalsiSolver();
            try {
                seedSolver.solve(3624, seedFunction, 1.0, 10.0);
            } catch (Throwable ignored) {
                // Reaching the real code path is enough here; the already-known symptom is not this harness's oracle.
            }
        } catch (Throwable ignoredOuter) {
            return;
        }

        // New independent oracle family: evaluation-count consistency.
        // Justification from BaseAbstractUnivariateRealSolver.computeObjectiveValue shown above:
        // each solver-side objective evaluation does exactly one incrementEvaluationCount() and then one function.value(point).
        // Therefore the solver's reported getEvaluations() must equal the empirical number of value(...) calls observed by the function.
        final double root = data.consumeInt(-8, 8) + (data.consumeInt(1, 999) / 1000.0);
        final double min = root - 1.0;
        final double max = root + 1.0;
        final int maxEval = data.consumeInt(20, 400);
        final int[] calls1 = new int[] { 0 };

        org.apache.commons.math.analysis.UnivariateRealFunction countedFunction1 =
            new org.apache.commons.math.analysis.UnivariateRealFunction() {
                public double value(double x) {
                    calls1[0]++;
                    return Math.exp(x - root) - 1.0;
                }
            };

        RegulaFalsiSolver solver = new RegulaFalsiSolver();
        double result1;
        try {
            result1 = solver.solve(maxEval, countedFunction1, min, max);
        } catch (Throwable t) {
            return;
        }

        int reported1 = solver.getEvaluations();
        if (reported1 != calls1[0]) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:eval-count-consistency] consistency violation: reportedEvaluations=" + reported1 +
                " empiricalCalls=" + calls1[0] +
                " result=" + result1 +
                " min=" + min +
                " max=" + max +
                " maxEval=" + maxEval);
        }

        // Second independent check on the same receiver after state change.
        // Justification: each solve(...) establishes a fresh problem; the same computeObjectiveValue contract still implies
        // getEvaluations() must match the empirical call count for this second solve, not any cumulative value from prior solves.
        final double endpointRoot = data.consumeInt(-20, 20);
        final double max2 = endpointRoot + data.consumeInt(1, 20);
        final int maxEval2 = data.consumeInt(5, 50);
        final int[] calls2 = new int[] { 0 };

        org.apache.commons.math.analysis.UnivariateRealFunction countedFunction2 =
            new org.apache.commons.math.analysis.UnivariateRealFunction() {
                public double value(double x) {
                    calls2[0]++;
                    return x - endpointRoot;
                }
            };

        double result2;
        try {
            result2 = solver.solve(maxEval2, countedFunction2, endpointRoot, max2);
        } catch (Throwable t) {
            return;
        }

        int reported2 = solver.getEvaluations();
        if (reported2 != calls2[0]) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:eval-count-reset] consistency violation: secondSolveReportedEvaluations=" + reported2 +
                " secondSolveEmpiricalCalls=" + calls2[0] +
                " secondResult=" + result2 +
                " secondMin=" + endpointRoot +
                " secondMax=" + max2 +
                " secondMaxEval=" + maxEval2);
        }

        // Fresh-object cross-check of the same quantity, obtained a second independent way.
        // Justification: two identically constructed solver instances solving the same deterministic problem must observe
        // the same number of function.value(...) calls when the function is pure and the inputs are identical.
        final int[] calls3 = new int[] { 0 };
        org.apache.commons.math.analysis.UnivariateRealFunction countedFunction3 =
            new org.apache.commons.math.analysis.UnivariateRealFunction() {
                public double value(double x) {
                    calls3[0]++;
                    return Math.exp(x - root) - 1.0;
                }
            };
        RegulaFalsiSolver freshSolver = new RegulaFalsiSolver();
        double result3;
        try {
            result3 = freshSolver.solve(maxEval, countedFunction3, min, max);
        } catch (Throwable t) {
            return;
        }

        int reported3 = freshSolver.getEvaluations();
        if (reported3 != calls3[0]) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:fresh-eval-count-consistency] consistency violation: freshReportedEvaluations=" + reported3 +
                " freshEmpiricalCalls=" + calls3[0] +
                " freshResult=" + result3 +
                " min=" + min +
                " max=" + max +
                " maxEval=" + maxEval);
        }

        if (reported1 != reported3) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:solver-instance-agreement] metamorphic violation: identical fresh solves disagreed on evaluation count inputRoot=" +
                root + " min=" + min + " max=" + max + " maxEval=" + maxEval +
                " firstReported=" + reported1 + " freshReported=" + reported3 +
                " firstResult=" + result1 + " freshResult=" + result3);
        }
    }
}