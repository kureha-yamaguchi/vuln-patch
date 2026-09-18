package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(1, 16);

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
                splitTolerance = Double.MIN_VALUE;
                break;
            case 5:
                splitTolerance = -Double.MIN_VALUE;
                break;
            case 6:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 7:
                splitTolerance = -Double.MAX_VALUE;
                break;
            case 8:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            default:
                splitTolerance = data.consumeInt() / 1024.0d;
                break;
        }

        EigenDecompositionImpl decomposition;

        if (data.consumeBoolean()) {
            double[] main = new double[n];
            double[] secondary = new double[Math.max(0, n - 1)];

            for (int i = 0; i < main.length; i++) {
                switch (data.consumeInt(0, 11)) {
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
                        main[i] = Double.MIN_VALUE;
                        break;
                    case 5:
                        main[i] = -Double.MIN_VALUE;
                        break;
                    case 6:
                        main[i] = Double.MAX_VALUE;
                        break;
                    case 7:
                        main[i] = -Double.MAX_VALUE;
                        break;
                    case 8:
                        main[i] = Double.POSITIVE_INFINITY;
                        break;
                    case 9:
                        main[i] = Double.NEGATIVE_INFINITY;
                        break;
                    case 10:
                        main[i] = Double.NaN;
                        break;
                    default:
                        main[i] = data.consumeInt() / ((data.consumeBoolean() ? 1.0d : 1024.0d));
                        break;
                }
            }

            for (int i = 0; i < secondary.length; i++) {
                switch (data.consumeInt(0, 11)) {
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
                        secondary[i] = Double.MIN_VALUE;
                        break;
                    case 5:
                        secondary[i] = -Double.MIN_VALUE;
                        break;
                    case 6:
                        secondary[i] = Double.MAX_VALUE;
                        break;
                    case 7:
                        secondary[i] = -Double.MAX_VALUE;
                        break;
                    case 8:
                        secondary[i] = Double.POSITIVE_INFINITY;
                        break;
                    case 9:
                        secondary[i] = Double.NEGATIVE_INFINITY;
                        break;
                    case 10:
                        secondary[i] = Double.NaN;
                        break;
                    default:
                        secondary[i] = data.consumeInt() / ((data.consumeBoolean() ? 1.0d : 1024.0d));
                        break;
                }
            }

            decomposition = new EigenDecompositionImpl(main, secondary, splitTolerance);
        } else {
            double[][] matrixData = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = i; j < n; j++) {
                    double value;
                    switch (data.consumeInt(0, 11)) {
                        case 0:
                            value = 0.0d;
                            break;
                        case 1:
                            value = -0.0d;
                            break;
                        case 2:
                            value = 1.0d;
                            break;
                        case 3:
                            value = -1.0d;
                            break;
                        case 4:
                            value = Double.MIN_VALUE;
                            break;
                        case 5:
                            value = -Double.MIN_VALUE;
                            break;
                        case 6:
                            value = Double.MAX_VALUE;
                            break;
                        case 7:
                            value = -Double.MAX_VALUE;
                            break;
                        case 8:
                            value = Double.POSITIVE_INFINITY;
                            break;
                        case 9:
                            value = Double.NEGATIVE_INFINITY;
                            break;
                        case 10:
                            value = Double.NaN;
                            break;
                        default:
                            value = data.consumeInt() / ((data.consumeBoolean() ? 1.0d : 1024.0d));
                            break;
                    }
                    matrixData[i][j] = value;
                    matrixData[j][i] = value;
                }
            }

            RealMatrix matrix = new Array2DRowRealMatrix(matrixData);
            decomposition = new EigenDecompositionImpl(matrix, splitTolerance);
        }

        decomposition.getDeterminant();
        decomposition.getD();
        decomposition.getV();
        decomposition.getVT();

        for (int i = 0; i < n; i++) {
            decomposition.getRealEigenvalue(i);
            decomposition.getImagEigenvalue(i);
            decomposition.getEigenvector(i);
        }

        DecompositionSolver solver = decomposition.getSolver();
        solver.isNonSingular();

        double[] rhs = new double[n];
        for (int i = 0; i < n; i++) {
            switch (data.consumeInt(0, 7)) {
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
                    rhs[i] = Double.MIN_VALUE;
                    break;
                case 4:
                    rhs[i] = -Double.MIN_VALUE;
                    break;
                case 5:
                    rhs[i] = Double.MAX_VALUE;
                    break;
                case 6:
                    rhs[i] = -Double.MAX_VALUE;
                    break;
                default:
                    rhs[i] = data.consumeInt() / 256.0d;
                    break;
            }
        }

        solver.solve(rhs);
        solver.solve(new ArrayRealVector(rhs));

        int cols = data.consumeInt(1, 4);
        double[][] rhsMatrixData = new double[n][cols];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < cols; j++) {
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        rhsMatrixData[i][j] = 0.0d;
                        break;
                    case 1:
                        rhsMatrixData[i][j] = 1.0d;
                        break;
                    case 2:
                        rhsMatrixData[i][j] = -1.0d;
                        break;
                    case 3:
                        rhsMatrixData[i][j] = Double.MIN_VALUE;
                        break;
                    case 4:
                        rhsMatrixData[i][j] = Double.MAX_VALUE;
                        break;
                    default:
                        rhsMatrixData[i][j] = data.consumeInt() / 256.0d;
                        break;
                }
            }
        }

        solver.solve(new Array2DRowRealMatrix(rhsMatrixData));
        solver.getInverse();
    }
}