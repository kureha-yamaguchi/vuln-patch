package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.distribution.NormalDistribution normal =
                new org.apache.commons.math.distribution.NormalDistributionImpl(0.0, 1.0);

        try {
            double anchorP = 0.9772498680518209d;
            double anchorResult = normal.inverseCumulativeProbability(anchorP);
            if (java.lang.Math.abs(anchorResult - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: failing test contract input=" + anchorP + " result=" + anchorResult + " expected=2.0");
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        int x = data.consumeInt(2, 50);
        if (data.consumeBoolean()) {
            x = 2;
        }

        try {
            double p = normal.cumulativeProbability((double) x);
            double roundTrip = normal.inverseCumulativeProbability(p);

            /* Contract/oracle:
               inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
               We build p via the real cumulativeProbability call, so p is valid by construction.
               A patch that only suppresses the throw or returns a silent wrong value breaks this relation. */
            if (java.lang.Math.abs(roundTrip - (double) x) > 1.0e-6d) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x input=" + x + " p=" + p + " lhs=" + roundTrip + " rhs=" + x);
            }

            double p2 = normal.cumulativeProbability(roundTrip);
            if (java.lang.Math.abs(p2 - p) > 1.0e-12d) {
                throw new RuntimeException("[oracle:prob-roundtrip] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) != p input=" + x + " p=" + p + " lhs=" + p2 + " rhs=" + p);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (t == null) {
            return;
        }

        if (isCleanRejection(t)) {
            return;
        }

        if (t instanceof RuntimeException) {
            String msg = ((RuntimeException) t).getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }

        if (isGroundTruthRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                return true;
            }
            String name = c.getClass().getName();
            if (name != null) {
                if (name.endsWith("IllegalArgumentException")
                        || name.endsWith("NumberFormatException")
                        || name.endsWith("InvalidRepresentationException")
                        || name.endsWith("InvalidMatrixException")
                        || name.endsWith("NotPositiveException")
                        || name.endsWith("OutOfRangeException")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        boolean sawMathException = false;
        boolean throughBracket = false;

        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof org.apache.commons.math.MathException) {
                sawMathException = true;
            }
            StackTraceElement[] st = c.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        throughBracket = true;
                        break;
                    }
                }
            }
            if (throughBracket && sawMathException) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}