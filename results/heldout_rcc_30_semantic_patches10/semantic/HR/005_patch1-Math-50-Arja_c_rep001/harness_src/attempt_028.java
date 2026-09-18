package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkIssue631PinnedBehavior();
        checkExactInteriorRootAllAllowed(data);
        checkGetterConsistencyAfterSuccessfulSolve(data);
    }

    private static void checkIssue631PinnedBehavior() {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };
        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        boolean violated = false;
        String what = null;
        try {
            double root = solver.solve(3624, f, 1.0, 10.0);
            violated = true;
            what = "completed normally with result=" + root;
        } catch (TooManyEvaluationsException expected) {
            return;
        } catch (Throwable t) {
            violated = true;
            what = "threw wrong exception=" + t.getClass().getName();
        }

        if (violated) {
            throw new FuzzerSecurityIssueLow("[oracle:issue631-ground-truth] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1,10) but " + what);
        }
    }

    private static void checkExactInteriorRootAllAllowed(FuzzedDataProvider data) {
        final int ai = data.consumeInt(-1000, 1000);
        final int left = data.consumeInt(1, 1000);
        final int right = data.consumeInt(1, 1000);
        final double a = (double) ai;
        final double min = a - left;
        final double max = a + right;
        final double start = a + ((double) (right - left)) / 3.0;

        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return x - a;
            }
        };

        final AllowedSolution[] values = AllowedSolution.values();
        for (int i = 0; i < values.length; i++) {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final AllowedSolution allowed = values[i];
            final double result;
            try {
                result = solver.solve(50, f, min, max, start, allowed);
            } catch (Throwable t) {
                return;
            }

            // Contract from doSolve: if the newly computed approximation is the exact root (`fx == 0.0`),
            // it is returned regardless of the allowed solution side. For f(x)=x-a on a bracket [a-left,a+right],
            // the regula-falsi step computes x=a exactly, so every AllowedSolution must return the same exact root.
            if (result != a) {
                throw new FuzzerSecurityIssueLow("[oracle:exact-root-all-allowed] metamorphic violation: exact interior root must be returned for every AllowedSolution inputRoot=" + a + " min=" + min + " max=" + max + " start=" + start + " allowed=" + allowed + " actual=" + result);
            }
        }
    }

    private static void checkGetterConsistencyAfterSuccessfulSolve(FuzzedDataProvider data) {
        final int ai = data.consumeInt(-1000, 1000);
        final int left = data.consumeInt(1, 1000);
        final int right = data.consumeInt(1, 1000);
        final double absAcc = Math.pow(10.0, -data.consumeInt(3, 8));
        final double relAcc = Math.pow(10.0, -data.consumeInt(3, 8));
        final double fAcc = Math.pow(10.0, -data.consumeInt(3, 8));

        final double a = (double) ai;
        final double min = a - left;
        final double max = a + right;
        final double start = a + ((double) (right - left)) / 4.0;

        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return x - a;
            }
        };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver(relAcc, absAcc, fAcc);

        final double result;
        try {
            result = solver.solve(50, f, min, max, start, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        final double reportedMin = solver.getMin();
        final double reportedMax = solver.getMax();
        final double reportedAbs = solver.getAbsoluteAccuracy();
        final double reportedRel = solver.getRelativeAccuracy();
        final double reportedF = solver.getFunctionValueAccuracy();

        // Consistency cross-check on masked helpers: getMin/getMax and the accuracy getters are direct readers of
        // state established by solve/constructor. A band-aid in the iteration logic can still leave these helpers
        // inconsistent even when the returned root looks fine. The reported bounds must equal the arguments just
        // installed for this solve, and the returned exact root for f(x)=x-a must lie within those same reported bounds.
        if (reportedMin != min || reportedMax != max) {
            throw new FuzzerSecurityIssueLow("[oracle:reported-bounds-consistency] consistency violation: passedMin=" + min + " passedMax=" + max + " reportedMin=" + reportedMin + " reportedMax=" + reportedMax + " result=" + result);
        }
        if (reportedAbs != absAcc || reportedRel != relAcc || reportedF != fAcc) {
            throw new FuzzerSecurityIssueLow("[oracle:reported-accuracy-consistency] consistency violation: expectedAbs=" + absAcc + " expectedRel=" + relAcc + " expectedF=" + fAcc + " reportedAbs=" + reportedAbs + " reportedRel=" + reportedRel + " reportedF=" + reportedF);
        }
        if (!(reportedMin <= result && result <= reportedMax) || result != a) {
            throw new FuzzerSecurityIssueLow("[oracle:root-within-reported-bounds] consistency violation: root=" + result + " expected=" + a + " reportedMin=" + reportedMin + " reportedMax=" + reportedMax);
        }
    }
}