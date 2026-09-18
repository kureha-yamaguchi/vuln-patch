package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double seedRoot = 3.4341896575482003;
        final double leftOffset = seedRoot - 1.0;
        final double rightOffset = 10.0 - seedRoot;

        final double shift = data.consumeInt(-500000, 500000) / 100000.0;
        final double root = seedRoot + shift;
        final double min = root - leftOffset;
        final double max = root + rightOffset;

        final org.apache.commons.math.analysis.UnivariateRealFunction translatedIssue =
                new org.apache.commons.math.analysis.UnivariateRealFunction() {
                    public double value(double x) {
                        return Math.exp(x - root) - 1.0;
                    }
                };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        final double gap1 = 0.5 + (data.consumeInt(0, 1000) / 1000.0);
        final double gap2 = gap1 + 0.5 + (data.consumeInt(0, 1000) / 1000.0);
        final boolean probeAbove = data.consumeBoolean();

        double badMin;
        double badMax;
        if (probeAbove) {
            badMin = max + gap1;
            badMax = max + gap2;
        } else {
            badMin = min - gap2;
            badMax = min - gap1;
        }

        int rejectBudget = 8 + data.consumeInt(0, 64);

        // Documented guarantee: verifyBracketing(function, lower, upper) throws when
        // the interval does not bracket a root. This must remain true regardless of
        // prior solver state, so we re-probe after each state change.
        try {
            solver.solve(rejectBudget, translatedIssue, badMin, badMax);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:verify-bracketing-state-0] semantic mismatch: non-bracketing interval returned " +
                    "badMin=" + badMin + " badMax=" + badMax + " root=" + root);
        } catch (Throwable expected) { /*__vpRepair*/ if (expected instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) expected;
        }

        // Reach the real patched path through the public API on the translated
        // test shape that preserves the seed's asymmetric geometry around the known root.
        try {
            solver.solve(3624, translatedIssue, min, max);
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
        }

        try {
            solver.solve(rejectBudget, translatedIssue, badMin, badMax);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:verify-bracketing-state-1] semantic mismatch: non-bracketing interval returned after issue-style solve " +
                    "badMin=" + badMin + " badMax=" + badMax + " root=" + root +
                    " storedMin=" + solver.getMin() + " storedMax=" + solver.getMax());
        } catch (Throwable expected) { /*__vpRepair*/ if (expected instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) expected;
        }

        // Second state change: a different valid solve on the same real solver.
        final double easyWidth = 1.0 + (data.consumeInt(0, 1000) / 1000.0);
        final double easyMin = root - easyWidth;
        final double easyMax = root + easyWidth;
        try {
            double solved = solver.solve(128, translatedIssue, easyMin, easyMax);
            // Oracle from the constructed input itself: f(x)=exp(x-root)-1 has the exact
            // root at 'root'. Any correct solver result for this monotone bracketed call
            // must recover that known answer within the solver's configured tolerance.
            double tol = 1e-6;
            if (Math.abs(solved - root) > tol) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:translated-known-root] semantic mismatch: expected=" + root +
                        " actual=" + solved + " min=" + easyMin + " max=" + easyMax);
            }
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
        }

        try {
            solver.solve(rejectBudget, translatedIssue, badMin, badMax);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:verify-bracketing-state-2] semantic mismatch: non-bracketing interval returned after reuse " +
                    "badMin=" + badMin + " badMax=" + badMax + " root=" + root +
                    " storedMin=" + solver.getMin() + " storedMax=" + solver.getMax());
        } catch (Throwable expected) { /*__vpRepair*/ if (expected instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) expected;
        }
    }
}