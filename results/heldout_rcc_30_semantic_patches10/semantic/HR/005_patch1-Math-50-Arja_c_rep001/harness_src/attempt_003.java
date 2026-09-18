package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        RegulaFalsiSolver issueSolver = new RegulaFalsiSolver();
        UnivariateRealFunction issueFunction = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        boolean issueViolated = false;
        String issueWhat = null;
        try {
            double root = issueSolver.solve(3624, issueFunction, 1.0, 10.0);
            issueViolated = true;
            issueWhat = "completed normally with result=" + root;
        } catch (TooManyEvaluationsException expected) {
            // Ground-truth oracle lifted verbatim from RegulaFalsiSolverTest.testIssue631:
            // a correct implementation must throw TooManyEvaluationsException here.
        } catch (Throwable t) {
            issueViolated = true;
            issueWhat = "threw wrong exception=" + t.getClass().getName();
        }
        if (issueViolated) {
            throw new FuzzerSecurityIssueLow("[oracle:issue631-throw] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1,10) but " + issueWhat);
        }

        final int root1 = data.consumeInt(-1000, 1000);
        final int width1 = data.consumeInt(1, 1000);
        final int root2 = data.consumeInt(-1000, 1000);
        final int width2 = data.consumeInt(1, 1000);
        final AllowedSolution[] allowedValues = AllowedSolution.values();
        final AllowedSolution allowed1 = allowedValues[data.consumeInt(0, allowedValues.length - 1)];
        final AllowedSolution allowed2 = allowedValues[data.consumeInt(0, allowedValues.length - 1)];

        final double min1 = root1;
        final double max1 = root1 + width1;
        final double start1 = min1 + 0.5 * (max1 - min1);

        final double min2 = root2;
        final double max2 = root2 + width2;
        final double start2 = min2 + 0.5 * (max2 - min2);

        RegulaFalsiSolver solver = new RegulaFalsiSolver();

        Double result1 = null;
        boolean firstOk = false;
        try {
            final double a = min1;
            UnivariateRealFunction f1 = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - a;
                }
            };
            result1 = solver.solve(50, f1, min1, max1, start1, allowed1);
            firstOk = true;
        } catch (Throwable t) {
            return;
        }

        // Contract from doSolve shown above: if f(min) == 0.0, the solver returns min immediately,
        // regardless of AllowedSolution. We construct f(x)=x-a with min=a, so the exact endpoint root is known.
        if (firstOk && result1.doubleValue() != min1) {
            throw new FuzzerSecurityIssueLow("[oracle:endpoint-root] semantic mismatch: expected exact left-endpoint root=" + min1 + " actual=" + result1);
        }

        // Reader/writer consistency on solver state:
        // solve(..., min, max, ...) installs the search interval that doSolve later reads through getMin/getMax.
        // After the state-changing call returns, getMin/getMax must report the same interval that was just set.
        double reportedMin1 = solver.getMin();
        double reportedMax1 = solver.getMax();
        if (reportedMin1 != min1 || reportedMax1 != max1) {
            throw new FuzzerSecurityIssueLow("[oracle:bounds-state] consistency violation: after first solve expected min=" + min1 + " max=" + max1 + " but getMin()=" + reportedMin1 + " getMax()=" + reportedMax1);
        }

        Double result2 = null;
        boolean secondOk = false;
        try {
            final double b = min2;
            UnivariateRealFunction f2 = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - b;
                }
            };
            result2 = solver.solve(50, f2, min2, max2, start2, allowed2);
            secondOk = true;
        } catch (Throwable t) {
            return;
        }

        if (secondOk && result2.doubleValue() != min2) {
            throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-2] semantic mismatch: expected exact left-endpoint root=" + min2 + " actual=" + result2);
        }

        double reportedMin2 = solver.getMin();
        double reportedMax2 = solver.getMax();
        if (reportedMin2 != min2 || reportedMax2 != max2) {
            throw new FuzzerSecurityIssueLow("[oracle:bounds-state-2] consistency violation: after second solve expected min=" + min2 + " max=" + max2 + " but getMin()=" + reportedMin2 + " getMax()=" + reportedMax2);
        }
    }
}