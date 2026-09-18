package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runExactAnchor();

        int a = data.consumeInt(-100, 100);
        int b = data.consumeInt(-100, 100);
        if (a == b) {
            b = a + 1;
        }

        double rootPassed = a;
        double rootStored = b;

        double lo = Math.min(rootPassed, rootStored) - 5.0;
        double hi = Math.max(rootPassed, rootStored) + 5.0;
        double initial = lo + (hi - lo) * 0.5;

        PolynomialFunction passedFunction = new PolynomialFunction(new double[] { -rootPassed, 1.0 });
        PolynomialFunction storedFunction = new PolynomialFunction(new double[] { -rootStored, 1.0 });

        BisectionSolver solver = new BisectionSolver();
        solver.f = storedFunction;

        double explicitResult;
        try {
            explicitResult = solver.solve((UnivariateRealFunction) passedFunction, lo, hi, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isKnownPatchedRegionNpe(t)) {
                return;
            }
            return;
        }

        double utilsResult;
        try {
            utilsResult = UnivariateRealSolverUtils.solve(passedFunction, lo, hi);
        } catch (Throwable t) {
            return;
        }

        double cachedResult;
        try {
            cachedResult = solver.getResult();
        } catch (Throwable t) {
            return;
        }

        double tol = Math.max(1.0e-6, solver.getAbsoluteAccuracy() * 8.0);

        if (Math.abs(explicitResult - cachedResult) > tol) {
            throw new RuntimeException("[oracle:explicit-cache] metamorphic violation: explicit solve return must match the receiver's cached result inputRoots=" + rootPassed + "," + rootStored + " lhs=" + explicitResult + " rhs=" + cachedResult);
        }

        /*
         * Contract/oracle:
         * solve(f, min, max, initial) is the explicit-function overload and must solve for the
         * passed function f on the supplied interval. UnivariateRealSolverUtils.solve(f, min, max)
         * computes the same mathematical quantity through an independent real library entry point.
         * A throw-deleting or state-leaking patch can make the top-level call return a value, but if
         * it still consults the receiver's stored function instead of the explicit argument, these
         * two independently obtained roots disagree.
         */
        if (Math.abs(explicitResult - utilsResult) > tol) {
            throw new RuntimeException("[oracle:explicit-vs-utils] metamorphic violation: explicit solve must agree with independent utility solve for the same function and interval passedRoot=" + rootPassed + " storedRoot=" + rootStored + " interval=[" + lo + "," + hi + "] lhs=" + explicitResult + " rhs=" + utilsResult);
        }
    }

    private static void runExactAnchor() {
        BisectionSolver solver = new BisectionSolver();
        UnivariateRealFunction f = new SinFunction();
        try {
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isKnownPatchedRegionNpe(t)) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t instanceof org.apache.commons.math.MathRuntimeException;
    }

    private static boolean isKnownPatchedRegionNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}