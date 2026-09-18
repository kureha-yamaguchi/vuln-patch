package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        {
            BisectionSolver solver = new BisectionSolver();
            try {
                double r = solver.solve(f, 3.0, 3.2, 3.1);
                double tol = Math.max(1.0e-4, solver.getAbsoluteAccuracy() * 8.0);
                if (Math.abs(r - Math.PI) > tol) {
                    throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: seeded bisection interval around pi returned wrong root lhs=" + r + " rhs=" + Math.PI);
                }
                try {
                    double fr = f.value(r);
                    if (Math.abs(fr) > 1.0e-4) {
                        throw new RuntimeException("[oracle:anchor-sin] metamorphic violation: solver returned non-root for seeded pi interval x=" + r + " sin(x)=" + fr);
                    }
                } catch (Throwable ignored) {
                }
            } catch (Throwable t) {
                boolean rootCauseNpe = t instanceof NullPointerException;
                if (rootCauseNpe) {
                    StackTraceElement[] st = t.getStackTrace();
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(st[i].getClassName())
                                && "solve".equals(st[i].getMethodName())) {
                            throw new RuntimeException("[oracle:anchor-npe] metamorphic violation: valid seeded solve(f,min,max,initial) threw from BisectionSolver.solve", t);
                        }
                    }
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
            }
        }

        int k = data.consumeInt(1, 32);
        double root = k * Math.PI;

        double left = 0.01 * (data.consumeInt(1, 40));
        double right = 0.01 * (data.consumeInt(1, 40));
        double min = root - left;
        double max = root + right;

        if (!(min < root && root < max)) {
            return;
        }

        double[] initials = new double[4];
        initials[0] = min;
        initials[1] = max;
        initials[2] = (min + max) / 2.0;
        int percent = data.consumeInt(0, 1000);
        initials[3] = min + (max - min) * (percent / 1000.0);

        double tol = 1.0e-4;
        double[] results = new double[initials.length];
        int successCount = 0;

        for (int i = 0; i < initials.length; i++) {
            BisectionSolver solver = new BisectionSolver();
            try {
                double r = solver.solve(f, min, max, initials[i]);
                results[i] = r;
                successCount++;

                double localTol = Math.max(tol, solver.getAbsoluteAccuracy() * 8.0);

                /* Contract/oracle: we constructed the interval to bracket the known sine root k*pi,
                 * so a correct solver must return that root (within accuracy), regardless of which
                 * in-interval initial guess we provide. A patch that merely suppresses the old throw
                 * but skips real solving will violate this observable post-condition. */
                if (Math.abs(r - root) > localTol) {
                    throw new RuntimeException("[oracle:known-root] metamorphic violation: solve(f,min,max,initial) failed to recover constructed sine root inputRoot=" + root + " initial=" + initials[i] + " result=" + r);
                }

                try {
                    double fr = f.value(r);
                    if (Math.abs(fr) > 1.0e-4) {
                        throw new RuntimeException("[oracle:function-zero] metamorphic violation: returned point is not a root interval=[" + min + "," + max + "] initial=" + initials[i] + " x=" + r + " sin(x)=" + fr);
                    }
                } catch (Throwable ignored) {
                }
            } catch (Throwable t) {
                boolean rootCauseNpe = t instanceof NullPointerException;
                if (rootCauseNpe) {
                    StackTraceElement[] st = t.getStackTrace();
                    for (int j = 0; j < st.length; j++) {
                        if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(st[j].getClassName())
                                && "solve".equals(st[j].getMethodName())) {
                            throw new RuntimeException("[oracle:valid-npe] metamorphic violation: valid constructed sine interval caused NPE in BisectionSolver.solve interval=[" + min + "," + max + "] initial=" + initials[i], t);
                        }
                    }
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
            }
        }

        if (successCount >= 2) {
            double baseline = Double.NaN;
            for (int i = 0; i < results.length; i++) {
                if (results[i] == results[i]) {
                    baseline = results[i];
                    break;
                }
            }
            if (baseline == baseline) {
                for (int i = 0; i < results.length; i++) {
                    if (results[i] == results[i] && Math.abs(results[i] - baseline) > tol) {
                        throw new RuntimeException("[oracle:initial-stability] metamorphic violation: same bracketed sine root solved to inconsistent answers under different in-interval initials baseline=" + baseline + " other=" + results[i] + " interval=[" + min + "," + max + "]");
                    }
                }
            }
        }
    }
}