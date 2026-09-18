package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int degree = data.consumeInt(0, 8);

        double root;
        switch (data.consumeInt(0, 7)) {
            case 0:
                root = 0.0;
                break;
            case 1:
                root = 1.0;
                break;
            case 2:
                root = -1.0;
                break;
            case 3:
                root = 0.5;
                break;
            case 4:
                root = -0.5;
                break;
            default:
                root = data.consumeInt() / 1024.0;
                break;
        }

        double[] q = new double[degree + 1];
        for (int i = 0; i < q.length; i++) {
            int selector = data.consumeInt(0, 9);
            if (selector == 0) {
                q[i] = 0.0;
            } else if (selector == 1) {
                q[i] = 1.0;
            } else if (selector == 2) {
                q[i] = -1.0;
            } else {
                q[i] = data.consumeInt() / 4096.0;
            }
        }

        if (q.length == 0) {
            q = new double[] { 1.0 };
        }

        boolean forceNonZeroAtRoot = data.consumeBoolean();
        double qAtRoot = 0.0;
        for (int i = q.length - 1; i >= 0; i--) {
            qAtRoot = qAtRoot * root + q[i];
        }
        if (forceNonZeroAtRoot && qAtRoot == 0.0) {
            q[0] += 1.0;
        }

        double[] coeffs = new double[q.length + 1];
        coeffs[0] = -root * q[0];
        for (int i = 1; i < q.length; i++) {
            coeffs[i] = q[i - 1] - root * q[i];
        }
        coeffs[q.length] = q[q.length - 1];

        UnivariateRealFunction function = new PolynomialFunction(coeffs);

        double leftWidth;
        switch (data.consumeInt(0, 7)) {
            case 0:
                leftWidth = 0.0;
                break;
            case 1:
                leftWidth = 1.0e-12;
                break;
            case 2:
                leftWidth = 1.0;
                break;
            case 3:
                leftWidth = 2.0;
                break;
            default:
                leftWidth = Math.abs(data.consumeInt()) / 1024.0;
                break;
        }

        double rightWidth;
        switch (data.consumeInt(0, 7)) {
            case 0:
                rightWidth = 0.0;
                break;
            case 1:
                rightWidth = 1.0e-12;
                break;
            case 2:
                rightWidth = 1.0;
                break;
            case 3:
                rightWidth = 2.0;
                break;
            default:
                rightWidth = Math.abs(data.consumeInt()) / 1024.0;
                break;
        }

        double min = root - leftWidth;
        double max = root + rightWidth;

        if (data.consumeBoolean()) {
            min = root;
        }
        if (data.consumeBoolean()) {
            max = root;
        }

        if (data.consumeBoolean()) {
            double tmp = min;
            min = max;
            max = tmp;
        }

        AllowedSolution allowed;
        switch (data.consumeInt(0, 4)) {
            case 0:
                allowed = AllowedSolution.ANY_SIDE;
                break;
            case 1:
                allowed = AllowedSolution.LEFT_SIDE;
                break;
            case 2:
                allowed = AllowedSolution.RIGHT_SIDE;
                break;
            case 3:
                allowed = AllowedSolution.BELOW_SIDE;
                break;
            default:
                allowed = AllowedSolution.ABOVE_SIDE;
                break;
        }

        BaseSecantSolver solver;
        switch (data.consumeInt(0, 2)) {
            case 0:
                solver = new RegulaFalsiSolver();
                break;
            case 1:
                solver = new IllinoisSolver();
                break;
            default:
                solver = new PegasusSolver();
                break;
        }

        int maxEval;
        switch (data.consumeInt(0, 5)) {
            case 0:
                maxEval = 0;
                break;
            case 1:
                maxEval = 1;
                break;
            case 2:
                maxEval = 2;
                break;
            case 3:
                maxEval = 10;
                break;
            default:
                maxEval = Math.abs(data.consumeInt());
                break;
        }

        solver.solve(maxEval, function, min, max, allowed);
    }
}