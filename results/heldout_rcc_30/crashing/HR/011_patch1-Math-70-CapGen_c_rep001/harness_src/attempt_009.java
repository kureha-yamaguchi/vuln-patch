package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    private static double solveExplicit(BisectionSolver solver, UnivariateRealFunction f,
                                        double min, double max, double initial) {
        try {
            return solver.solve(f, min, max, initial);
        } catch (MaxIterationsExceededException | FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    private static double solveStored(BisectionSolver solver, double min, double max, double initial) {
        try {
            return solver.solve(min, max, initial);
        } catch (MaxIterationsExceededException | FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        BisectionSolver anchor = new BisectionSolver();
        double anchorResult = solveExplicit(anchor, f, 3.0, 3.2, 3.1);

        double anchorTol = Math.max(anchor.getAbsoluteAccuracy(), 1e-12);
        if (Math.abs(anchorResult - Math.PI) > anchorTol) {
            throw new RuntimeException("[oracle:anchor-pi-window] metamorphic violation: solving sin(x)=0 on [3.0,3.2] must return the unique root near pi result="
                    + anchorResult + " tol=" + anchorTol);
        }
        double anchorStored = anchor.getResult();
        if (Math.abs(anchorStored - anchorResult) > anchorTol) {
            throw new RuntimeException("[oracle:result-stored-explicit] metamorphic violation: after a successful explicit solve, getResult() must report the returned value returned="
                    + anchorResult + " stored=" + anchorStored + " tol=" + anchorTol);
        }

        int k = data.consumeInt(1, 30);
        int leftMilli = data.consumeInt(10, 250);
        int rightMilli = data.consumeInt(10, 250);
        double root = k * Math.PI;
        double min = root - (leftMilli / 1000.0);
        double max = root + (rightMilli / 1000.0);

        double span = max - min;
        double initial = min + span * (data.consumeInt(0, 1000) / 1000.0);

        BisectionSolver explicitSolver = new BisectionSolver();
        double explicitResult = solveExplicit(explicitSolver, f, min, max, initial);
        double explicitTol = Math.max(explicitSolver.getAbsoluteAccuracy(), 1e-12);

        if (explicitResult < min - explicitTol || explicitResult > max + explicitTol) {
            throw new RuntimeException("[oracle:interval-contained-explicit] metamorphic violation: a bisection result for a bracketed interval must lie within that interval min="
                    + min + " max=" + max + " result=" + explicitResult + " tol=" + explicitTol);
        }

        BisectionSolver storedSolver = new BisectionSolver();
        storedSolver.f = f;
        double storedResult = solveStored(storedSolver, min, max, initial);
        double storedTol = Math.max(storedSolver.getAbsoluteAccuracy(), 1e-12);
        double crossTol = Math.max(Math.max(explicitTol, storedTol), 1e-9);

        if (Math.abs(explicitResult - storedResult) > crossTol) {
            throw new RuntimeException("[oracle:stored-vs-explicit-initial] metamorphic violation: with the same installed function and same valid interval, explicit-function and stored-function initial-value overloads must agree explicit="
                    + explicitResult + " stored=" + storedResult + " min=" + min + " max=" + max + " initial=" + initial + " tol=" + crossTol);
        }

        double explicitStored = explicitSolver.getResult();
        if (Math.abs(explicitStored - explicitResult) > explicitTol) {
            throw new RuntimeException("[oracle:result-stored-explore] metamorphic violation: after a successful explicit solve, getResult() must equal the returned root returned="
                    + explicitResult + " stored=" + explicitStored + " tol=" + explicitTol);
        }
    }
}