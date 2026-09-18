package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final UnivariateRealSolver solver = new RegulaFalsiSolver();
        try {
            double root = solver.solve(3624, issue631Function, 1, 10);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:issue631] semantic mismatch: expected TooManyEvaluationsException but solve returned root=" + root
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        final double knownRoot = 2.0;
        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - knownRoot;
            }
        };

        double min = 0.0;
        double max = 4.0;
        double start = 1.0 + (data.consumeInt(0, 2000) / 1000.0);

        RegulaFalsiSolver s1 = new RegulaFalsiSolver();
        RegulaFalsiSolver s2 = new RegulaFalsiSolver();

        try {
            double lhs = s1.solve(32, linear, min, max, start);
            double rhs = s2.solve(32, linear, min, max, start, AllowedSolution.ANY_SIDE);

            if (Double.doubleToLongBits(lhs) != Double.doubleToLongBits(rhs)) {
                throw new RuntimeException(
                    "[oracle:any-side-overload] metamorphic violation: solve(maxEval,f,min,max,startValue) must agree with " +
                    "solve(maxEval,f,min,max,startValue,AllowedSolution.ANY_SIDE) because BaseSecantSolver constructors set " +
                    "default allowed solution to ANY_SIDE; input=start=" + start + " lhs=" + lhs + " rhs=" + rhs
                );
            }

            if (Double.doubleToLongBits(lhs) != Double.doubleToLongBits(knownRoot)) {
                throw new RuntimeException(
                    "[oracle:constructed-root] metamorphic violation: for f(x)=x-c, the real root is the chosen constant c; " +
                    "input=c=" + knownRoot + " result=" + lhs
                );
            }
        } catch (RuntimeException ex) { /*__vpRepair*/ if (ex instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ex;
            String name = ex.getClass().getName();
            if (name.indexOf("Math") >= 0 || name.indexOf("Evaluation") >= 0 || name.indexOf("Convergence") >= 0) {
                return;
            }
            throw ex;
        }
    }
}