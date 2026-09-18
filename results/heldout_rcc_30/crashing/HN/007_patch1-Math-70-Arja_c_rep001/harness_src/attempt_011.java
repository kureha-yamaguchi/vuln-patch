package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        runSolve(f, 3.0d, 3.2d, 3.1d, true);
        checkSiblingAgreement(f, 3.0d, 3.2d, 3.1d);

        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double leftWidth = boundedDouble(data.consumeInt(), 0.01d, 1.0d);
        double rightWidth = boundedDouble(data.consumeInt(), 0.01d, 1.0d);

        double min = root - leftWidth;
        double max = root + rightWidth;

        double initial;
        switch (data.consumeInt(0, 4)) {
            case 0:
                initial = root;
                break;
            case 1:
                initial = min;
                break;
            case 2:
                initial = max;
                break;
            case 3:
                initial = min + (max - min) * boundedUnit(data.consumeInt());
                break;
            default:
                initial = (min + max) / 2.0d;
                break;
        }

        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        runSolve(f, min, max, initial, true);
        checkSiblingAgreement(f, min, max, initial);
    }

    private static void runSolve(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        } catch (FunctionEvaluationException t) {
            return;
        } catch (MaxIterationsExceededException t) {
            return;
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkSiblingAgreement(UnivariateRealFunction f, double min, double max, double initial) {
        double withInitial;
        double withoutInitial;
        double tolerance;

        try {
            BisectionSolver solver1 = new BisectionSolver();
            withInitial = solver1.solve(f, min, max, initial);
            tolerance = solver1.getAbsoluteAccuracy();
        } catch (Throwable t) {
            return;
        }

        try {
            BisectionSolver solver2 = new BisectionSolver();
            withoutInitial = solver2.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                || Double.isInfinite(withInitial) || Double.isInfinite(withoutInitial)) {
            throw new RuntimeException("[oracle:sibling-agree] metamorphic violation: solve overloads returned non-finite results input=min="
                    + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
        }

        double allowed = Math.max(tolerance, 1.0e-12d);
        if (Math.abs(withInitial - withoutInitial) > allowed) {
            throw new RuntimeException("[oracle:sibling-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=min="
                    + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial + " tol=" + allowed);
        }
    }

    private static boolean isRootCause(Throwable t) {
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
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static double boundedUnit(int x) {
        long nonNegative = x & 0x7fffffffL;
        return nonNegative / (double) Integer.MAX_VALUE;
    }

    private static double boundedDouble(int x, double min, double max) {
        return min + (max - min) * boundedUnit(x);
    }
}