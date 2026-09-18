package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.NoBracketingException;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkIssue631PinnedThrow();
        checkDefaultAnySideAgreement(data);
        checkNoBracketingRejectionIsStateIndependent(data);
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
            double r = solver.solve(3624, f, 1.0, 10.0);
            violated = true;
            what = "completed normally with result " + r;
        } catch (TooManyEvaluationsException ok) {
            return;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631-pinned] semantic mismatch: expected TooManyEvaluationsException but caught "
                    + t.getClass().getName() + " message=" + String.valueOf(t.getMessage()),
                t);
        }
        if (violated) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631-pinned] semantic mismatch: expected TooManyEvaluationsException but " + what);
        }
    }

    private static void checkDefaultAnySideAgreement(FuzzedDataProvider data) {
        final int ai = data.consumeInt(-1000, 1000);
        final int di = data.consumeInt(1, 1000);
        final double a = ai;
        final double max = ai + di;
        final double start = a + 0.5 * (max - a);

        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return x - a;
            }
        };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        final double rExplicit;
        try {
            rExplicit = solver.solve(50, f, a, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        final double rDefault;
        try {
            rDefault = solver.solve(50, f, a, max, start);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (rExplicit != a || rDefault != a || rExplicit != rDefault) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-anyside-explicit] metamorphic violation: explicit="
                    + rExplicit + " default=" + rDefault + " expected=" + a);
        }
    }

    private static void checkNoBracketingRejectionIsStateIndependent(FuzzedDataProvider data) {
        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        final int mutations = data.consumeInt(1, 4);

        probeNoBracketingMustReject(solver);

        for (int i = 0; i < mutations; i++) {
            final int rootInt = data.consumeInt(-1000, 1000);
            final int widthInt = data.consumeInt(1, 1000);
            final double min = rootInt;
            final double max = rootInt + widthInt;
            final double root = min;

            final UnivariateRealFunction valid = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - root;
                }
            };

            try {
                double result = solver.solve(64, valid, min, max);
                if (result != root) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:stateful-valid-solve] semantic mismatch: endpoint root expected="
                            + root + " actual=" + result);
                }
                // Documented/state-coupling check: getMin()/getMax() report the interval established by solve().
                // A correct implementation writes searchMin/searchMax during solve setup and these readers must
                // reflect that exact state; a band-aid around the main symptom that corrupts or fails to update
                // solver state would violate this even if the returned root looked plausible.
                if (solver.getMin() != min || solver.getMax() != max) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:getter-state-after-solve] consistency violation: expectedMin="
                            + min + " actualMin=" + solver.getMin()
                            + " expectedMax=" + max + " actualMax=" + solver.getMax());
                }
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }

            // Re-probe the documented rejection after each state change.
            probeNoBracketingMustReject(solver);

            // Hidden-state check: these reader methods are pure accessors over stored solver state/accuracy.
            // Re-reading them must not mutate the interval state established above.
            final double beforeMin = solver.getMin();
            final double beforeMax = solver.getMax();
            solver.getAbsoluteAccuracy();
            solver.getRelativeAccuracy();
            solver.getFunctionValueAccuracy();
            final double afterMin = solver.getMin();
            final double afterMax = solver.getMax();
            if (beforeMin != afterMin || beforeMax != afterMax) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:getter-purity] consistency violation: min/max changed after read-only getters"
                        + " beforeMin=" + beforeMin + " afterMin=" + afterMin
                        + " beforeMax=" + beforeMax + " afterMax=" + afterMax);
            }
        }
    }

    private static void probeNoBracketingMustReject(RegulaFalsiSolver solver) {
        final UnivariateRealFunction nonBracketing = new UnivariateRealFunction() {
            public double value(double x) {
                return x * x + 1.0;
            }
        };

        try {
            solver.solve(32, nonBracketing, -2.0, 2.0);
        } catch (NoBracketingException expected) {
            return;
        } catch (TooManyEvaluationsException unexpected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:rejection-no-bracket-state] semantic mismatch: expected NoBracketingException but got TooManyEvaluationsException",
                unexpected);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            throw new FuzzerSecurityIssueLow(
                "[oracle:rejection-no-bracket-state] semantic mismatch: expected NoBracketingException but got "
                    + t.getClass().getName() + " message=" + String.valueOf(t.getMessage()),
                t);
        }
        throw new FuzzerSecurityIssueLow(
            "[oracle:rejection-no-bracket-state] semantic mismatch: non-bracketing interval was accepted");
    }
}