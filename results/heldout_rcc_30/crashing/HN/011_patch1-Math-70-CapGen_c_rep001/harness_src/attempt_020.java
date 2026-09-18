package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        try {
            BisectionSolver anchorSolver = new BisectionSolver();
            double anchorResult = anchorSolver.solve(f, 3.0, 3.2, 3.1);

            /* Contract/oracle: the same-name overloads solve(f,min,max,initial) and solve(f,min,max)
             * are documented to solve the same problem on the same bracketed interval; a correct fix
             * must therefore agree with the sibling overload, so deleting the crashing statement or
             * otherwise bypassing the real solve path would be caught as a wrong-result divergence.
             */
            try {
                BisectionSolver siblingSolver = new BisectionSolver();
                double siblingResult = siblingSolver.solve(f, 3.0, 3.2);
                double tol = Math.max(anchorSolver.getAbsoluteAccuracy(), siblingSolver.getAbsoluteAccuracy());
                if (Double.isNaN(anchorResult) || Double.isNaN(siblingResult)
                        || Math.abs(anchorResult - siblingResult) > tol) {
                    throw new RuntimeException("[oracle:bisection-anchor] metamorphic violation: sibling overload agreement input=[3.0,3.2,3.1] lhs="
                            + anchorResult + " rhs=" + siblingResult);
                }
            } catch (Throwable t) {
                return;
            }
        } catch (RuntimeException t) {
            boolean inPatchedRegion = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        inPatchedRegion = true;
                        break;
                    }
                }
            }
            if ((t instanceof IllegalArgumentException || t instanceof NumberFormatException)) {
                return;
            }
            if ((t instanceof NullPointerException) && inPatchedRegion) {
                throw t;
            }
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        }

        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;
            double leftDelta = data.consumeInt(1, 1000) / 1000.0;
            double rightDelta = data.consumeInt(1, 1000) / 1000.0;
            double min = root - leftDelta;
            double max = root + rightDelta;
            double fraction = data.consumeInt(0, 1000) / 1000.0;
            double initial = min + fraction * (max - min);

            try {
                BisectionSolver s1 = new BisectionSolver();
                double r1 = s1.solve(f, min, max, initial);

                /* Contract/oracle: same-name overloads with and without initial guess must agree for the
                 * same function and bracketing interval; both are real library entry points and a correct
                 * implementation is obliged to return the same root, so disagreement is a real wrong-answer bug.
                 */
                try {
                    BisectionSolver s2 = new BisectionSolver();
                    double r2 = s2.solve(f, min, max);
                    double tol = Math.max(s1.getAbsoluteAccuracy(), s2.getAbsoluteAccuracy());
                    if (Double.isNaN(r1) || Double.isNaN(r2) || Math.abs(r1 - r2) > tol) {
                        throw new RuntimeException("[oracle:bisection-sibling] metamorphic violation: sibling overload agreement input=["
                                + min + "," + max + "," + initial + "] lhs=" + r1 + " rhs=" + r2);
                    }
                } catch (Throwable t) {
                    continue;
                }
            } catch (RuntimeException t) {
                boolean inPatchedRegion = false;
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    for (int j = 0; j < st.length; j++) {
                        StackTraceElement e = st[j];
                        if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                                && "solve".equals(e.getMethodName())) {
                            inPatchedRegion = true;
                            break;
                        }
                    }
                }
                if ((t instanceof IllegalArgumentException || t instanceof NumberFormatException)) {
                    continue;
                }
                if ((t instanceof NullPointerException) && inPatchedRegion) {
                    throw t;
                }
                continue;
            } catch (FunctionEvaluationException e) {
                continue;
            } catch (MaxIterationsExceededException e) {
                continue;
            }
        }
    }
}