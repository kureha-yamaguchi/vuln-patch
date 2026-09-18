package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            final int a1 = data.consumeInt(-1000, 1000);
            final int d1 = data.consumeInt(1, 1000);
            final double min1 = a1;
            final double max1 = a1 + d1;
            final double start1 = min1 + (max1 - min1) * (data.consumeInt(0, 1000) / 1000.0);
            final double start2 = min1 + (max1 - min1) * (data.consumeInt(0, 1000) / 1000.0);

            final org.apache.commons.math.analysis.UnivariateRealFunction endpointRoot =
                    new org.apache.commons.math.analysis.UnivariateRealFunction() {
                        public double value(double x) {
                            return x - min1;
                        }
                    };

            final RegulaFalsiSolver stateSolver = new RegulaFalsiSolver();

            double r1;
            try {
                r1 = stateSolver.solve(50, endpointRoot, min1, max1, start1);
            } catch (Throwable t) {
                r1 = Double.NaN;
            }
            if (!Double.isNaN(r1)) {
                // Contract justification: solve(maxEval,f,min,max,startValue) writes the search state for this
                // invocation, and the reader getStartValue() must report that explicit start value afterwards.
                // Re-checking after a second solve catches stale-state bugs that a throw-deleting patch would not.
                if (r1 != min1 || stateSolver.getStartValue() != start1) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:startvalue-state-1] semantic mismatch: returned=" + r1
                                    + " expectedRoot=" + min1
                                    + " reportedStart=" + stateSolver.getStartValue()
                                    + " expectedStart=" + start1);
                }

                double r2;
                try {
                    r2 = stateSolver.solve(50, endpointRoot, min1, max1, start2);
                } catch (Throwable t) {
                    r2 = Double.NaN;
                }
                if (!Double.isNaN(r2) && (r2 != min1 || stateSolver.getStartValue() != start2)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:startvalue-state-2] semantic mismatch: returned=" + r2
                                    + " expectedRoot=" + min1
                                    + " reportedStart=" + stateSolver.getStartValue()
                                    + " expectedStart=" + start2);
                }
            }
        }

        {
            final org.apache.commons.math.analysis.UnivariateRealFunction f =
                    new org.apache.commons.math.analysis.UnivariateRealFunction() {
                        public double value(double x) {
                            return Math.exp(x) - Math.pow(Math.PI, 3.0);
                        }
                    };
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();

            boolean violated = false;
            double result = Double.NaN;
            Throwable unexpected = null;
            try {
                result = solver.solve(3624, f, 1.0, 10.0);
                violated = true;
            } catch (org.apache.commons.math.exception.TooManyEvaluationsException expected) {
                // Correct behavior pinned by RegulaFalsiSolverTest.testIssue631.
            } catch (Throwable t) {
                violated = true;
                unexpected = t;
            }

            if (violated) {
                if (unexpected != null) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:issue631-exact-throw] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1.0,10.0) but got "
                                    + unexpected.getClass().getName(),
                            unexpected);
                }
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:issue631-exact-throw] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1.0,10.0) but completed normally with result="
                                + result);
            }
        }
    }
}