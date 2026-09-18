package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.distribution.NormalDistributionImpl anchorDist =
                new org.apache.commons.math.distribution.NormalDistributionImpl(0, 1);

        // ANCHOR: exact failing test input from NormalDistributionTest.testMath280.
        try {
            double anchorResult = anchorDist.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(anchorResult - 2.0d) > 1.0e-12d) {
                throw new RuntimeException(
                        "[oracle:anchor] metamorphic violation: exact regression seed should round-trip to 2.0 inputP=0.9772498680518209 result="
                                + anchorResult);
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException
                            || t instanceof NumberFormatException;
            boolean hasBracket = false;
            for (Throwable c = t; c != null && !hasBracket; c = c.getCause()) {
                StackTraceElement[] st = c.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(st[i].getClassName())
                                && "bracket".equals(st[i].getMethodName())) {
                            hasBracket = true;
                            break;
                        }
                    }
                }
            }
            if (!cleanRejection && t instanceof org.apache.commons.math.MathException && hasBracket) {
                throw new RuntimeException(t);
            }
        }

        // EXPLORE:
        // Root cause property: the quantile equals a bracketing endpoint exactly, so fa*fb == 0.0.
        // Build valid inputs by construction through the real public API:
        // choose mean, sd, and an integer endpoint target = mean + k*sd, then p = CDF(target).
        // For any correct implementation, inverseCumulativeProbability(CDF(x)) ~= x.
        double mean = data.consumeInt(-50, 50);
        double sd = data.consumeInt(1, 20);
        int kChoice = data.consumeInt(0, 5);
        int k;
        switch (kChoice) {
            case 0:
                k = 2;   // exact buggy seed shape
                break;
            case 1:
                k = 0;   // lower endpoint possibility
                break;
            case 2:
                k = 1;
                break;
            case 3:
                k = -1;
                break;
            case 4:
                k = -2;
                break;
            default:
                k = data.consumeInt(-6, 6);
                break;
        }

        org.apache.commons.math.distribution.NormalDistributionImpl dist =
                new org.apache.commons.math.distribution.NormalDistributionImpl(mean, sd);

        double x = mean + ((double) k) * sd;
        double p;
        try {
            p = dist.cumulativeProbability(x);
        } catch (Throwable t) {
            return;
        }

        if (!(p > 0.0d && p < 1.0d) || Double.isNaN(p)) {
            return;
        }

        try {
            double inv = dist.inverseCumulativeProbability(p);

            // Mandatory post-condition / metamorphic oracle:
            // For a correct distribution implementation, inverseCumulativeProbability is the inverse
            // of cumulativeProbability on valid probabilities. Deleting the patched throw or otherwise
            // "fixing" by returning the wrong value would violate this observable round-trip.
            double tol = 1.0e-9d * Math.max(1.0d, Math.abs(x));
            if (Math.abs(inv - x) > tol) {
                throw new RuntimeException(
                        "[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) == x"
                                + " mean=" + mean
                                + " sd=" + sd
                                + " k=" + k
                                + " x=" + x
                                + " p=" + p
                                + " inv=" + inv
                                + " tol=" + tol);
            }

            // Also exercise the patched function directly via the real library type, using the
            // same endpoint-root property, and compare the two overloads for sibling agreement.
            try {
                final org.apache.commons.math.analysis.UnivariateRealFunction f =
                        new org.apache.commons.math.analysis.UnivariateRealFunction() {
                            public double value(double v) {
                                return v - x;
                            }
                        };
                double initial = mean + sd;
                double lower = Math.min(initial, x) - 1.0d;
                double upper = Math.max(initial, x) + 1.0d;
                if (lower < upper) {
                    double[] b1 = org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils.bracket(
                            f, initial, lower, upper);
                    double[] b2 = org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils.bracket(
                            f, initial, lower, upper, Integer.MAX_VALUE);

                    if (b1 == null || b2 == null || b1.length != 2 || b2.length != 2
                            || b1[0] != b2[0] || b1[1] != b2[1]) {
                        throw new RuntimeException(
                                "[oracle:overload] metamorphic violation: bracket overloads must agree"
                                        + " initial=" + initial
                                        + " lower=" + lower
                                        + " upper=" + upper
                                        + " x=" + x
                                        + " b1=[" + (b1 == null ? "null" : (b1[0] + "," + b1[1])) + "]"
                                        + " b2=[" + (b2 == null ? "null" : (b2[0] + "," + b2[1])) + "]");
                    }
                    if (!((b2[0] <= x && x <= b2[1]) || (b2[1] <= x && x <= b2[0]))) {
                        throw new RuntimeException(
                                "[oracle:bracketed] metamorphic violation: returned interval must bracket the constructed root"
                                        + " x=" + x
                                        + " interval=[" + b2[0] + "," + b2[1] + "]");
                    }
                }
            } catch (IllegalArgumentException e) {
                return;
            } catch (org.apache.commons.math.FunctionEvaluationException e) {
                return;
            } catch (org.apache.commons.math.ConvergenceException e) {
                return;
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException
                            || t instanceof NumberFormatException;
            boolean hasBracket = false;
            for (Throwable c = t; c != null && !hasBracket; c = c.getCause()) {
                StackTraceElement[] st = c.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(st[i].getClassName())
                                && "bracket".equals(st[i].getMethodName())) {
                            hasBracket = true;
                            break;
                        }
                    }
                }
            }
            if (!cleanRejection && t instanceof org.apache.commons.math.MathException && hasBracket) {
                throw new RuntimeException(t);
            }
        }
    }
}