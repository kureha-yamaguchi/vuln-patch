package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.analysis.UnivariateRealFunction function =
                new org.apache.commons.math.analysis.SinFunction();

        BaseSecantSolver solver;
        int solverKind = data.consumeInt(0, 5);
        if (solverKind == 0) {
            solver = new RegulaFalsiSolver();
        } else if (solverKind == 1) {
            solver = new IllinoisSolver();
        } else if (solverKind == 2) {
            solver = new PegasusSolver();
        } else if (solverKind == 3) {
            double absAcc = org.apache.commons.math.util.FastMath.abs(data.consumeInt()) / 1024.0;
            solver = new RegulaFalsiSolver(absAcc);
        } else if (solverKind == 4) {
            double absAcc = org.apache.commons.math.util.FastMath.abs(data.consumeInt()) / 1024.0;
            solver = new IllinoisSolver(absAcc);
        } else {
            double absAcc = org.apache.commons.math.util.FastMath.abs(data.consumeInt()) / 1024.0;
            solver = new PegasusSolver(absAcc);
        }

        int k = data.consumeInt(-10000, 10000);
        double root = k * org.apache.commons.math.util.FastMath.PI;

        long bitsA = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long bitsB = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long bitsC = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

        double a = Double.longBitsToDouble(bitsA);
        double b = Double.longBitsToDouble(bitsB);
        double c = Double.longBitsToDouble(bitsC);

        if (Double.isNaN(a) || Double.isInfinite(a)) {
            a = data.consumeInt(-1000000, 1000000) / 64.0;
        }
        if (Double.isNaN(b) || Double.isInfinite(b)) {
            b = data.consumeInt(-1000000, 1000000) / 64.0;
        }
        if (Double.isNaN(c) || Double.isInfinite(c)) {
            c = data.consumeInt(-1000000, 1000000) / 64.0;
        }

        a = org.apache.commons.math.util.FastMath.max(-1.0e6, org.apache.commons.math.util.FastMath.min(1.0e6, a));
        b = org.apache.commons.math.util.FastMath.max(-1.0e6, org.apache.commons.math.util.FastMath.min(1.0e6, b));
        c = org.apache.commons.math.util.FastMath.max(-1.0e6, org.apache.commons.math.util.FastMath.min(1.0e6, c));

        double off1 = data.consumeInt(-4096, 4096) / 1024.0;
        double off2 = data.consumeInt(-4096, 4096) / 1024.0;
        double eps = data.consumeInt(-16, 16) / 1048576.0;

        double min;
        double max;
        double start;

        switch (data.consumeInt(0, 9)) {
            case 0:
                min = root - org.apache.commons.math.util.FastMath.abs(off1);
                max = root + org.apache.commons.math.util.FastMath.abs(off2);
                start = root + eps;
                break;
            case 1:
                min = root;
                max = root + org.apache.commons.math.util.FastMath.abs(off2);
                start = root;
                break;
            case 2:
                min = root - org.apache.commons.math.util.FastMath.abs(off1);
                max = root;
                start = root;
                break;
            case 3:
                min = root + org.apache.commons.math.util.FastMath.abs(off1);
                max = root - org.apache.commons.math.util.FastMath.abs(off2);
                start = root + eps;
                break;
            case 4:
                min = root;
                max = root;
                start = root;
                break;
            case 5:
                min = a;
                max = b;
                start = c;
                break;
            case 6:
                min = root + eps;
                max = root + eps;
                start = root + eps;
                break;
            case 7:
                min = root - 1.0;
                max = root + 1.0;
                start = data.consumeBoolean() ? min : max;
                break;
            case 8:
                min = root - org.apache.commons.math.util.FastMath.PI;
                max = root + org.apache.commons.math.util.FastMath.PI;
                start = root + off1;
                break;
            default:
                min = root + a;
                max = root + b;
                start = root + c;
                break;
        }

        int maxEval = data.consumeInt(0, 10000);
        AllowedSolution allowed = AllowedSolution.values()[data.consumeInt(0, AllowedSolution.values().length - 1)];

        switch (data.consumeInt(0, 3)) {
            case 0:
                solver.solve(maxEval, function, min, max);
                break;
            case 1:
                solver.solve(maxEval, function, min, max, start);
                break;
            case 2:
                solver.solve(maxEval, function, min, max, allowed);
                break;
            default:
                solver.solve(maxEval, function, min, max, start, allowed);
                break;
        }
    }
}