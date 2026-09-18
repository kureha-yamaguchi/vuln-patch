package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        data.consumeRemainingAsBytes();

        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            Throwable wrong = null;
            boolean completedNormally = false;
            double returned = Double.NaN;
            try {
                returned = solver.solve(3624, issue631Function, 1.0, 10.0);
                completedNormally = true;
            } catch (TooManyEvaluationsException expected) {
                // Expected per the lifted oracle from RegulaFalsiSolverTest.testIssue631.
            } catch (Throwable t) {
                wrong = t;
            }
            if (wrong != null) {
                throw new FuzzerSecurityIssueLow("[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException for RegulaFalsiSolver.solve(3624,f,1,10) but got " + wrong.getClass().getName() + ": " + wrong.getMessage(), wrong);
            }
            if (completedNormally) {
                throw new FuzzerSecurityIssueLow("[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException for RegulaFalsiSolver.solve(3624,f,1,10) but call completed normally with root=" + returned);
            }
        }

        {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean leftRoot = data.consumeBoolean();
            final double min = leftRoot ? c : c - w;
            final double max = leftRoot ? c + w : c;
            final UnivariateRealFunction linear = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            double rDefault;
            double rAnySide;
            try {
                rDefault = solver.solve(100, linear, min, max);
                rAnySide = solver.solve(100, linear, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }
            if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rAnySide)) {
                throw new FuzzerSecurityIssueLow("[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) because ANY_SIDE is the documented default; min=" + min + " max=" + max + " c=" + c + " default=" + rDefault + " explicitAnySide=" + rAnySide);
            }
        }

        {
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean rootAtLeft = data.consumeBoolean();
            final double min = rootAtLeft ? c : c - w;
            final double max = rootAtLeft ? c + w : c;
            final double startValue = data.consumeBoolean() ? min : max;
            final UnivariateRealFunction linear = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            double r1;
            double r2;
            double r3;
            try {
                RegulaFalsiSolver s1 = new RegulaFalsiSolver();
                RegulaFalsiSolver s2 = new RegulaFalsiSolver();
                RegulaFalsiSolver s3 = new RegulaFalsiSolver();
                r1 = s1.solve(100, linear, min, max);
                r2 = s2.solve(100, linear, min, max, startValue);
                r3 = s3.solve(100, linear, min, max, startValue, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            // Contract justification: doSolve() returns an exact endpoint root immediately when f(min)==0 or f(max)==0.
            // For f(x)=x-c with c placed exactly at one endpoint, every correct overload must therefore return c.
            // This also checks the hidden shared state established by BaseSecantSolver constructors and solve(..., AllowedSolution):
            // ANY_SIDE is documented as the default, so the overloads must agree observably on the same valid problem.
            if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(c)) {
                throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-overload1] semantic mismatch: exact endpoint root must be returned; expected=" + c + " actual=" + r1 + " min=" + min + " max=" + max);
            }
            if (Double.doubleToLongBits(r2) != Double.doubleToLongBits(c)) {
                throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-overload2] semantic mismatch: exact endpoint root must be returned even with explicit startValue; expected=" + c + " actual=" + r2 + " min=" + min + " max=" + max + " startValue=" + startValue);
            }
            if (Double.doubleToLongBits(r3) != Double.doubleToLongBits(c)) {
                throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-overload3] semantic mismatch: exact endpoint root must be returned with explicit ANY_SIDE; expected=" + c + " actual=" + r3 + " min=" + min + " max=" + max + " startValue=" + startValue);
            }
            if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(r2) ||
                Double.doubleToLongBits(r1) != Double.doubleToLongBits(r3)) {
                throw new FuzzerSecurityIssueLow("[oracle:sibling-overloads] metamorphic violation: solve overloads must agree on the same valid problem with an exact endpoint root; c=" + c + " r1=" + r1 + " r2=" + r2 + " r3=" + r3 + " min=" + min + " max=" + max + " startValue=" + startValue);
            }
        }
    }
}