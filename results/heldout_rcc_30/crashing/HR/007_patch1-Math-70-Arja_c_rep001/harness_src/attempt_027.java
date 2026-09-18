package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exactAnchor();

        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            fuzzSinNearPi(data);
        } else {
            fuzzLinearRoot(data);
        }
    }

    private static void exactAnchor() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        try {
            double result = solver.solve(f, 3.0, 3.2, 3.1);
            double baseline = new BisectionSolver().solve(f, 3.0, 3.2);
            double tol = Math.max(solver.getAbsoluteAccuracy() * 2.0, 1e-12);
            if (Math.abs(result - baseline) > tol) {
                throw new RuntimeException("[oracle:anchor-vs-3arg] metamorphic violation: 4-arg and 3-arg bisection should agree on same valid interval result=" + result + " baseline=" + baseline + " tol=" + tol);
            }
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void fuzzSinNearPi(FuzzedDataProvider data) {
        double leftSlack = data.consumeInt(0, 1000) / 10000.0;
        double rightSlack = data.consumeInt(0, 1000) / 10000.0;
        double min = Math.PI - (0.2 + leftSlack);
        double max = Math.PI + (0.2 + rightSlack);

        double initial;
        switch (data.consumeInt(0, 5)) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = Math.PI;
                break;
            case 3:
                initial = (min + max) / 2.0;
                break;
            case 4:
                initial = min - data.consumeInt(0, 100) / 1000.0;
                break;
            default:
                initial = max + data.consumeInt(0, 100) / 1000.0;
                break;
        }

        BisectionSolver solver = new BisectionSolver();
        SinFunction f = new SinFunction();
        try {
            double result = solver.solve(f, min, max, initial);
            double baseline = new BisectionSolver().solve(f, min, max);
            double tol = Math.max(solver.getAbsoluteAccuracy() * 2.0, 1e-9);
            if (Math.abs(result - baseline) > tol) {
                throw new RuntimeException("[oracle:sin-vs-3arg] metamorphic violation: initial parameter must not change bisection result on same valid bracket min=" + min + " max=" + max + " initial=" + initial + " result=" + result + " baseline=" + baseline + " tol=" + tol);
            }
            if (Math.abs(Math.sin(result)) > 1e-6) {
                throw new RuntimeException("[oracle:sin-residual-window] metamorphic violation: returned point should be a root approximation min=" + min + " max=" + max + " initial=" + initial + " result=" + result + " residual=" + Math.sin(result));
            }
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void fuzzLinearRoot(FuzzedDataProvider data) {
        double root = data.consumeInt(-10000, 10000) / 100.0;
        double leftWidth = 0.1 + (data.consumeInt(0, 5000) / 100.0);
        double rightWidth = 0.1 + (data.consumeInt(0, 5000) / 100.0);
        double min = root - leftWidth;
        double max = root + rightWidth;

        double initial;
        switch (data.consumeInt(0, 4)) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = root;
                break;
            case 3:
                initial = min - data.consumeInt(0, 100) / 10.0;
                break;
            default:
                initial = max + data.consumeInt(0, 100) / 10.0;
                break;
        }

        PolynomialFunction f = new PolynomialFunction(new double[] { -root, 1.0 });
        BisectionSolver solver = new BisectionSolver();
        try {
            double result = solver.solve(f, min, max, initial);
            double baseline = new BisectionSolver().solve(f, min, max);
            double tol = Math.max(solver.getAbsoluteAccuracy() * 2.0, 1e-8);

            if (Math.abs(result - baseline) > tol) {
                throw new RuntimeException("[oracle:linear-vs-3arg] metamorphic violation: initial parameter must not change bisection result on same valid linear bracket root=" + root + " min=" + min + " max=" + max + " initial=" + initial + " result=" + result + " baseline=" + baseline + " tol=" + tol);
            }

            double residual = result - root;
            if (Math.abs(residual) > tol) {
                throw new RuntimeException("[oracle:linear-known-root] metamorphic violation: x-root linear polynomial has unique root at root root=" + root + " min=" + min + " max=" + max + " initial=" + initial + " result=" + result + " residual=" + residual + " tol=" + tol);
            }
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}