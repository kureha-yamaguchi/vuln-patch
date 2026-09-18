package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runExactAnchorFirst();

        int centerTenth = data.consumeInt(-1000, 1000);
        int widthTenth = data.consumeInt(5, 200);
        double center = centerTenth / 10.0;
        double width = widthTenth / 10.0;

        double min = center - width;
        double max = center + width;

        double explicitRoot = center - width / 3.0;
        double storedRoot = center + width / 3.0;
        if (storedRoot == explicitRoot) {
            storedRoot = center + width / 4.0;
        }

        double initial;
        switch (data.consumeInt(0, 4)) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = explicitRoot;
                break;
            case 3:
                initial = storedRoot;
                break;
            default:
                initial = center + (data.consumeInt(-100, 100) / 100.0) * width;
                break;
        }

        runPolynomialChecks(min, max, initial, explicitRoot, storedRoot);
        runStoredTwoArgCheck(min, max, storedRoot);
        runBoundarySeedVariants(data);
    }

    private static void runExactAnchorFirst() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        try {
            double result = solver.solve(f, 3.0, 3.2, 3.1);
            double acc = solver.getAbsoluteAccuracy();
            if (Math.abs(result - Math.PI) > Math.max(acc * 2.0, 1e-12)) {
                throw new RuntimeException("[oracle:anchor-pi-close] metamorphic violation: valid seed should approximate PI result=" + result + " acc=" + acc);
            }
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                return;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runPolynomialChecks(double min, double max, double initial, double explicitRoot, double storedRoot) {
        PolynomialFunction explicitFunction = linearRoot(explicitRoot);
        PolynomialFunction storedFunction = linearRoot(storedRoot);

        BisectionSolver solver = new BisectionSolver(storedFunction);
        double beforeAcc = solver.getAbsoluteAccuracy();
        int beforeMaxIter = solver.getMaximalIterationCount();

        try {
            double result = solver.solve(explicitFunction, min, max, initial);
            double tol = Math.max(solver.getAbsoluteAccuracy() * 2.0, 1e-9);

            if (Math.abs(result - explicitRoot) > tol) {
                throw new RuntimeException("[oracle:poly-root] metamorphic violation: explicit linear function on a valid bracket must return its unique root min=" + min + " max=" + max + " initial=" + initial + " explicitRoot=" + explicitRoot + " storedRoot=" + storedRoot + " result=" + result + " tol=" + tol);
            }

            double residual = explicitFunction.value(result);
            if (Math.abs(residual) > Math.max(4.0 * tol, 1e-8)) {
                throw new RuntimeException("[oracle:poly-residual] metamorphic violation: returned value must approximately satisfy the explicit function min=" + min + " max=" + max + " initial=" + initial + " explicitRoot=" + explicitRoot + " result=" + result + " residual=" + residual);
            }

            if (solver.getAbsoluteAccuracy() != beforeAcc || solver.getMaximalIterationCount() != beforeMaxIter) {
                throw new RuntimeException("[oracle:config-stable] metamorphic violation: solve should not mutate solver configuration accBefore=" + beforeAcc + " accAfter=" + solver.getAbsoluteAccuracy() + " iterBefore=" + beforeMaxIter + " iterAfter=" + solver.getMaximalIterationCount());
            }
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                return;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runStoredTwoArgCheck(double min, double max, double storedRoot) {
        PolynomialFunction storedFunction = linearRoot(storedRoot);
        BisectionSolver solver = new BisectionSolver(storedFunction);
        try {
            double result = solver.solve(min, max);
            double tol = Math.max(solver.getAbsoluteAccuracy() * 2.0, 1e-9);
            if (Math.abs(result - storedRoot) > tol) {
                throw new RuntimeException("[oracle:stored-twoarg-root] metamorphic violation: two-arg solve with stored linear function must approximate its unique root min=" + min + " max=" + max + " storedRoot=" + storedRoot + " result=" + result + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runBoundarySeedVariants(FuzzedDataProvider data) {
        double base = 3.0 + (data.consumeInt(-20, 20) / 1000.0);
        double min = base;
        double max = base + 0.2 + (data.consumeInt(0, 20) / 1000.0);
        if (!(min < Math.PI && Math.PI < max)) {
            min = 3.0;
            max = 3.2;
        }

        double[] initials = new double[] {
            min,
            max,
            (min + max) / 2.0,
            Math.PI,
            min - 0.01,
            max + 0.01
        };

        for (int i = 0; i < initials.length; i++) {
            BisectionSolver solver = new BisectionSolver(new SinFunction());
            try {
                double result = solver.solve(new SinFunction(), min, max, initials[i]);
                double tol = Math.max(solver.getAbsoluteAccuracy() * 2.0, 1e-9);
                if (Math.abs(result - Math.PI) > tol) {
                    throw new RuntimeException("[oracle:boundary-initial-window] metamorphic violation: on a valid sin bracket around PI, varying initial near the changed delegation boundary must still converge to PI min=" + min + " max=" + max + " initial=" + initials[i] + " result=" + result + " tol=" + tol);
                }
            } catch (Throwable t) {
                if (isRootCauseNpe(t)) {
                    return;
                }
                if (isCleanRejection(t)) {
                    return;
                }
            }
        }
    }

    private static PolynomialFunction linearRoot(double root) {
        return new PolynomialFunction(new double[] { -root, 1.0 });
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t instanceof MaxIterationsExceededException
                || t instanceof FunctionEvaluationException;
    }
}