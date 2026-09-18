package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    /*__vpCause*/ private static Throwable __vpCause = null;

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double pinnedBuggyRoot = 3.4341896575482003d;

        // Lifted oracle from RegulaFalsiSolverTest.testIssue631:
        // BracketedUnivariateRealSolver.solve(...) documents TooManyEvaluationsException
        // when max evaluations are exceeded, and the failing test pins that this exact
        // input must throw rather than return normally.
        boolean issue631Violated = false;
        String issue631What = null;
        try {
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            double r = solver.solve(3624, f, 1.0, 10.0);
            issue631Violated = true;
            issue631What = "completed normally with result=" + r;
            if (Double.doubleToLongBits(r) == Double.doubleToLongBits(pinnedBuggyRoot)) {
                issue631What += " pinnedBuggyResult=" + pinnedBuggyRoot;
            }
        } catch (TooManyEvaluationsException expected) { __vpCause = expected;
            issue631Violated = false;
        } catch (Throwable t) {
            issue631Violated = true;
            issue631What = "threw wrong exception=" + t.getClass().getName();
        }
        if (issue631Violated) {
            throw new FuzzerSecurityIssueLow("[oracle:issue631] semantic mismatch: " + issue631What, __vpCause);
        }

        // Sibling-agreement / hidden-state check:
        // BaseSecantSolver constructors set default allowed=ANY_SIDE, and the overloads are
        // documented to agree under explicit ANY_SIDE vs default behavior. Also, getMin/getMax
        // report the interval stored by solve(...), so after a successful solve they must match
        // the min/max just installed.
        final int ai = data.consumeInt(-1000, 1000);
        final int di = data.consumeInt(1, 1000);
        final double a = ai;
        final double min = a;
        final double max = ai + di;
        final double start = a + 0.5d * (max - min);

        boolean anySideViolated = false;
        String anySideWhat = null;
        try {
            final UnivariateRealFunction endpointRoot = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - a;
                }
            };

            final RegulaFalsiSolver explicitSolver = new RegulaFalsiSolver();
            final double explicit = explicitSolver.solve(50, endpointRoot, min, max, AllowedSolution.ANY_SIDE);

            final RegulaFalsiSolver defaultSolver = new RegulaFalsiSolver();
            final double implicit = defaultSolver.solve(50, endpointRoot, min, max, start);

            if (explicit != a || implicit != a || explicit != implicit) {
                anySideViolated = true;
                anySideWhat = "explicit=" + explicit + " implicit=" + implicit + " expected=" + a;
            } else if (defaultSolver.getMin() != min || defaultSolver.getMax() != max) {
                anySideViolated = true;
                anySideWhat = "storedIntervalMin=" + defaultSolver.getMin() + " storedIntervalMax=" + defaultSolver.getMax()
                        + " expectedMin=" + min + " expectedMax=" + max;
            }
        } catch (Throwable t) {
            return;
        }
        if (anySideViolated) {
            throw new FuzzerSecurityIssueLow("[oracle:anyside-state] consistency violation: " + anySideWhat);
        }

        // Additional trusted post-condition on a valid-by-construction root problem:
        // doSolve first checks exact endpoint roots and otherwise iterates until returning
        // an allowed solution; for f(x)=x-root over [root-1, root+1], all successful overloads
        // must agree on the same exact root. A patch that only suppresses/avoids the problematic
        // branch but perturbs solver state or method behavior breaks this agreement.
        final int rootInt = data.consumeInt(-1000, 1000);
        final double root = rootInt;
        final double min2 = root - 1.0d;
        final double max2 = root + 1.0d;
        final double start2 = root;

        boolean overloadViolated = false;
        String overloadWhat = null;
        try {
            final UnivariateRealFunction linear = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - root;
                }
            };

            final RegulaFalsiSolver s1 = new RegulaFalsiSolver();
            final double r1 = s1.solve(50, linear, min2, max2);

            final RegulaFalsiSolver s2 = new RegulaFalsiSolver();
            final double r2 = s2.solve(50, linear, min2, max2, start2);

            final RegulaFalsiSolver s3 = new RegulaFalsiSolver();
            final double r3 = s3.solve(50, linear, min2, max2, start2, AllowedSolution.ANY_SIDE);

            if (r1 != root || r2 != root || r3 != root) {
                overloadViolated = true;
                overloadWhat = "r1=" + r1 + " r2=" + r2 + " r3=" + r3 + " expected=" + root;
            } else {
                double v1 = linear.value(r1);
                double v2 = linear.value(r2);
                double v3 = linear.value(r3);
                if (v1 != 0.0d || v2 != 0.0d || v3 != 0.0d) {
                    overloadViolated = true;
                    overloadWhat = "f(r1)=" + v1 + " f(r2)=" + v2 + " f(r3)=" + v3;
                }
            }
        } catch (Throwable t) {
            return;
        }
        if (overloadViolated) {
            throw new FuzzerSecurityIssueLow("[oracle:overload-agreement] metamorphic violation: " + overloadWhat);
        }
    }
}