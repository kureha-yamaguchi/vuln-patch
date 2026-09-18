package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);

        double splitTolerance;
        switch (data.consumeInt(0, 7)) {
            case 0:
                splitTolerance = 0.0d;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = data.consumeInt();
                break;
            case 3:
                splitTolerance = data.consumeInt() / (double) data.consumeInt(1, 1024);
                break;
            case 4:
                splitTolerance = Double.NaN;
                break;
            case 5:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            case 6:
                splitTolerance = Double.NEGATIVE_INFINITY;
                break;
            default:
                splitTolerance = Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
                break;
        }

        double[] main = new double[n];
        double[] secondary = new double[n > 0 ? n - 1 : 0];

        for (int i = 0; i < main.length; i++) {
            int choice = data.consumeInt(0, 9);
            int raw = data.consumeInt();
            switch (choice) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = raw;
                    break;
                case 3:
                    main[i] = raw / (double) data.consumeInt(1, 4096);
                    break;
                case 4:
                    main[i] = Math.scalb((double) raw, data.consumeInt(-1074, 1023));
                    break;
                case 5:
                    main[i] = Double.NaN;
                    break;
                case 6:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    main[i] = (raw & 1) == 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                    break;
                default:
                    main[i] = (double) data.consumeByte();
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int choice = data.consumeInt(0, 9);
            int raw = data.consumeInt();
            switch (choice) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = raw;
                    break;
                case 3:
                    secondary[i] = raw / (double) data.consumeInt(1, 4096);
                    break;
                case 4:
                    secondary[i] = Math.scalb((double) raw, data.consumeInt(-1074, 1023));
                    break;
                case 5:
                    secondary[i] = Double.NaN;
                    break;
                case 6:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    secondary[i] = (double) data.consumeByte();
                    break;
                default:
                    secondary[i] = data.consumeBoolean() ? 1.0d : -1.0d;
                    break;
            }
        }

        boolean useMatrixPath = data.consumeBoolean();

        EigenDecompositionImpl eig;
        if (useMatrixPath) {
            double[][] matrixData = new double[n][n];
            for (int i = 0; i < n; i++) {
                matrixData[i][i] = main[i];
                if (i + 1 < n) {
                    double v = secondary[i];
                    matrixData[i][i + 1] = v;
                    matrixData[i + 1][i] = v;
                }
            }

            int extraEdits = data.consumeInt(0, n == 0 ? 0 : Math.min(n * n, 16));
            for (int e = 0; e < extraEdits; e++) {
                int r = data.consumeInt(0, n - 1);
                int c = data.consumeInt(0, n - 1);
                double v;
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        v = data.consumeInt();
                        break;
                    case 1:
                        v = data.consumeInt() / (double) data.consumeInt(1, 256);
                        break;
                    case 2:
                        v = Double.NaN;
                        break;
                    case 3:
                        v = Double.POSITIVE_INFINITY;
                        break;
                    case 4:
                        v = Double.NEGATIVE_INFINITY;
                        break;
                    default:
                        v = Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
                        break;
                }
                matrixData[r][c] = v;
                matrixData[c][r] = v;
            }

            RealMatrix matrix = new Array2DRowRealMatrix(matrixData);
            eig = new EigenDecompositionImpl(matrix, splitTolerance);
        } else {
            eig = new EigenDecompositionImpl(main, secondary, splitTolerance);
        }

        if (data.consumeBoolean()) {
            eig.getD();
        }
        if (data.consumeBoolean()) {
            eig.getV();
        }
        if (data.consumeBoolean()) {
            eig.getVT();
        }
        if (data.consumeBoolean()) {
            eig.getDeterminant();
        }

        int queries = n == 0 ? 0 : data.consumeInt(0, Math.min(n + 4, 20));
        for (int i = 0; i < queries; i++) {
            int index;
            if (data.consumeBoolean()) {
                index = data.consumeInt(-2, n + 1);
            } else {
                index = data.consumeInt(0, n - 1);
            }
            switch (data.consumeInt(0, 4)) {
                case 0:
                    eig.getRealEigenvalue(index);
                    break;
                case 1:
                    eig.getImagEigenvalue(index);
                    break;
                case 2:
                    eig.getEigenvector(index);
                    break;
                case 3:
                    eig.getSolver();
                    break;
                default:
                    eig.getDeterminant();
                    break;
            }
        }

        if (data.consumeBoolean()) {
            eig.getRealEigenvalues();
        }
        if (data.consumeBoolean()) {
            eig.getImagEigenvalues();
        }
    }
}