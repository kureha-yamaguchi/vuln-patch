package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean fixedOracleViolated = false;
        String fixedOracleMsg = null;
        Throwable fixedOracleCause = null;

        try {
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };
            final UnivariateRealSolver solver = new RegulaFalsiSolver();
            final int maxEval = 3624 + data.consumeInt(0, 0);

            double root = Double.NaN;
            try {
                root = solver.solve(maxEval, f, 1.0, 10.0);
                fixedOracleViolated = true;
                fixedOracleMsg = "[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException but solve completed normally with root=" + root;
            } catch (TooManyEvaluationsException expected) {
                // expected by the lifted oracle from RegulaFalsiSolverTest.testIssue631
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                fixedOracleViolated = true;
                fixedOracleMsg = "[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException but got " + t.getClass().getName();
                fixedOracleCause = t;
            }

            if (fixedOracleViolated) {
                if (fixedOracleCause != null) {
                    throw new FuzzerSecurityIssueLow(fixedOracleMsg, fixedOracleCause);
                }
                throw new FuzzerSecurityIssueLow(fixedOracleMsg);
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            return;
        }

        boolean anySideViolation = false;
        String anySideMsg = null;
        try {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
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

            double rDefault;
            double rExplicit;
            double rStartExplicit;
            try {
                rDefault = solver.solve(100, f, min, max);
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }
            try {
                rExplicit = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }
            try {
                final double startValue = leftRoot ? min : max;
                rStartExplicit = solver.solve(100, f, min, max, startValue, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }

            // Contract justification: BracketedUnivariateRealSolver requires ANY_SIDE as the default allowed solution.
            // Therefore solve(maxEval,f,min,max) and solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) must agree.
            if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicit)) {
                anySideViolation = true;
                anySideMsg = "[oracle:any-side-default] metamorphic violation: default ANY_SIDE disagrees with explicit ANY_SIDE c="
                        + c + " min=" + min + " max=" + max + " default=" + rDefault + " explicit=" + rExplicit;
            }

            // Contract justification: these overloads solve the same valid problem with the same allowed solution.
            // Using an endpoint as startValue on a problem whose exact root is that endpoint must not change the returned exact root.
            if (!anySideViolation && Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rStartExplicit)) {
                anySideViolation = true;
                anySideMsg = "[oracle:start-overload-agreement] metamorphic violation: overload with explicit startValue disagrees c="
                        + c + " min=" + min + " max=" + max + " default=" + rDefault + " explicitStart=" + rStartExplicit;
            }

            // Post-condition justification: doSolve returns an endpoint immediately if its function value is exactly zero.
            // For f(x)=x-c and interval chosen so that one endpoint equals c, every correct implementation must return that exact endpoint/root.
            if (!anySideViolation && Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(c)) {
                anySideViolation = true;
                anySideMsg = "[oracle:endpoint-root] semantic mismatch: expected exact endpoint root c="
                        + c + " but default solve returned " + rDefault + " on [" + min + "," + max + "]";
            }
            if (!anySideViolation && Double.doubleToLongBits(rExplicit) != Double.doubleToLongBits(c)) {
                anySideViolation = true;
                anySideMsg = "[oracle:endpoint-root-explicit] semantic mismatch: expected exact endpoint root c="
                        + c + " but explicit ANY_SIDE solve returned " + rExplicit + " on [" + min + "," + max + "]";
            }
            if (!anySideViolation && Double.doubleToLongBits(rStartExplicit) != Double.doubleToLongBits(c)) {
                anySideViolation = true;
                anySideMsg = "[oracle:endpoint-root-start] semantic mismatch: expected exact endpoint root c="
                        + c + " but explicit-start solve returned " + rStartExplicit + " on [" + min + "," + max + "]";
            }
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            return;
        }

        if (anySideViolation) {
            throw new FuzzerSecurityIssueLow(anySideMsg);
        }

        try {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final int shift = data.consumeInt(-50, 50);
            final int width = data.consumeInt(2, 200);
            final double min1 = -width;
            final double max1 = width;
            final double min2 = min1 + shift;
            final double max2 = max1 + shift;
            final UnivariateRealFunction base = new UnivariateRealFunction() {
                public double value(double x) {
                    return x;
                }
            };
            final UnivariateRealFunction shifted = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - shift;
                }
            };

            double root1;
            double root2;
            try {
                root1 = solver.solve(100, base, min1, max1, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }
            try {
                root2 = solver.solve(100, shifted, min2, max2, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }

            boolean translationViolation = false;
            String translationMsg = null;

            // Contract justification: translating both the function f(x)=x and the interval by the same constant
            // translates the unique exact root by that constant; both sides are computed by real solver calls.
            if (Double.doubleToLongBits(root2) != Double.doubleToLongBits(root1 + shift)) {
                translationViolation = true;
                translationMsg = "[oracle:translation] metamorphic violation: translated problem root mismatch shift="
                        + shift + " root1=" + root1 + " root2=" + root2;
            }

            if (translationViolation) {
                throw new FuzzerSecurityIssueLow(translationMsg);
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            return;
        }
    }
}