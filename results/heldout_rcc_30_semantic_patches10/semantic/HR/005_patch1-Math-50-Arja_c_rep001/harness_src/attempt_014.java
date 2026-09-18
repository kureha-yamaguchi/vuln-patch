package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        reachPatchedPath(data);
        checkConstructorAgreementOnKnownInteriorRoot(data);
        checkTranslatedPathologyAgreement(data);
    }

    private static void reachPatchedPath(FuzzedDataProvider data) {
        final int shift = data.consumeInt(-20, 20);
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x - shift) - Math.pow(Math.PI, 3.0);
            }
        };
        try {
            new RegulaFalsiSolver().solve(3624, f, 1.0 + shift, 10.0 + shift);
        } catch (Throwable ignored) {
        }
    }

    private static void checkConstructorAgreementOnKnownInteriorRoot(FuzzedDataProvider data) {
        final int ai = data.consumeInt(-1000, 1000);
        final int left = data.consumeInt(1, 1000);
        final int right = data.consumeInt(1, 1000);
        final double a = ai;
        final double min = a - left;
        final double max = a + right;
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return x - a;
            }
        };

        final RegulaFalsiSolver s0;
        final RegulaFalsiSolver s1;
        final RegulaFalsiSolver s2;
        try {
            s0 = new RegulaFalsiSolver();
            s1 = new RegulaFalsiSolver(1.0e-6);
            s2 = new RegulaFalsiSolver(1.0e-12, 1.0e-6);
        } catch (Throwable t) {
            return;
        }

        final double r0;
        final double r1;
        final double r2;
        try {
            r0 = s0.solve(10, f, min, max);
            r1 = s1.solve(10, f, min, max);
            r2 = s2.solve(10, f, min, max);
        } catch (Throwable t) {
            return;
        }

        if (r0 != a || r1 != a || r2 != a) {
            throw new FuzzerSecurityIssueLow("[oracle:constructor-agreement] semantic mismatch: known linear interior root expected=" + a + " actualDefault=" + r0 + " actualAbsOnly=" + r1 + " actualRelAbs=" + r2);
        }
        if (r0 != r1 || r1 != r2) {
            throw new FuzzerSecurityIssueLow("[oracle:constructor-agreement] metamorphic violation: constructor overloads disagree on same valid problem expectedAll=" + a + " default=" + r0 + " absOnly=" + r1 + " relAbs=" + r2);
        }
    }

    private static void checkTranslatedPathologyAgreement(FuzzedDataProvider data) {
        final int shift1 = data.consumeInt(-10, 10);
        final int delta = data.consumeInt(1, 5);
        final int shift2 = shift1 + delta;

        final UnivariateRealFunction f1 = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x - shift1) - Math.pow(Math.PI, 3.0);
            }
        };
        final UnivariateRealFunction f2 = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x - shift2) - Math.pow(Math.PI, 3.0);
            }
        };

        final RegulaFalsiSolver s1 = new RegulaFalsiSolver();
        final RegulaFalsiSolver s2 = new RegulaFalsiSolver();

        final double r1;
        final double r2;
        try {
            r1 = s1.solve(5000, f1, 1.0 + shift1, 10.0 + shift1);
            r2 = s2.solve(5000, f2, 1.0 + shift2, 10.0 + shift2);
        } catch (TooManyEvaluationsException e) {
            return;
        } catch (Throwable t) {
            return;
        }

        final double expectedDelta = shift2 - shift1;
        final double actualDelta = r2 - r1;
        final double tol = 1e-12 * Math.max(1.0, Math.max(Math.abs(r1), Math.abs(r2)));

        /*
         * Contract/justification: both calls solve the same real problem up to an additive
         * translation of x. For g_s(x) = exp(x - s) - PI^3, we have g_s(y + s) = exp(y) - PI^3,
         * so the regula-falsi/secant updates, which depend only on x positions and scaled function
         * values, must translate by the same amount when both solves return normally.
         * A patch that merely silences the known throw symptom or special-cases one seed can still
         * leave the x==x1 stagnation logic wrong on nearby translated instances; then the returned
         * roots need not preserve this exact translation relation.
         */
        if (Math.abs(actualDelta - expectedDelta) > tol) {
            throw new FuzzerSecurityIssueLow("[oracle:translation-invariance] metamorphic violation: translated equivalent solves disagreed shift1=" + shift1 + " shift2=" + shift2 + " root1=" + r1 + " root2=" + r2 + " expectedDelta=" + expectedDelta + " actualDelta=" + actualDelta);
        }

        final double residual1 = Math.abs(f1.value(r1));
        final double residual2 = Math.abs(f2.value(r2));
        if (!(residual1 <= 1e-8 && residual2 <= 1e-8)) {
            throw new FuzzerSecurityIssueLow("[oracle:translation-invariance-residual] consistency violation: returned roots from translated equivalent solves are not roots root1=" + r1 + " residual1=" + residual1 + " root2=" + r2 + " residual2=" + residual2);
        }
    }
}