package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.analysis.UnivariateRealFunction f = new SinFunction();

        {
            double min = 3.0;
            double max = 3.2;
            double initial = 3.1;
            try {
                BisectionSolver solverWithInitial = new BisectionSolver();
                double r1 = solverWithInitial.solve(f, min, max, initial);

                BisectionSolver solverWithoutInitial = new BisectionSolver();
                double r2 = solverWithoutInitial.solve(f, min, max);

                double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), solverWithoutInitial.getAbsoluteAccuracy());

                if (Double.isNaN(r1) || Double.isInfinite(r1)) {
                    throw new RuntimeException("[oracle:anchor-finite] metamorphic violation: anchored solve returned non-finite result input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + r2);
                }

                if (Math.abs(r1 - Math.PI) > solverWithInitial.getAbsoluteAccuracy()) {
                    throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: anchored solve should recover the known root pi for SinFunction input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + Math.PI);
                }

                /* Contract/oracle: the documented same-name solve overloads are intended to agree on equivalent inputs;
                   adding an initial guess must not change the root found for the same valid function and bracketing interval. */
                if (Math.abs(r1 - r2) > tol) {
                    throw new RuntimeException("[oracle:anchor-overload] metamorphic violation: solve(f,min,max,initial) disagrees with solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + r2);
                }
            } catch (RuntimeException t) {
                boolean throughSolve = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(st[i].getClassName())
                            && "solve".equals(st[i].getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && throughSolve) {
                    throw t;
                }
            } catch (Exception t) {
                return;
            }
        }

        int k = data.consumeInt(-100, 100);
        if (k == 0) {
            k = 1;
        }

        double root = k * Math.PI;
        double width = data.consumeInt(1, 1000) / 1000.0;
        if (width <= 0.0) {
            width = 0.1;
        }
        if (width > 1.0) {
            width = 1.0;
        }

        double min = root - width;
        double max = root + width;
        if (min > max) {
            double tmp = min;
            min = max;
            max = tmp;
        }

        int initialMode = data.consumeInt(0, 4);
        double initial;
        if (initialMode == 0) {
            initial = root;
        } else if (initialMode == 1) {
            initial = min + (max - min) * 0.25;
        } else if (initialMode == 2) {
            initial = min + (max - min) * 0.5;
        } else if (initialMode == 3) {
            initial = min + (max - min) * 0.75;
        } else {
            double fraction = data.consumeInt(0, 1000) / 1000.0;
            initial = min + (max - min) * fraction;
        }

        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double r1 = solverWithInitial.solve(f, min, max, initial);

            BisectionSolver solverWithoutInitial = new BisectionSolver();
            double r2 = solverWithoutInitial.solve(f, min, max);

            double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), solverWithoutInitial.getAbsoluteAccuracy());

            /* Contract/oracle: these inputs are valid by construction for SinFunction:
               f is non-null and [min,max] brackets the known root k*pi.
               Therefore a correct implementation should solve them, and the returned root should recover the known value we constructed. */
            if (Double.isNaN(r1) || Double.isInfinite(r1)) {
                throw new RuntimeException("[oracle:finite] metamorphic violation: solve returned non-finite result input=[" + min + "," + max + "," + initial + "] lhs=" + r1 + " rhs=" + r2);
            }

            if (Math.abs(r1 - root) > solverWithInitial.getAbsoluteAccuracy()) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: constructed SinFunction interval around known root k*pi was not recovered input=[" + min + "," + max + "," + initial + "] lhs=" + r1 + " rhs=" + root);
            }

            /* Contract/oracle: same-name overload agreement on equivalent valid inputs. A patch that merely suppresses the crash
               but skips installing/using the function will make solve(f,min,max,initial) diverge from solve(f,min,max). */
            if (Math.abs(r1 - r2) > tol) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) disagrees with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + r1 + " rhs=" + r2);
            }
        } catch (RuntimeException t) {
            boolean throughSolve = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(st[i].getClassName())
                        && "solve".equals(st[i].getMethodName())) {
                    throughSolve = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && throughSolve) {
                throw t;
            }
        } catch (Exception t) {
            return;
        }
    }
}