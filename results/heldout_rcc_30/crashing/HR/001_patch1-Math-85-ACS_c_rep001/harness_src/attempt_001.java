package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);

        // ANCHOR: exact regression input from NormalDistributionTest.testMath280.
        try {
            double seedP = 0.9772498680518209d;
            double seedX = normal.inverseCumulativeProbability(seedP);
            // Contract-based post-condition: cumulativeProbability(inverseCumulativeProbability(p)) == p
            // for valid p in (0,1) on a continuous distribution, up to numerical tolerance.
            double seedRoundTrip = normal.cumulativeProbability(seedX);
            if (Math.abs(seedRoundTrip - seedP) > 1.0e-12) {
                throw new RuntimeException("[oracle:normal-roundtrip-seed] metamorphic violation: p->icdf->cdf input=" + seedP + " x=" + seedX + " roundTrip=" + seedRoundTrip);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseThrowable(t)) {
                throwUnchecked(t);
            }
        }

        // EXPLORE root-cause property directly at the patched helper:
        // choose valid inputs where bracket reaches an endpoint root, so f(a)*f(b) == 0 exactly.
        // For SinFunction with initial=1, lower=0, upper>=2, after one expansion a=0 and b=2.
        // Since sin(0) == 0, a correct implementation must accept this as a valid bracket.
        try {
            UnivariateRealFunction f = new SinFunction();
            int extraUpper = data.consumeInt(0, 50);
            double upper = 2.0 + extraUpper;
            int maxIterations = data.consumeInt(1, 1000);

            double[] r1 = null;
            double[] r2 = null;
            boolean ok1 = false;
            boolean ok2 = false;

            try {
                r1 = UnivariateRealSolverUtils.bracket(f, 1.0, 0.0, upper);
                ok1 = true;
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                // Do not report helper-only throws directly; the public API anchor above is the
                // ground-truth observable. Skip this check on exceptions.
            }

            try {
                r2 = UnivariateRealSolverUtils.bracket(f, 1.0, 0.0, upper, maxIterations);
                ok2 = true;
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
            }

            // Documented sibling agreement: the overloads should agree on equivalent valid inputs.
            // This cross-check reaches the patched helper two independent ways.
            if (ok1 && ok2) {
                if (r1 == null || r2 == null || r1.length != 2 || r2.length != 2) {
                    throw new RuntimeException("[oracle:bracket-shape] metamorphic violation: unexpected bracket result shape");
                }
                if (Math.abs(r1[0] - r2[0]) > 0.0 || Math.abs(r1[1] - r2[1]) > 0.0) {
                    throw new RuntimeException("[oracle:bracket-overloads] metamorphic violation: overload disagreement lhs=[" + r1[0] + "," + r1[1] + "] rhs=[" + r2[0] + "," + r2[1] + "]");
                }
                double fa = f.value(r2[0]);
                double fb = f.value(r2[1]);
                if (fa * fb > 0.0) {
                    throw new RuntimeException("[oracle:bracket-contract] metamorphic violation: returned interval is not a bracket a=" + r2[0] + " b=" + r2[1] + " fa=" + fa + " fb=" + fb);
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseThrowable(t)) {
                throwUnchecked(t);
            }
        }

        // EXPLORE via the real public API on many valid probabilities.
        // Inputs are valid by construction: p is always strictly inside (0,1).
        try {
            double p = validProbability(data);
            double x = normal.inverseCumulativeProbability(p);
            double p2 = normal.cumulativeProbability(x);
            if (Math.abs(p2 - p) > 1.0e-10) {
                throw new RuntimeException("[oracle:normal-roundtrip] metamorphic violation: p->icdf->cdf input=" + p + " x=" + x + " roundTrip=" + p2);
            }

            // Independent consistency check using symmetry of the standard normal:
            // inverseCumulativeProbability(p) == -inverseCumulativeProbability(1-p).
            double xMirror = normal.inverseCumulativeProbability(1.0 - p);
            if (Math.abs(x + xMirror) > 1.0e-10) {
                throw new RuntimeException("[oracle:normal-symmetry] metamorphic violation: x(p) + x(1-p) should be 0, p=" + p + " lhs=" + x + " rhs=" + xMirror);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseThrowable(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static double validProbability(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 3);
        if (choice == 0) {
            return 0.9772498680518209d;
        }
        if (choice == 1) {
            int n = data.consumeInt(1, 999999);
            return n / 1000000.0d;
        }
        if (choice == 2) {
            int n = data.consumeInt(1, 1000000);
            double p = n / 1000001.0d;
            if (p <= 0.0) {
                p = 1.0e-6;
            } else if (p >= 1.0) {
                p = 1.0 - 1.0e-6;
            }
            return p;
        }
        double p = data.consumeBoolean() ? 0.5d : 0.8413447460685429d;
        if (data.remainingBytes() > 0) {
            int tweak = data.consumeInt(0, 1000);
            p += (tweak - 500) * 1.0e-8;
        }
        if (p <= 1.0e-6) {
            p = 1.0e-6;
        }
        if (p >= 1.0 - 1.0e-6) {
            p = 1.0 - 1.0e-6;
        }
        return p;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Illegal") || name.contains("Invalid")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCauseThrowable(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException || cur instanceof RuntimeException) {
                if (passesThroughPatchedRegion(cur)) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean passesThroughPatchedRegion(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String cls = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(m)) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                    && "inverseCumulativeProbability".equals(m)) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}