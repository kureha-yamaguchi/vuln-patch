package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Oracle lifted exactly from RegulaFalsiSolverTest.testIssue631:
        // for f(x)=exp(x)-pi^3 on [1,10] with maxEval=3624, solve(...) must throw
        // TooManyEvaluationsException. Returning normally is the documented bug.
        {
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            boolean violated = false;
            String message = "completed normally";
            Throwable wrong = null;
            try {
                final double root = solver.solve(3624 + data.consumeInt(0, 0), f, 1.0, 10.0);
                violated = true;
                message = "expected org.apache.commons.math.exception.TooManyEvaluationsException but returned root=" + root;
            } catch (TooManyEvaluationsException expected) {
                // Expected by the failing test.
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                message = "expected org.apache.commons.math.exception.TooManyEvaluationsException but got " + t.getClass().getName();
            }
            if (violated) {
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow("[oracle:issue631-exception] semantic mismatch: " + message, wrong);
                }
                throw new FuzzerSecurityIssueLow("[oracle:issue631-exception] semantic mismatch: " + message);
            }
        }

        // Metamorphic / sibling-agreement check:
        // BracketedUnivariateRealSolver documents ANY_SIDE as the backwards-compatible default.
        // Therefore solve(maxEval,f,min,max) must agree with solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE)
        // on the same valid problem. This also checks the constructor-established allowed=ANY_SIDE state.
        {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean leftRoot = data.consumeBoolean();
            final double min = leftRoot ? c : c - w;
            final double max = leftRoot ? c + w : c;
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            Double rDefault = null;
            Double rAny = null;
            Throwable problem = null;
            try {
                rDefault = solver.solve(100, f, min, max);
                rAny = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                problem = t;
            }
            if (problem == null) {
                if (Double.doubleToLongBits(rDefault.doubleValue()) != Double.doubleToLongBits(rAny.doubleValue())) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:default-any-side] metamorphic violation: default solve and explicit ANY_SIDE disagree inputC="
                            + c + " min=" + min + " max=" + max + " lhs=" + rDefault + " rhs=" + rAny);
                }
            }

            // Re-probe after state changes to the same receiver:
            // solve(..., allowedSolution) writes the shared 'allowed' state; a correct implementation's
            // later default solve must still behave as ANY_SIDE, not as the previously written setting.
            final AllowedSolution firstMutation = data.consumeBoolean() ? AllowedSolution.LEFT_SIDE : AllowedSolution.RIGHT_SIDE;
            final AllowedSolution secondMutation = data.consumeBoolean() ? AllowedSolution.ABOVE_SIDE : AllowedSolution.BELOW_SIDE;
            try {
                solver.solve(100, f, min, max, firstMutation);
                solver.solve(100, f, min, max, secondMutation);
            } catch (Throwable t) {
                return;
            }

            final double c2 = data.consumeInt(-1000, 1000);
            final int w2 = data.consumeInt(1, 1000);
            final boolean leftRoot2 = data.consumeBoolean();
            final double min2 = leftRoot2 ? c2 : c2 - w2;
            final double max2 = leftRoot2 ? c2 + w2 : c2;
            final UnivariateRealFunction f2 = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c2;
                }
            };

            Double rDefault2 = null;
            Double rAny2 = null;
            Throwable problem2 = null;
            try {
                rDefault2 = solver.solve(100, f2, min2, max2);
                rAny2 = solver.solve(100, f2, min2, max2, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                problem2 = t;
            }
            if (problem2 == null) {
                if (Double.doubleToLongBits(rDefault2.doubleValue()) != Double.doubleToLongBits(rAny2.doubleValue())) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:allowed-state-reset] metamorphic violation: after explicit allowedSolution mutations, default solve no longer matches explicit ANY_SIDE inputC="
                            + c2 + " min=" + min2 + " max=" + max2 + " lhs=" + rDefault2 + " rhs=" + rAny2);
                }
            }
        }

        // Additional sibling-agreement across the same-name solve overloads:
        // On a valid problem with an exact endpoint root, all three public overloads solve the same equation
        // and must return that same exact endpoint root; the result is fixed by construction, not guessed.
        {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean rootAtLeft = data.consumeBoolean();
            final double min = rootAtLeft ? c : c - w;
            final double max = rootAtLeft ? c + w : c;
            final double start = rootAtLeft ? c + (w / 2.0) : c - (w / 2.0);
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            Double r1 = null;
            Double r2 = null;
            Double r3 = null;
            Throwable problem = null;
            try {
                r1 = solver.solve(100, f, min, max);
                r2 = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
                r3 = solver.solve(100, f, min, max, start);
            } catch (Throwable t) {
                problem = t;
            }
            if (problem == null) {
                long expectedBits = Double.doubleToLongBits(c);
                if (Double.doubleToLongBits(r1.doubleValue()) != expectedBits) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:endpoint-root-overload1] semantic mismatch: expected root=" + c + " actual=" + r1
                            + " min=" + min + " max=" + max + " start=" + start);
                }
                if (Double.doubleToLongBits(r2.doubleValue()) != expectedBits) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:endpoint-root-overload2] semantic mismatch: expected root=" + c + " actual=" + r2
                            + " min=" + min + " max=" + max + " start=" + start);
                }
                if (Double.doubleToLongBits(r3.doubleValue()) != expectedBits) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:endpoint-root-overload3] semantic mismatch: expected root=" + c + " actual=" + r3
                            + " min=" + min + " max=" + max + " start=" + start);
                }
            }
        }
    }
}