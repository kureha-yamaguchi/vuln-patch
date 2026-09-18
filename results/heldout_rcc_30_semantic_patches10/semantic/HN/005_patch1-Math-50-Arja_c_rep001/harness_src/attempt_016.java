package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            final org.apache.commons.math.analysis.UnivariateRealFunction f =
                    new org.apache.commons.math.analysis.UnivariateRealFunction() {
                        public double value(double x) {
                            return Math.exp(x) - Math.pow(Math.PI, 3.0);
                        }
                    };

            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final int maxEval = 3624 + data.consumeInt(0, 0);

            boolean violation = false;
            double returned = Double.NaN;
            Throwable wrong = null;

            try {
                returned = solver.solve(maxEval, f, 1.0, 10.0);
                violation = true;
            } catch (org.apache.commons.math.exception.TooManyEvaluationsException expected) {
                // Ground-truth lifted oracle from RegulaFalsiSolverTest.testIssue631:
                // for this exact input the correct behavior is to throw TooManyEvaluationsException.
            } catch (Throwable t) {
                violation = true;
                wrong = t;
            }

            if (violation) {
                if (wrong != null) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:issue631] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but got "
                                    + wrong.getClass().getName(),
                            wrong);
                }
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:issue631] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but solve returned "
                                + returned);
            }
        }

        {
            RegulaFalsiSolver solver;
            org.apache.commons.math.analysis.UnivariateRealFunction f;
            double c;
            double min;
            double max;
            double startValue;
            double rDefault;
            double rExplicitAny;
            double rStartAny;

            try {
                solver = new RegulaFalsiSolver();
                c = data.consumeInt(-1000, 1000);
                int w = data.consumeInt(1, 1000);
                boolean leftRoot = data.consumeBoolean();
                min = leftRoot ? c : c - w;
                max = leftRoot ? c + w : c;
                startValue = min + (max - min) / 2.0;
                final double root = c;
                f = new org.apache.commons.math.analysis.UnivariateRealFunction() {
                    public double value(double x) {
                        return x - root;
                    }
                };
            } catch (Throwable t) {
                return;
            }

            try {
                rDefault = solver.solve(100, f, min, max);
                rExplicitAny = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
                rStartAny = solver.solve(100, f, min, max, startValue, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            // Documented contract: all root-finding algorithms must have ANY_SIDE as default.
            // Therefore the default overload and the explicit ANY_SIDE overloads must agree.
            if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicitAny)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) != solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE)"
                                + " min=" + min + " max=" + max + " default=" + rDefault + " explicitAny=" + rExplicitAny);
            }

            // Same documented job, same logical input; on an endpoint root, start value is irrelevant.
            if (Double.doubleToLongBits(rExplicitAny) != Double.doubleToLongBits(rStartAny)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:sibling-overloads] metamorphic violation: explicit ANY_SIDE overloads disagree"
                                + " min=" + min + " max=" + max + " startValue=" + startValue
                                + " noStart=" + rExplicitAny + " withStart=" + rStartAny);
            }
        }

        {
            RegulaFalsiSolver solver;
            double c;
            double min;
            double max;
            double expected;
            org.apache.commons.math.analysis.UnivariateRealFunction f;

            try {
                solver = new RegulaFalsiSolver();
                c = data.consumeInt(-1000, 1000);
                int w = data.consumeInt(1, 1000);
                boolean rootOnLeft = data.consumeBoolean();
                min = rootOnLeft ? c : c - w;
                max = rootOnLeft ? c + w : c;
                expected = c;
                final double root = c;
                f = new org.apache.commons.math.analysis.UnivariateRealFunction() {
                    public double value(double x) {
                        return x - root;
                    }
                };
            } catch (Throwable t) {
                return;
            }

            int mutations = data.consumeInt(1, 5);
            for (int i = 0; i < mutations; i++) {
                AllowedSolution chosen = AllowedSolution.values()[
                        data.consumeInt(0, AllowedSolution.values().length - 1)];
                try {
                    solver.solve(100, f, min, max, chosen);
                } catch (Throwable t) {
                    return;
                }

                double afterDefault;
                double afterExplicitAny;
                try {
                    afterDefault = solver.solve(100, f, min, max);
                    afterExplicitAny = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
                } catch (Throwable t) {
                    return;
                }

                // Hidden-state/state-coupling check: solve(..., allowedSolution) writes BaseSecantSolver.allowed,
                // while doSolve reads it. The constructors establish ANY_SIDE as the default, and the default
                // solve overload is documented to behave as ANY_SIDE regardless of prior explicit calls.
                // A patch that leaves stale receiver state would make the post-mutation default call differ.
                if (Double.doubleToLongBits(afterDefault) != Double.doubleToLongBits(afterExplicitAny)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:allowed-state-reset] metamorphic violation: default overload disagrees with explicit ANY_SIDE after prior state-changing solve"
                                    + " mutationIndex=" + i + " previousAllowed=" + chosen
                                    + " min=" + min + " max=" + max
                                    + " default=" + afterDefault + " explicitAny=" + afterExplicitAny);
                }

                // Oracle from known answer: the input is constructed from the chosen root c, so every correct
                // solver must return exactly that endpoint root on this problem.
                if (Double.doubleToLongBits(afterDefault) != Double.doubleToLongBits(expected)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:known-endpoint-root] semantic mismatch: constructed endpoint root not recovered"
                                    + " mutationIndex=" + i + " previousAllowed=" + chosen
                                    + " expected=" + expected + " actual=" + afterDefault
                                    + " min=" + min + " max=" + max);
                }
            }
        }
    }
}