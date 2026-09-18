package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);

        boolean useValidSecondaryLength = data.consumeBoolean();
        int secondaryLength;
        if (useValidSecondaryLength) {
            secondaryLength = n > 0 ? n - 1 : 0;
        } else {
            secondaryLength = data.consumeInt(0, 32);
        }

        double[] main = new double[n];
        double[] secondary = new double[secondaryLength];

        for (int i = 0; i < main.length; i++) {
            int a = data.consumeInt();
            int b = data.consumeInt();
            switch (a & 7) {
                case 0:
                    main[i] = a;
                    break;
                case 1:
                    main[i] = -a;
                    break;
                case 2:
                    main[i] = b == 0 ? a : ((double) a) / b;
                    break;
                case 3:
                    main[i] = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                    break;
                case 4:
                    main[i] = 0.0;
                    break;
                case 5:
                    main[i] = -0.0;
                    break;
                case 6:
                    main[i] = (double) (byte) a;
                    break;
                default:
                    main[i] = (double) a * (double) b;
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int a = data.consumeInt();
            int b = data.consumeInt();
            switch (b & 7) {
                case 0:
                    secondary[i] = a;
                    break;
                case 1:
                    secondary[i] = -a;
                    break;
                case 2:
                    secondary[i] = a == 0 ? b : ((double) b) / a;
                    break;
                case 3:
                    secondary[i] = Double.longBitsToDouble((((long) b) << 32) ^ (a & 0xffffffffL));
                    break;
                case 4:
                    secondary[i] = 0.0;
                    break;
                case 5:
                    secondary[i] = -0.0;
                    break;
                case 6:
                    secondary[i] = (double) (byte) b;
                    break;
                default:
                    secondary[i] = (double) a * (double) b;
                    break;
            }
        }

        int sa = data.consumeInt();
        int sb = data.consumeInt();
        double splitTolerance;
        switch (sa & 7) {
            case 0:
                splitTolerance = sa;
                break;
            case 1:
                splitTolerance = -sa;
                break;
            case 2:
                splitTolerance = sb == 0 ? sa : ((double) sa) / sb;
                break;
            case 3:
                splitTolerance = Double.longBitsToDouble((((long) sa) << 32) ^ (sb & 0xffffffffL));
                break;
            case 4:
                splitTolerance = 0.0;
                break;
            case 5:
                splitTolerance = -0.0;
                break;
            case 6:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            default:
                splitTolerance = Double.NaN;
                break;
        }

        if (data.consumeBoolean()) {
            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

            ed.getDeterminant();
            ed.getRealEigenvalues();
            ed.getImagEigenvalues();
            ed.getVT();
            ed.getV();
            ed.getD();

            if (n > 0) {
                int index = data.consumeInt(0, n - 1);
                ed.getRealEigenvalue(index);
                ed.getImagEigenvalue(index);
                ed.getEigenvector(index);

                double[] rhs = new double[n];
                for (int i = 0; i < rhs.length; i++) {
                    int x = data.consumeInt();
                    int y = data.consumeInt();
                    rhs[i] = ((x & 1) == 0) ? x : Double.longBitsToDouble((((long) x) << 32) ^ (y & 0xffffffffL));
                }

                DecompositionSolver solver = ed.getSolver();
                solver.isNonSingular();
                solver.solve(rhs);
                solver.solve(new ArrayRealVector(rhs, true));

                double[][] rhsMatrix = new double[n][n == 0 ? 0 : data.consumeInt(1, Math.max(1, n))];
                for (int r = 0; r < rhsMatrix.length; r++) {
                    for (int c = 0; c < rhsMatrix[r].length; c++) {
                        int x = data.consumeInt();
                        rhsMatrix[r][c] = ((x & 3) == 0) ? 0.0 : x;
                    }
                }
                solver.solve(new Array2DRowRealMatrix(rhsMatrix, false));
            }
        } else {
            double[][] matrixData = new double[n][n];
            for (int i = 0; i < n; i++) {
                matrixData[i][i] = main[i];
            }
            int bound = Math.min(n - 1, secondary.length);
            for (int i = 0; i < bound; i++) {
                matrixData[i][i + 1] = secondary[i];
                matrixData[i + 1][i] = secondary[i];
            }
            if (data.consumeBoolean()) {
                for (int i = 0; i < n; i++) {
                    for (int j = i + 2; j < n; j++) {
                        int x = data.consumeInt();
                        double v = ((x & 7) == 0) ? Double.longBitsToDouble((((long) x) << 32) ^ (data.consumeInt() & 0xffffffffL)) : 0.0;
                        matrixData[i][j] = v;
                        matrixData[j][i] = v;
                    }
                }
            }

            RealMatrix matrix = new Array2DRowRealMatrix(matrixData, false);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(matrix, splitTolerance);

            ed.getDeterminant();
            ed.getRealEigenvalues();
            ed.getImagEigenvalues();
            ed.getVT();
            ed.getV();
            ed.getD();

            if (n > 0) {
                int index = data.consumeInt(0, n - 1);
                ed.getRealEigenvalue(index);
                ed.getImagEigenvalue(index);
                ed.getEigenvector(index);

                double[] rhs = new double[n];
                for (int i = 0; i < rhs.length; i++) {
                    rhs[i] = data.consumeInt();
                }

                DecompositionSolver solver = ed.getSolver();
                solver.isNonSingular();
                solver.solve(rhs);
                solver.solve(new ArrayRealVector(rhs, true));
            }
        }
    }
}