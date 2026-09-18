package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    private static final UnivariateRealFunction ISSUE631_FUNCTION = new UnivariateRealFunction() {
        public double value(double x) {
            return Math.exp(x) - Math.pow(Math.PI, 3.0);
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkIssue631PinnedBehavior();
        helperConsistencyChecks(data);
    }

    private static void checkIssue631PinnedBehavior() {
        final UnivariateRealSolver solver = new RegulaFalsiSolver();
        try {
            final double root = solver.solve(3624, ISSUE631_FUNCTION, 1, 10);
            if (Double.doubleToLongBits(root) == Double.doubleToLongBits(3.4341896575482003d)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:issue631-pinned-value] semantic mismatch: expected TooManyEvaluationsException but solve returned pinned buggy value root="
                                + root);
            }
            throw new FuzzerSecurityIssueLow(
                    "[oracle:issue631-returned-instead-of-throwing] semantic mismatch: expected TooManyEvaluationsException but solve returned root="
                            + root);
        } catch (TooManyEvaluationsException expected) {
            // Expected by the lifted regression test.
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            // Any other documented rejection is not the target of this semantic oracle.
        }
    }

    private static void helperConsistencyChecks(FuzzedDataProvider data) {
        final int samples = data.consumeInt(1, 4);
        final double[] points = new double[samples];
        for (int i = 0; i < samples; i++) {
            points[i] = 1.0 + (data.consumeInt(0, 9000) / 1000.0);
        }

        final RegulaFalsiSolver helperSolver = new RegulaFalsiSolver();
        try {
            helperSolver.setup(samples + 8, ISSUE631_FUNCTION, 1.0, 10.0, 5.0);
        } catch (RuntimeException setupRejected) { /*__vpRepair*/ if (setupRejected instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) setupRejected;
            return;
        }

        try {
            final double reportedMin = helperSolver.getMin();
            final double reportedMax = helperSolver.getMax();

            for (int i = 0; i < points.length; i++) {
                final double p = points[i];
                final double viaSolver = helperSolver.computeObjectiveValue(p);
                final double direct = ISSUE631_FUNCTION.value(p);
                if (Double.doubleToLongBits(viaSolver) != Double.doubleToLongBits(direct)) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:objective-consistency] consistency violation: point=" + p
                                    + " viaSolver=" + viaSolver + " direct=" + direct
                                    + " min=" + reportedMin + " max=" + reportedMax);
                }
            }

            final double fMin = ISSUE631_FUNCTION.value(reportedMin);
            final double fMax = ISSUE631_FUNCTION.value(reportedMax);

            if (!(fMin * fMax <= 0.0)) {
                return;
            }
            helperSolver.verifyBracketing(reportedMin, reportedMax);
        } catch (TooManyEvaluationsException skipped) {
            return;
        } catch (RuntimeException unexpected) { /*__vpRepair*/ if (unexpected instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) unexpected;
            throw new FuzzerSecurityIssueLow(
                    "[oracle:verify-bracketing-helper] metamorphic violation: helper state was internally inconsistent for reported interval ["
                            + helperSolver.getMin() + "," + helperSolver.getMax() + "] exception="
                            + unexpected.getClass().getName(),
                    unexpected);
        }
    }
}