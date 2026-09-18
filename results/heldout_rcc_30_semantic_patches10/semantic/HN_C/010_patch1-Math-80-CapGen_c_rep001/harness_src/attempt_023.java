package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(1, 32);

        double splitTolerance;
        switch (data.consumeInt(0, 9)) {
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
                splitTolerance = data.consumeInt(-16, 16);
                break;
            case 4:
                splitTolerance = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                break;
            case 5:
                splitTolerance = Double.NaN;
                break;
            case 6:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            case 7:
                splitTolerance = Double.NEGATIVE_INFINITY;
                break;
            case 8:
                splitTolerance = Double.MIN_VALUE;
                break;
            default:
                splitTolerance = Double.MAX_VALUE;
                break;
        }

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
                        main[i] = i;
                        break;
                    case 3:
                        main[i] = -i;
                        break;
                    case 4:
                        main[i] = data.consumeInt();
                        break;
                    case 5:
                        main[i] = data.consumeInt(-32, 32);
                        break;
                    case 6:
                        main[i] = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 7:
                        main[i] = Double.NaN;
                        break;
                    case 8:
                        main[i] = Double.POSITIVE_INFINITY;
                        break;
                    case 9:
                        main[i] = Double.NEGATIVE_INFINITY;
                        break;
                    case 10:
                        main[i] = Double.MIN_VALUE;
                        break;
                    default:
                        main[i] = Double.MAX_VALUE;
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
                        secondary[i] = i;
                        break;
                    case 3:
                        secondary[i] = -i;
                        break;
                    case 4:
                        secondary[i] = data.consumeInt();
                        break;
                    case 5:
                        secondary[i] = data.consumeInt(-32, 32);
                        break;
                    case 6:
                        secondary[i] = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 7:
                        secondary[i] = Double.NaN;
                        break;
                    case 8:
                        secondary[i] = Double.POSITIVE_INFINITY;
                        break;
                    case 9:
                        secondary[i] = Double.NEGATIVE_INFINITY;
                        break;
                    case 10:
                        secondary[i] = Double.MIN_VALUE;
                        break;
                    default:
                        secondary[i] = Double.MAX_VALUE;
                        break;
                }
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);
            ed.getRealEigenvalues();
            ed.getImagEigenvalues();
            ed.getDeterminant();
            ed.getD();
            ed.getV();
            ed.getVT();

            int idx = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(idx);
            ed.getImagEigenvalue(idx);
            ed.getEigenvector(idx);

            if (data.consumeBoolean()) {
                DecompositionSolver solver = ed.getSolver();
                solver.isNonSingular();
            }
        } else {
            double[][] matrixData = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = i; j < n; j++) {
                    double v;
                    switch (data.consumeInt(0, 11)) {
                        case 0:
                            v = 0.0d;
                            break;
                        case 1:
                            v = -0.0d;
                            break;
                        case 2:
                            v = i - j;
                            break;
                        case 3:
                            v = j - i;
                            break;
                        case 4:
                            v = data.consumeInt();
                            break;
                        case 5:
                            v = data.consumeInt(-32, 32);
                            break;
                        case 6:
                            v = Double.longBitsToDouble(
                                    (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                            break;
                        case 7:
                            v = Double.NaN;
                            break;
                        case 8:
                            v = Double.POSITIVE_INFINITY;
                            break;
                        case 9:
                            v = Double.NEGATIVE_INFINITY;
                            break;
                        case 10:
                            v = Double.MIN_VALUE;
                            break;
                        default:
                            v = Double.MAX_VALUE;
                            break;
                    }
                    matrixData[i][j] = v;
                    matrixData[j][i] = v;
                }
            }

            RealMatrix matrix = new Array2DRowRealMatrix(matrixData);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(matrix, splitTolerance);
            ed.getRealEigenvalues();
            ed.getImagEigenvalues();
            ed.getDeterminant();
            ed.getD();
            ed.getV();
            ed.getVT();

            int idx = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(idx);
            ed.getImagEigenvalue(idx);
            ed.getEigenvector(idx);

            if (data.consumeBoolean()) {
                DecompositionSolver solver = ed.getSolver();
                solver.isNonSingular();
            }
        }
    }
}