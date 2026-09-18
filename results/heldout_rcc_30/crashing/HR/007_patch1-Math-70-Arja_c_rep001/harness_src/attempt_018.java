package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runExactAnchorAsOracle();
        runAccuracyMonotonicityCheck(data);
    }

    private static void runExactAnchorAsOracle() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        try {
            double r = solver.solve(f, 3.0, 3.2, 3.1);
            double acc = solver.getAbsoluteAccuracy();
            if (Math.abs(r - Math.PI) > acc) {
                throw new RuntimeException("[oracle:anchor-pi-accuracy] valid explicit bisection around pi returned wrong root lhs="
                        + r + " rhs=" + Math.PI + " acc=" + acc);
            }
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                throw new RuntimeException("[oracle:anchor-valid-explicit-acceptance] valid explicit solve threw from BisectionSolver.solve", t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runAccuracyMonotonicityCheck(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-8, 8);
        double root = k * Math.PI;

        double left = scaled(data.consumeInt(1, 1500), 1000.0);
        double right = scaled(data.consumeInt(1, 1500), 1000.0);

        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }

        double initOffset = scaled(data.consumeInt(-900, 900), 1000.0);
        double initial = root + initOffset;
        if (initial <= min) {
            initial = min + (max - min) * 0.25;
        } else if (initial >= max) {
            initial = min + (max - min) * 0.75;
        }

        double coarseAcc = scaled(data.consumeInt(1, 1000), 10000.0);
        double fineAcc = coarseAcc / (2.0 + data.consumeInt(1, 8));

        BisectionSolver coarse = new BisectionSolver();
        BisectionSolver fine = new BisectionSolver();
        try {
            coarse.setAbsoluteAccuracy(coarseAcc);
            fine.setAbsoluteAccuracy(fineAcc);
        } catch (Throwable t) {
            return;
        }

        Double coarseResult = null;
        Integer coarseIters = null;
        try {
            coarseResult = coarse.solve(f, min, max, initial);
            coarseIters = coarse.getIterationCount();
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                throw new RuntimeException("[oracle:explicit-valid-bracket] valid explicit solve threw from BisectionSolver.solve min="
                        + min + " max=" + max + " initial=" + initial, t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        Double fineResult = null;
        Integer fineIters = null;
        try {
            fineResult = fine.solve(f, min, max, initial);
            fineIters = fine.getIterationCount();
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                throw new RuntimeException("[oracle:explicit-valid-bracket-fine] valid explicit solve threw from BisectionSolver.solve min="
                        + min + " max=" + max + " initial=" + initial, t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        if (coarseResult == null || fineResult == null || coarseIters == null || fineIters == null) {
            return;
        }

        /*
         * Sound oracle: in the real BisectionSolver loop, termination is guarded by
         *   Math.abs(max - min) <= absoluteAccuracy
         * with one bisection per iteration. For the same valid interval/function,
         * a stricter absoluteAccuracy cannot converge in fewer iterations than a
         * looser one on a correct implementation.
         */
        if (fineIters.intValue() < coarseIters.intValue()) {
            throw new RuntimeException("[oracle:accuracy-iteration-monotonicity] tighter accuracy used fewer iterations"
                    + " coarseAcc=" + coarseAcc
                    + " fineAcc=" + fineAcc
                    + " coarseIters=" + coarseIters
                    + " fineIters=" + fineIters
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial);
        }

        /*
         * Independent post-condition: for sin on an interval constructed to bracket
         * exactly the known root k*pi, the tighter-accuracy solve should not be
         * farther from that known root than the looser-accuracy solve, up to a
         * generous tolerance tied to the coarser accuracy.
         */
        double coarseErr = Math.abs(coarseResult.doubleValue() - root);
        double fineErr = Math.abs(fineResult.doubleValue() - root);
        if (fineErr > coarseErr + coarseAcc) {
            throw new RuntimeException("[oracle:accuracy-error-monotonicity] tighter accuracy produced a worse root"
                    + " root=" + root
                    + " coarseResult=" + coarseResult
                    + " fineResult=" + fineResult
                    + " coarseErr=" + coarseErr
                    + " fineErr=" + fineErr
                    + " coarseAcc=" + coarseAcc
                    + " fineAcc=" + fineAcc
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial);
        }
    }

    private static double scaled(int v, double d) {
        return ((double) v) / d;
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        String targetClass = "org.apache.commons.math.analysis.solvers.BisectionSolver";
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if (targetClass.equals(e.getClassName()) && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.endsWith("ConvergenceException")
                || n.endsWith("FunctionEvaluationException")
                || n.endsWith("MaxIterationsExceededException")
                || n.contains("Invalid")
                || n.contains("Argument");
    }
}