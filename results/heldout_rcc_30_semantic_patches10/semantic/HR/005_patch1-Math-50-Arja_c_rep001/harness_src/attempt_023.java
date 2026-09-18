package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    private static final UnivariateRealFunction ISSUE631_FUNCTION = new UnivariateRealFunction() {
        public double value(double x) {
            return Math.exp(x) - Math.pow(Math.PI, 3.0);
        }
    };

    private static final double EXPECTED_ROOT = 3.4341896575482003;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int extraEval = data.consumeInt(0, 4096);
        int maxEval = 3624 + extraEval;

        double min1 = 1.0;
        double max1 = 10.0;

        double min2 = data.consumeBoolean() ? data.consumeInt(-20, 3) : data.consumeInt(0, 3);
        double max2 = data.consumeBoolean() ? data.consumeInt(4, 20) : data.consumeInt(4, 10);
        if (!(min2 < EXPECTED_ROOT && EXPECTED_ROOT < max2)) {
            min2 = 0.0;
            max2 = 8.0;
        }

        RegulaFalsiSolver seedSolver = new RegulaFalsiSolver();
        try {
            seedSolver.solve(maxEval, ISSUE631_FUNCTION, min1, max1);
        } catch (Throwable ignored) {
        }

        checkDefaultConstructorEquivalence(maxEval, min1, max1);
        checkUniqueRootBracketConsistency(maxEval, min1, max1, min2, max2);
    }

    private static void checkDefaultConstructorEquivalence(int maxEval, double min, double max) {
        RegulaFalsiSolver defaultCtor = new RegulaFalsiSolver();
        RegulaFalsiSolver explicitCtor = new RegulaFalsiSolver(1e-6);

        Double lhs = null;
        Double rhs = null;
        Throwable leftThrown = null;
        Throwable rightThrown = null;

        try {
            lhs = defaultCtor.solve(maxEval, ISSUE631_FUNCTION, min, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            leftThrown = t;
        }

        try {
            rhs = explicitCtor.solve(maxEval, ISSUE631_FUNCTION, min, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            rightThrown = t;
        }

        if (leftThrown != null || rightThrown != null) {
            if (leftThrown == null || rightThrown == null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:default-ctor-equiv] consistency violation: default constructor and explicit-accuracy constructor are documented to represent the same default absolute accuracy (1e-6), so on the same function and interval they must both reject or both solve; leftThrown=" +
                    className(leftThrown) + " rightThrown=" + className(rightThrown));
            }
            if (!leftThrown.getClass().equals(rightThrown.getClass())) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:default-ctor-equiv] consistency violation: equivalent constructors rejected differently; leftThrown=" +
                    leftThrown.getClass().getName() + " rightThrown=" + rightThrown.getClass().getName());
            }
            return;
        }

        if (Math.abs(lhs.doubleValue() - rhs.doubleValue()) > 1e-15) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:default-ctor-equiv] consistency violation: equivalent constructors produced different roots lhs=" +
                lhs + " rhs=" + rhs + " maxEval=" + maxEval + " min=" + min + " max=" + max);
        }
    }

    private static void checkUniqueRootBracketConsistency(int maxEval, double min1, double max1, double min2, double max2) {
        RegulaFalsiSolver solverA = new RegulaFalsiSolver();
        RegulaFalsiSolver solverB = new RegulaFalsiSolver();

        Double rootA = null;
        Double rootB = null;

        try {
            rootA = solverA.solve(maxEval, ISSUE631_FUNCTION, min1, max1, AllowedSolution.ANY_SIDE);
            rootB = solverB.solve(maxEval, ISSUE631_FUNCTION, min2, max2, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        if (rootA == null || rootB == null) {
            return;
        }

        double delta = Math.abs(rootA.doubleValue() - rootB.doubleValue());
        if (delta > 1e-12) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:unique-root-brackets] metamorphic violation: for f(x)=exp(x)-pi^3 the function is continuous and strictly increasing, so every valid bracketing interval contains the same unique zero; solving the same function from two different bracketing intervals must therefore yield the same root within solver accuracy rootA=" +
                rootA + " rootB=" + rootB + " delta=" + delta + " intervalA=[" + min1 + "," + max1 + "] intervalB=[" + min2 + "," + max2 + "] maxEval=" + maxEval);
        }
    }

    private static String className(Throwable t) {
        return t == null ? "null" : t.getClass().getName();
    }
}