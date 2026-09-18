package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double root = data.consumeInt(1500, 4500) / 1000.0;
        final double leftWidth = data.consumeInt(1, 3000) / 1000.0;
        final double rightWidth = data.consumeInt(1, 5000) / 1000.0;
        final double min = root - leftWidth;
        final double max = root + rightWidth;
        if (!(min < root && root < max)) {
            return;
        }

        final double scale;
        {
            int exp = data.consumeInt(-20, 20);
            double s = Math.scalb(1.0, exp);
            if (!(s > 0.0) || Double.isInfinite(s) || Double.isNaN(s)) {
                return;
            }
            scale = s;
        }

        final double absoluteAccuracy = data.consumeInt(1, 1000) / 1.0e12;
        final int maxEval = data.consumeInt(50, 20000);

        final double target = Math.exp(root);

        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - target;
            }
        };

        final UnivariateRealFunction scaledF = new UnivariateRealFunction() {
            public double value(double x) {
                return scale * (Math.exp(x) - target);
            }
        };

        final RegulaFalsiSolver solverA = new RegulaFalsiSolver(absoluteAccuracy);
        final RegulaFalsiSolver solverB = new RegulaFalsiSolver(absoluteAccuracy);

        final double solA;
        final double solB;
        try {
            solA = solverA.solve(maxEval, f, min, max);
            solB = solverB.solve(maxEval, scaledF, min, max);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (!(solA >= min && solA <= max && solB >= min && solB <= max)) {
            return;
        }

        final double tolerance = Math.max(1e-8, 64.0 * absoluteAccuracy);
        final double diff = Math.abs(solA - solB);

        // Contract justification: both functions have exactly the same zero set because one is a
        // positive scalar multiple of the other. A correct solver that "return[s] A value where
        // the function is zero" must therefore produce the same root (up to the solver accuracy)
        // for both calls. A band-aid that merely suppresses the known throw but keeps the buggy
        // Regula Falsi state update can make termination depend on function-value magnitude,
        // breaking this scale-invariance while still avoiding the already-known symptom.
        if (diff > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:scale-invariance] metamorphic violation: positive scaling changed root "
                    + "min=" + min
                    + " max=" + max
                    + " knownRoot=" + root
                    + " scale=" + scale
                    + " absAcc=" + absoluteAccuracy
                    + " maxEval=" + maxEval
                    + " lhs=" + solA
                    + " rhs=" + solB
                    + " diff=" + diff
                    + " tol=" + tolerance);
        }

        try {
            // Cross-check through the real solver state readers to steer execution through
            // BaseSecantSolver/BaseAbstractUnivariateRealSolver getters on the same solved object.
            // These are simple state-reporting methods: after solve(..., min, max), they must
            // report the same interval that was installed for that solve invocation.
            if (solverA.getMin() != min || solverA.getMax() != max) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:getter-interval] consistency violation: reportedMin=" + solverA.getMin()
                        + " reportedMax=" + solverA.getMax()
                        + " expectedMin=" + min
                        + " expectedMax=" + max);
            }
            if (solverA.getAbsoluteAccuracy() != absoluteAccuracy) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:getter-accuracy] consistency violation: reportedAbsAcc="
                        + solverA.getAbsoluteAccuracy()
                        + " expectedAbsAcc=" + absoluteAccuracy);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }
}