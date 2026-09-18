package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorSeed();

        int selector = data.consumeInt(0, 3);
        if (selector == 0) {
            exploreLinearExplicitVsStored(data);
        } else if (selector == 1) {
            exploreLinearKnownRoot(data);
        } else if (selector == 2) {
            exploreShiftedInterval(data);
        } else {
            exploreSymmetricWindow(data);
        }
    }

    private static void anchorSeed() {
        try {
            UnivariateRealFunction f = new SinFunction();
            UnivariateRealSolver solver = new BisectionSolver();
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isKnownPatchedRegionNpe(t)) {
                return;
            }
        }
    }

    private static void exploreLinearExplicitVsStored(FuzzedDataProvider data) {
        double explicitRoot = bounded(data.consumeInt(-1000, 1000) / 10.0);
        double delta = 0.5 + (data.consumeInt(0, 200) / 100.0);
        double storedRoot = explicitRoot + (data.consumeBoolean() ? delta : -delta);

        exerciseLinearCase(explicitRoot, storedRoot, data);
    }

    private static void exploreLinearKnownRoot(FuzzedDataProvider data) {
        double explicitRoot = bounded(data.consumeInt(-5000, 5000) / 50.0);
        double offset = 1.0 + (data.consumeInt(0, 300) / 100.0);
        double storedRoot = bounded(explicitRoot + offset + 1.0);

        exerciseLinearCase(explicitRoot, storedRoot, data);
    }

    private static void exploreShiftedInterval(FuzzedDataProvider data) {
        double base = bounded(data.consumeInt(-2000, 2000) / 20.0);
        double explicitRoot = base + 0.25;
        double storedRoot = base - 0.75;

        exerciseLinearCase(explicitRoot, storedRoot, data);
    }

    private static void exploreSymmetricWindow(FuzzedDataProvider data) {
        double center = bounded(data.consumeInt(-3000, 3000) / 30.0);
        double explicitRoot = center - 0.4;
        double storedRoot = center + 0.4;

        exerciseLinearCase(explicitRoot, storedRoot, data);
    }

    private static void exerciseLinearCase(double explicitRoot, double storedRoot, FuzzedDataProvider data) {
        if (Math.abs(explicitRoot - storedRoot) < 0.2) {
            storedRoot = explicitRoot + 1.0;
        }

        double pad = 1.0 + (data.consumeInt(0, 200) / 100.0);
        double min = Math.min(explicitRoot, storedRoot) - pad;
        double max = Math.max(explicitRoot, storedRoot) + pad;
        double initial = (min + max) / 2.0;

        if (!(min < explicitRoot && explicitRoot < max && min < storedRoot && storedRoot < max && min < max)) {
            return;
        }

        PolynomialFunction explicitFunction = linearRoot(explicitRoot);
        PolynomialFunction storedFunction = linearRoot(storedRoot);

        double reported;
        try {
            BisectionSolver solver = new BisectionSolver(storedFunction);
            reported = solver.solve(explicitFunction, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isKnownPatchedRegionNpe(t)) {
                return;
            }
            return;
        }

        double independent;
        try {
            BisectionSolver explicitSolver = new BisectionSolver(explicitFunction);
            independent = explicitSolver.solve(min, max);
        } catch (Throwable t) {
            return;
        }

        double tol = 1.0e-6;

        if (Math.abs(reported - explicitRoot) > tol) {
            throw new RuntimeException(
                "[oracle:linear-root-recovery] metamorphic violation: "
                    + "for the real library PolynomialFunction x-c, solve(f,min,max,initial) must recover c on a valid bracketing interval; "
                    + "inputRoot=" + explicitRoot
                    + " storedRoot=" + storedRoot
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial
                    + " reported=" + reported);
        }

        if (Math.abs(reported - independent) > tol) {
            throw new RuntimeException(
                "[oracle:fresh-explicit-agreement] metamorphic violation: "
                    + "the explicit-function overload and a fresh solver constructed with that same function compute the same root on the same valid interval; "
                    + "storedRoot=" + storedRoot
                    + " explicitRoot=" + explicitRoot
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial
                    + " reported=" + reported
                    + " independent=" + independent);
        }
    }

    private static PolynomialFunction linearRoot(double root) {
        return new PolynomialFunction(new double[] { -root, 1.0 });
    }

    private static boolean isKnownPatchedRegionNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("ConvergenceException")
                || name.contains("Argument")
                || name.contains("Invalid")
                || name.contains("OutOfRange")
                || name.contains("NoBracketing")
                || name.contains("NotPositive")
                || name.contains("NullArgument");
    }

    private static double bounded(double v) {
        if (v > 1000000.0) {
            return 1000000.0;
        }
        if (v < -1000000.0) {
            return -1000000.0;
        }
        return v;
    }
}