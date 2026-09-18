package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);

        double[] main = new double[n];
        double[] secondary = new double[n > 0 ? n - 1 : 0];
        double[] rhs = new double[n];

        for (int i = 0; i < n; i++) {
            int selector = data.consumeInt(0, 11);
            double value;
            switch (selector) {
                case 0:
                    value = 0.0d;
                    break;
                case 1:
                    value = -0.0d;
                    break;
                case 2:
                    value = Double.NaN;
                    break;
                case 3:
                    value = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    value = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    value = Double.MIN_VALUE;
                    break;
                case 6:
                    value = -Double.MIN_VALUE;
                    break;
                case 7:
                    value = Double.MAX_VALUE;
                    break;
                case 8:
                    value = -Double.MAX_VALUE;
                    break;
                case 9:
                    value = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                    );
                    break;
                case 10:
                    value = (double) data.consumeInt();
                    break;
                default:
                    value = (double) data.consumeByte();
                    break;
            }
            main[i] = value;
        }

        for (int i = 0; i < secondary.length; i++) {
            int selector = data.consumeInt(0, 11);
            double value;
            switch (selector) {
                case 0:
                    value = 0.0d;
                    break;
                case 1:
                    value = -0.0d;
                    break;
                case 2:
                    value = Double.NaN;
                    break;
                case 3:
                    value = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    value = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    value = Double.MIN_VALUE;
                    break;
                case 6:
                    value = -Double.MIN_VALUE;
                    break;
                case 7:
                    value = Double.MAX_VALUE;
                    break;
                case 8:
                    value = -Double.MAX_VALUE;
                    break;
                case 9:
                    value = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                    );
                    break;
                case 10:
                    value = (double) data.consumeInt();
                    break;
                default:
                    value = (double) data.consumeByte();
                    break;
            }
            secondary[i] = value;
        }

        for (int i = 0; i < rhs.length; i++) {
            int selector = data.consumeInt(0, 5);
            double value;
            switch (selector) {
                case 0:
                    value = 0.0d;
                    break;
                case 1:
                    value = 1.0d;
                    break;
                case 2:
                    value = -1.0d;
                    break;
                case 3:
                    value = (double) data.consumeInt();
                    break;
                case 4:
                    value = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                    );
                    break;
                default:
                    value = (double) data.consumeByte();
                    break;
            }
            rhs[i] = value;
        }

        double splitTolerance;
        switch (data.consumeInt(0, 8)) {
            case 0:
                splitTolerance = 0.0d;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = 1.0d;
                break;
            case 3:
                splitTolerance = -1.0d;
                break;
            case 4:
                splitTolerance = Double.MIN_VALUE;
                break;
            case 5:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 6:
                splitTolerance = Double.NaN;
                break;
            case 7:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            default:
                splitTolerance = Double.longBitsToDouble(
                    (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                );
                break;
        }

        EigenDecompositionImpl eig = new EigenDecompositionImpl(main, secondary, splitTolerance);

        eig.getD();
        eig.getV();
        eig.getVT();
        eig.getDeterminant();

        double[] real = eig.getRealEigenvalues();
        double[] imag = eig.getImagEigenvalues();

        for (int i = 0; i < real.length; i++) {
            eig.getRealEigenvalue(i);
        }
        for (int i = 0; i < imag.length; i++) {
            eig.getImagEigenvalue(i);
        }

        if (n > 0) {
            int idx = data.consumeInt(0, n - 1);
            eig.getRealEigenvalue(idx);
            eig.getImagEigenvalue(idx);
            eig.getEigenvector(idx);
        }

        DecompositionSolver solver = eig.getSolver();
        solver.isNonSingular();
        if (n > 0) {
            solver.solve(rhs);
        }
        solver.getInverse();
    }
}