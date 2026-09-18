package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            boolean violated = false;
            double returned = Double.NaN;
            Throwable wrong = null;

            try {
                returned = solver.solve(3624 + data.consumeInt(0, 0), f, 1.0, 10.0);
                violated = true;
            } catch (TooManyEvaluationsException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
            }

            if (violated) {
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException but got "
                            + wrong.getClass().getName(),
                        wrong);
                }
                final double buggyRoot = 3.4341896575482003;
                if (Double.doubleToLongBits(returned) != Double.doubleToLongBits(buggyRoot)) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException; call returned "
                            + returned + " (setup divergence from failing test, expected buggy return " + buggyRoot + ")");
                }
                throw new FuzzerSecurityIssueLow(
                    "[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException but call returned buggy root "
                        + returned);
            }
        }

        {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
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

            double r1;
            double r2;
            try {
                r1 = solver.solve(100, f, min, max);
                r2 = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(r2)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must match solve(maxEval,f,min,max,ANY_SIDE) "
                        + "for the same valid problem; c=" + c + " min=" + min + " max=" + max + " lhs=" + r1 + " rhs=" + r2);
            }
        }

        {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean rootAtLeft = data.consumeBoolean();
            final double min = rootAtLeft ? c : c - w;
            final double max = rootAtLeft ? c + w : c;
            final double startValue = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            double noStart;
            double withStart;
            double withStartAny;
            try {
                noStart = solver.solve(100, f, min, max);
                withStart = solver.solve(100, f, min, max, startValue);
                withStartAny = solver.solve(100, f, min, max, startValue, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            /* Contract justification:
               - Constructors initialize allowed to ANY_SIDE.
               - The overload without AllowedSolution is documented to use ANY_SIDE for backwards compatibility.
               - For a valid problem whose root is exactly at one endpoint, all overloads must return that exact endpoint root. */
            if (Double.doubleToLongBits(noStart) != Double.doubleToLongBits(c)
                    || Double.doubleToLongBits(withStart) != Double.doubleToLongBits(c)
                    || Double.doubleToLongBits(withStartAny) != Double.doubleToLongBits(c)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:endpoint-root-overloads] metamorphic violation: endpoint root must be preserved across solve overloads "
                        + "c=" + c + " min=" + min + " max=" + max + " startValue=" + startValue
                        + " noStart=" + noStart + " withStart=" + withStart + " withStartAny=" + withStartAny);
            }
        }
    }
}