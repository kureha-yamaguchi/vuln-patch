package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int fixedMaxEval = 3624 + data.consumeInt(0, 0);
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final RegulaFalsiSolver fixedSolver = new RegulaFalsiSolver();
        boolean fixedViolation = false;
        String fixedMessage = null;
        Throwable fixedCause = null;
        double fixedRoot = Double.NaN;
        try {
            fixedRoot = fixedSolver.solve(fixedMaxEval, issue631Function, 1.0, 10.0);
            fixedViolation = true;
            fixedMessage = "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but completed normally with root=" + fixedRoot;
        } catch (TooManyEvaluationsException expected) {
        } catch (Throwable t) {
            fixedViolation = true;
            fixedCause = t;
            fixedMessage = "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but caught " + t.getClass().getName();
        }
        if (fixedViolation) {
            if (fixedCause != null) {
                throw new FuzzerSecurityIssueLow(fixedMessage, fixedCause);
            }
            throw new FuzzerSecurityIssueLow(fixedMessage);
        }

        final int c = data.consumeInt(-1000, 1000);
        final int w = data.consumeInt(1, 1000);
        final boolean leftRoot = data.consumeBoolean();
        final double min = leftRoot ? c : c - w;
        final double max = leftRoot ? c + w : c;
        final double startValue = (min + max) / 2.0;
        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c;
            }
        };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        double rDefault;
        try {
            rDefault = solver.solve(100, linear, min, max);
        } catch (Throwable t) {
            return;
        }

        double rExplicitAny;
        try {
            rExplicitAny = solver.solve(100, linear, min, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicitAny)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-vs-explicit-any] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,ANY_SIDE) inputC=" +
                c + " min=" + min + " max=" + max + " lhs=" + rDefault + " rhs=" + rExplicitAny
            );
        }

        double rDefaultStart;
        try {
            rDefaultStart = solver.solve(100, linear, min, max, startValue);
        } catch (Throwable t) {
            return;
        }

        double rExplicitAnyStart;
        try {
            rExplicitAnyStart = solver.solve(100, linear, min, max, startValue, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(rDefaultStart) != Double.doubleToLongBits(rExplicitAnyStart)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:start-default-vs-explicit-any] metamorphic violation: solve(maxEval,f,min,max,startValue) must equal solve(maxEval,f,min,max,startValue,ANY_SIDE) inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue + " lhs=" + rDefaultStart + " rhs=" + rExplicitAnyStart
            );
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rDefaultStart)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:overload-agreement-endpoint-root] metamorphic violation: endpoint exact-root problem must return the same root across overloads inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue + " lhs=" + rDefault + " rhs=" + rDefaultStart
            );
        }

        final AllowedSolution chosenAllowed = AllowedSolution.values()[data.consumeInt(0, AllowedSolution.values().length - 1)];

        double rChosen;
        try {
            rChosen = solver.solve(100, linear, min, max, startValue, chosenAllowed);
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(rChosen) != Double.doubleToLongBits((double) c)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:known-answer-endpoint-root] semantic mismatch: for f(x)=x-c with c as an endpoint, the solver must return that exact endpoint root inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue + " allowed=" + chosenAllowed + " actual=" + rChosen
            );
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits((double) c) ||
            Double.doubleToLongBits(rExplicitAny) != Double.doubleToLongBits((double) c) ||
            Double.doubleToLongBits(rDefaultStart) != Double.doubleToLongBits((double) c) ||
            Double.doubleToLongBits(rExplicitAnyStart) != Double.doubleToLongBits((double) c)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:known-answer-all-overloads] semantic mismatch: endpoint-root construction fixes the trusted answer to c inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue +
                " default=" + rDefault + " explicitAny=" + rExplicitAny +
                " defaultStart=" + rDefaultStart + " explicitAnyStart=" + rExplicitAnyStart
            );
        }

        final int c2 = data.consumeInt(-1000, 1000);
        final int width2 = data.consumeInt(2, 1000);
        final double min2 = c2 - width2;
        final double max2 = c2 + width2;
        final UnivariateRealFunction symmetricLinear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c2;
            }
        };

        double leftSideRoot;
        double rightSideRoot;
        try {
            leftSideRoot = solver.solve(100, symmetricLinear, min2, max2, c2, AllowedSolution.LEFT_SIDE);
            rightSideRoot = solver.solve(100, symmetricLinear, min2, max2, c2, AllowedSolution.RIGHT_SIDE);
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(leftSideRoot) != Double.doubleToLongBits(rightSideRoot) ||
            Double.doubleToLongBits(leftSideRoot) != Double.doubleToLongBits((double) c2)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:allowed-sides-agree-on-exact-root] metamorphic violation: when the exact root is returned, allowed side cannot change the answer inputC=" +
                c2 + " min=" + min2 + " max=" + max2 + " left=" + leftSideRoot + " right=" + rightSideRoot
            );
        }
    }
}