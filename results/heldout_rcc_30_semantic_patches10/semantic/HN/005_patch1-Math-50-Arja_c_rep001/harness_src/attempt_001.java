package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final int sink = data.consumeInt(0, 0);

        // Lifted oracle from RegulaFalsiSolverTest.testIssue631:
        // for this exact public-API call, the documented solve contract says a TooManyEvaluationsException
        // must be thrown when maxEval is exceeded; the upstream test pins that this exact setup must throw.
        {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };

            boolean violation = false;
            String message = "completed normally";
            try {
                double root = solver.solve(3624 + sink, f, 1.0, 10.0);
                violation = true;
                message = "completed normally with root=" + root;
            } catch (TooManyEvaluationsException expected) {
                // expected
            } catch (Throwable t) {
                violation = true;
                message = "threw wrong exception " + t.getClass().getName() + ": " + t.getMessage();
            }
            if (violation) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:issue631-must-throw] semantic mismatch: " + message +
                    " expected=org.apache.commons.math.exception.TooManyEvaluationsException"
                );
            }
        }

        // Metamorphic sibling-agreement check:
        // BracketedUnivariateRealSolver documents ANY_SIDE as the default allowed solution for backwards compatibility,
        // so solve(maxEval,f,min,max) and solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) must agree.
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

            double rDefault;
            try {
                rDefault = solver.solve(100, f, min, max);
            } catch (Throwable t) {
                return;
            }

            double rExplicit;
            try {
                rExplicit = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicit)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:default-vs-explicit-any-side] metamorphic violation: " +
                    "solve(maxEval,f,min,max) != solve(maxEval,f,min,max,ANY_SIDE)" +
                    " min=" + min + " max=" + max + " c=" + c +
                    " lhs=" + rDefault + " rhs=" + rExplicit
                );
            }
        }

        // Additional sibling-agreement/post-state check:
        // The overload with startValue but without AllowedSolution is also documented to use the default ANY_SIDE.
        // We call the explicit-ANY_SIDE overload first to mutate the solver's internal `allowed` state, then re-probe
        // with the default overload on the same valid problem. A correct implementation must still behave as ANY_SIDE.
        {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(2, 1000);
            final double min = c - w;
            final double max = c + w;
            final double startValue = c + data.consumeInt(-w + 1, w - 1);
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            try {
                solver.solve(100, f, min, max, startValue, AllowedSolution.LEFT_SIDE);
            } catch (Throwable t) {
                return;
            }

            double rDefault;
            try {
                rDefault = solver.solve(100, f, min, max, startValue);
            } catch (Throwable t) {
                return;
            }

            double rExplicitAny;
            try {
                rExplicitAny = solver.solve(100, f, min, max, startValue, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicitAny)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:start-overload-default-any-side] metamorphic violation: " +
                    "solve(maxEval,f,min,max,startValue) != solve(maxEval,f,min,max,startValue,ANY_SIDE)" +
                    " min=" + min + " max=" + max + " startValue=" + startValue + " c=" + c +
                    " lhs=" + rDefault + " rhs=" + rExplicitAny
                );
            }
        }
    }
}