package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exercisePublicAnchorSeed();

        int mode = data.consumeInt(0, 3);
        if (mode == 0) {
            checkRepeatedExactEndpointAcceptance();
        } else if (mode == 1) {
            checkExactEndpointWithVariableWindow(data);
        } else if (mode == 2) {
            checkExactEndpointWithBudgetFlip(data);
        } else {
            checkEndpointPreservedAcrossRebracketing(data);
        }
    }

    private static void exercisePublicAnchorSeed() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            normal.inverseCumulativeProbability(0.9772498680518209d);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isMathRootCauseFromInverse(t)) {
                return;
            }
        }
    }

    private static void checkRepeatedExactEndpointAcceptance() {
        SinFunction f = new SinFunction();
        double[] first;
        try {
            first = UnivariateRealSolverUtils.bracket(f, 1.0d, 0.0d, 2.0d, 1);
        } catch (Throwable t) {
            if (isExactEndpointBugFromBracket(t)) {
                throw new RuntimeException(
                    "[oracle:rebracket-root] valid exact-endpoint root rejected on first bracketing",
                    t);
            }
            return;
        }

        double root = chooseExactEndpoint(f, first);
        if (Double.isNaN(root)) {
            return;
        }

        double[] second;
        try {
            second = UnivariateRealSolverUtils.bracket(f, root, first[0], first[1], 1);
        } catch (Throwable t) {
            if (isExactEndpointBugFromBracket(t)) {
                throw new RuntimeException(
                    "[oracle:rebracket-root] valid exact-endpoint root rejected when reused as initial point",
                    t);
            }
            return;
        }

        verifySameEndpointRootRecovered(f, first, second, root);
    }

    private static void checkExactEndpointWithVariableWindow(FuzzedDataProvider data) {
        SinFunction f = new SinFunction();
        int extra = data.consumeInt(0, 20);
        double upper = 2.0d + extra;
        double initial = 1.0d;

        double[] bracketed;
        try {
            bracketed = UnivariateRealSolverUtils.bracket(f, initial, 0.0d, upper, 1);
        } catch (Throwable t) {
            if (isExactEndpointBugFromBracket(t)) {
                throw new RuntimeException(
                    "[oracle:boundary-zero-window] exact lower-endpoint root should be accepted for any valid wider window upper=" + upper,
                    t);
            }
            return;
        }

        double root = chooseExactEndpoint(f, bracketed);
        if (Double.isNaN(root)) {
            return;
        }

        verifyReturnedIntervalSolvesToEndpoint(f, bracketed, root);
    }

    private static void checkExactEndpointWithBudgetFlip(FuzzedDataProvider data) {
        SinFunction f = new SinFunction();
        int largerBudget = data.consumeInt(2, 50);

        double[] oneStep;
        try {
            oneStep = UnivariateRealSolverUtils.bracket(f, 1.0d, 0.0d, 2.0d, 1);
        } catch (Throwable t) {
            if (isExactEndpointBugFromBracket(t)) {
                throw new RuntimeException(
                    "[oracle:budget-boundary] exact endpoint root should not depend on larger iteration budget",
                    t);
            }
            return;
        }

        double[] manySteps;
        try {
            manySteps = UnivariateRealSolverUtils.bracket(f, 1.0d, 0.0d, 2.0d, largerBudget);
        } catch (Throwable t) {
            if (isExactEndpointBugFromBracket(t)) {
                throw new RuntimeException(
                    "[oracle:budget-boundary] exact endpoint root rejected only for larger budget=" + largerBudget,
                    t);
            }
            return;
        }

        double root1 = chooseExactEndpoint(f, oneStep);
        double root2 = chooseExactEndpoint(f, manySteps);
        if (Double.isNaN(root1) || Double.isNaN(root2)) {
            return;
        }

        if (Math.abs(root1 - root2) > 0.0d) {
            throw new RuntimeException(
                "[oracle:budget-boundary] exact root endpoint changed across iteration budgets root1=" + root1 + " root2=" + root2);
        }

        verifyReturnedIntervalSolvesToEndpoint(f, oneStep, root1);
        verifyReturnedIntervalSolvesToEndpoint(f, manySteps, root2);
    }

    private static void checkEndpointPreservedAcrossRebracketing(FuzzedDataProvider data) {
        SinFunction f = new SinFunction();
        int upperJitter = data.consumeInt(0, 30);
        double upper = 2.0d + upperJitter;

        double[] outer;
        try {
            outer = UnivariateRealSolverUtils.bracket(f, 1.0d, 0.0d, upper, 1);
        } catch (Throwable t) {
            if (isExactEndpointBugFromBracket(t)) {
                throw new RuntimeException(
                    "[oracle:nested-endpoint] exact endpoint root rejected in outer bracketing upper=" + upper,
                    t);
            }
            return;
        }

        double root = chooseExactEndpoint(f, outer);
        if (Double.isNaN(root)) {
            return;
        }

        double tighterUpper = Math.min(outer[1], Math.max(root + 1.0d, 1.0d));
        double[] inner;
        try {
            inner = UnivariateRealSolverUtils.bracket(f, root, root, tighterUpper, 1);
        } catch (Throwable t) {
            if (isExactEndpointBugFromBracket(t)) {
                throw new RuntimeException(
                    "[oracle:nested-endpoint] exact endpoint root rejected in nested bracketing tighterUpper=" + tighterUpper,
                    t);
            }
            return;
        }

        verifySameEndpointRootRecovered(f, outer, inner, root);
    }

    private static void verifySameEndpointRootRecovered(SinFunction f, double[] a, double[] b, double expectedRoot) {
        verifyReturnedIntervalSolvesToEndpoint(f, a, expectedRoot);
        verifyReturnedIntervalSolvesToEndpoint(f, b, expectedRoot);

        double ra = solveQuietly(f, a);
        double rb = solveQuietly(f, b);
        if (Double.isNaN(ra) || Double.isNaN(rb)) {
            return;
        }

        if (Math.abs(ra - rb) > 1.0e-12d || Math.abs(ra - expectedRoot) > 1.0e-12d) {
            throw new RuntimeException(
                "[oracle:rebracket-root] same exact endpoint root was not preserved across real solver calls expected=" +
                expectedRoot + " first=" + ra + " second=" + rb);
        }
    }

    private static void verifyReturnedIntervalSolvesToEndpoint(SinFunction f, double[] interval, double expectedRoot) {
        double left;
        double right;
        try {
            left = f.value(interval[0]);
            right = f.value(interval[1]);
        } catch (FunctionEvaluationException e) {
            return;
        }

        boolean leftZero = left == 0.0d;
        boolean rightZero = right == 0.0d;
        if (!leftZero && !rightZero) {
            throw new RuntimeException(
                "[oracle:boundary-zero-window] returned interval lost the exact endpoint root interval=[" +
                interval[0] + "," + interval[1] + "] f(left)=" + left + " f(right)=" + right);
        }

        double solved = solveQuietly(f, interval);
        if (Double.isNaN(solved)) {
            return;
        }

        if (Math.abs(solved - expectedRoot) > 1.0e-12d) {
            throw new RuntimeException(
                "[oracle:boundary-zero-window] solver did not recover the exact endpoint root expected=" +
                expectedRoot + " solved=" + solved + " interval=[" + interval[0] + "," + interval[1] + "]");
        }
    }

    private static double chooseExactEndpoint(SinFunction f, double[] interval) {
        try {
            if (f.value(interval[0]) == 0.0d) {
                return interval[0];
            }
            if (f.value(interval[1]) == 0.0d) {
                return interval[1];
            }
        } catch (FunctionEvaluationException e) {
            return Double.NaN;
        }
        return Double.NaN;
    }

    private static double solveQuietly(SinFunction f, double[] interval) {
        try {
            BrentSolver solver = new BrentSolver();
            return solver.solve(f, interval[0], interval[1]);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static boolean isMathRootCauseFromInverse(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        for (StackTraceElement frame : t.getStackTrace()) {
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(frame.getClassName())
                && "inverseCumulativeProbability".equals(frame.getMethodName())) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        while (cause != null) {
            for (StackTraceElement frame : cause.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(frame.getClassName())
                    && "bracket".equals(frame.getMethodName())) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static boolean isExactEndpointBugFromBracket(Throwable t) {
        if (!(t instanceof ConvergenceException)) {
            return false;
        }
        for (StackTraceElement frame : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(frame.getClassName())
                && "bracket".equals(frame.getMethodName())) {
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
        return name.contains("IllegalArgument") || name.contains("Invalid") || name.contains("OutOfRange");
    }
}