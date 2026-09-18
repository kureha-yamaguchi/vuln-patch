package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631 = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        try {
            new RegulaFalsiSolver().solve(3624, issue631, 1.0, 10.0);
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631-exact] semantic mismatch: expected TooManyEvaluationsException for solve(3624, exp(x)-PI^3, 1, 10) but call returned normally");
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        double center = data.consumeInt(-10000, 10000) / 16.0;
        double leftSpan = 1.0 + (data.consumeInt(0, 10000) / 32.0);
        double rightSpan = 1.0 + (data.consumeInt(0, 10000) / 32.0);
        final double min = center - leftSpan;
        final double max = center + rightSpan;
        final double root = center;
        final double start = min + (max - min) * 0.5;

        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - root;
            }
        };

        RegulaFalsiSolver solverA = new RegulaFalsiSolver();
        RegulaFalsiSolver solverB = new RegulaFalsiSolver();

        double noAllowed;
        double anySide;
        try {
            noAllowed = solverA.solve(128, linear, min, max);
            anySide = solverB.solve(128, linear, min, max, AllowedSolution.ANY_SIDE);
        } catch (RuntimeException ex) { /*__vpRepair*/ if (ex instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ex;
            return;
        }

        if (Double.doubleToLongBits(noAllowed) != Double.doubleToLongBits(anySide)) {
            throw new RuntimeException(
                "[oracle:default-any-side] metamorphic violation: documented default allowed solution is ANY_SIDE, so equivalent overloads must agree input=min=" +
                min + ",max=" + max + ",root=" + root + " lhs=" + noAllowed + " rhs=" + anySide);
        }

        RegulaFalsiSolver solverC = new RegulaFalsiSolver();
        RegulaFalsiSolver solverD = new RegulaFalsiSolver();

        double withStartDefault;
        double withStartAnySide;
        try {
            withStartDefault = solverC.solve(128, linear, min, max, start);
            withStartAnySide = solverD.solve(128, linear, min, max, start, AllowedSolution.ANY_SIDE);
        } catch (RuntimeException ex) { /*__vpRepair*/ if (ex instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ex;
            return;
        }

        if (Double.doubleToLongBits(withStartDefault) != Double.doubleToLongBits(withStartAnySide)) {
            throw new RuntimeException(
                "[oracle:start-overload-any-side] metamorphic violation: equivalent solve overloads with identical startValue must agree because constructors initialize allowed to ANY_SIDE by default input=min=" +
                min + ",max=" + max + ",start=" + start + ",root=" + root + " lhs=" + withStartDefault + " rhs=" + withStartAnySide);
        }
    }
}