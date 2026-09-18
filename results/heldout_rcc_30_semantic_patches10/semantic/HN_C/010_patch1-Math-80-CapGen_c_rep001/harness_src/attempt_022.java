package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);

        int secondaryLength;
        if (n == 0) {
            secondaryLength = data.consumeInt(0, 8);
        } else if (data.consumeBoolean()) {
            secondaryLength = n - 1;
        } else {
            secondaryLength = data.consumeInt(0, 32);
        }

        double[] main = new double[n];
        double[] secondary = new double[secondaryLength];

        for (int i = 0; i < main.length; i++) {
            int choice = data.consumeInt(0, 11);
            switch (choice) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = 1.0d;
                    break;
                case 3:
                    main[i] = -1.0d;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    main[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    main[i] = Double.MIN_NORMAL;
                    break;
                case 10:
                    main[i] = -Double.MIN_VALUE;
                    break;
                default:
                    main[i] = (double) Float.intBitsToFloat(data.consumeInt());
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int choice = data.consumeInt(0, 11);
            switch (choice) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = 1.0d;
                    break;
                case 3:
                    secondary[i] = -1.0d;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    secondary[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    secondary[i] = Double.MIN_NORMAL;
                    break;
                case 10:
                    secondary[i] = -Double.MIN_VALUE;
                    break;
                default:
                    secondary[i] = (double) Float.intBitsToFloat(data.consumeInt());
                    break;
            }
        }

        double splitTolerance;
        switch (data.consumeInt(0, 9)) {
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
                splitTolerance = Double.NaN;
                break;
            case 5:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            case 6:
                splitTolerance = Double.NEGATIVE_INFINITY;
                break;
            case 7:
                splitTolerance = Double.MIN_NORMAL;
                break;
            case 8:
                splitTolerance = Double.MAX_VALUE;
                break;
            default:
                splitTolerance = (double) Float.intBitsToFloat(data.consumeInt());
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        ed.getDeterminant();
        RealMatrix d = ed.getD();
        RealMatrix v = ed.getV();
        RealMatrix vt = ed.getVT();

        if (n > 0) {
            int index = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(index);
            ed.getImagEigenvalue(index);
            ed.getEigenvector(index);
        }

        DecompositionSolver solver = ed.getSolver();
        solver.isNonSingular();

        double[] rhs = new double[n];
        for (int i = 0; i < rhs.length; i++) {
            int choice = data.consumeInt(0, 7);
            switch (choice) {
                case 0:
                    rhs[i] = 0.0d;
                    break;
                case 1:
                    rhs[i] = 1.0d;
                    break;
                case 2:
                    rhs[i] = -1.0d;
                    break;
                case 3:
                    rhs[i] = Double.NaN;
                    break;
                case 4:
                    rhs[i] = Double.POSITIVE_INFINITY;
                    break;
                case 5:
                    rhs[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 6:
                    rhs[i] = Double.MIN_NORMAL;
                    break;
                default:
                    rhs[i] = (double) Float.intBitsToFloat(data.consumeInt());
                    break;
            }
        }

        RealVector b = new ArrayRealVector(rhs, false);
        solver.solve(b);

        double[][] rhsMatrixData = new double[n][];
        int cols = data.consumeInt(0, 8);
        for (int r = 0; r < n; r++) {
            rhsMatrixData[r] = new double[cols];
            for (int c = 0; c < cols; c++) {
                int choice = data.consumeInt(0, 5);
                switch (choice) {
                    case 0:
                        rhsMatrixData[r][c] = 0.0d;
                        break;
                    case 1:
                        rhsMatrixData[r][c] = 1.0d;
                        break;
                    case 2:
                        rhsMatrixData[r][c] = -1.0d;
                        break;
                    case 3:
                        rhsMatrixData[r][c] = Double.NaN;
                        break;
                    case 4:
                        rhsMatrixData[r][c] = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        rhsMatrixData[r][c] = (double) Float.intBitsToFloat(data.consumeInt());
                        break;
                }
            }
        }

        RealMatrix rhsMatrix = new Array2DRowRealMatrix(rhsMatrixData, false);
        solver.solve(rhsMatrix);

        d.getRowDimension();
        v.getColumnDimension();
        vt.getRowDimension();
    }
}