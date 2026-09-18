package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mode = data.consumeInt(0, 2);

        if (mode <= 1) {
            int n = data.consumeInt(1, 32);
            double[] main = new double[n];
            double[] secondary = new double[Math.max(0, n - 1)];

            for (int i = 0; i < main.length; i++) {
                int choice = data.consumeInt(0, 11);
                double v;
                switch (choice) {
                    case 0:
                        v = 0.0d;
                        break;
                    case 1:
                        v = -0.0d;
                        break;
                    case 2:
                        v = 1.0d;
                        break;
                    case 3:
                        v = -1.0d;
                        break;
                    case 4:
                        v = Double.NaN;
                        break;
                    case 5:
                        v = Double.POSITIVE_INFINITY;
                        break;
                    case 6:
                        v = Double.NEGATIVE_INFINITY;
                        break;
                    case 7:
                        v = Double.MIN_VALUE;
                        break;
                    case 8:
                        v = -Double.MIN_VALUE;
                        break;
                    case 9:
                        v = Double.MAX_VALUE;
                        break;
                    case 10:
                        v = -Double.MAX_VALUE;
                        break;
                    default:
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        v = Double.longBitsToDouble(bits);
                        break;
                }
                main[i] = v;
            }

            for (int i = 0; i < secondary.length; i++) {
                int choice = data.consumeInt(0, 11);
                double v;
                switch (choice) {
                    case 0:
                        v = 0.0d;
                        break;
                    case 1:
                        v = -0.0d;
                        break;
                    case 2:
                        v = 1.0d;
                        break;
                    case 3:
                        v = -1.0d;
                        break;
                    case 4:
                        v = Double.NaN;
                        break;
                    case 5:
                        v = Double.POSITIVE_INFINITY;
                        break;
                    case 6:
                        v = Double.NEGATIVE_INFINITY;
                        break;
                    case 7:
                        v = Double.MIN_VALUE;
                        break;
                    case 8:
                        v = -Double.MIN_VALUE;
                        break;
                    case 9:
                        v = Double.MAX_VALUE;
                        break;
                    case 10:
                        v = -Double.MAX_VALUE;
                        break;
                    default:
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        v = Double.longBitsToDouble(bits);
                        break;
                }
                secondary[i] = v;
            }

            double splitTolerance;
            switch (data.consumeInt(0, 7)) {
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
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    splitTolerance = Double.longBitsToDouble(bits);
                    break;
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

            ed.getD();
            ed.getV();
            ed.getVT();
            ed.getDeterminant();
            double[] reals = ed.getRealEigenvalues();
            for (int i = 0; i < reals.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
            ed.getRealEigenvalues();
            ed.getDeterminant();
        } else {
            int n = data.consumeInt(1, 16);
            double[][] matrix = new double[n][n];

            for (int i = 0; i < n; i++) {
                for (int j = i; j < n; j++) {
                    int choice = data.consumeInt(0, 11);
                    double v;
                    switch (choice) {
                        case 0:
                            v = 0.0d;
                            break;
                        case 1:
                            v = -0.0d;
                            break;
                        case 2:
                            v = 1.0d;
                            break;
                        case 3:
                            v = -1.0d;
                            break;
                        case 4:
                            v = Double.NaN;
                            break;
                        case 5:
                            v = Double.POSITIVE_INFINITY;
                            break;
                        case 6:
                            v = Double.NEGATIVE_INFINITY;
                            break;
                        case 7:
                            v = Double.MIN_VALUE;
                            break;
                        case 8:
                            v = -Double.MIN_VALUE;
                            break;
                        case 9:
                            v = Double.MAX_VALUE;
                            break;
                        case 10:
                            v = -Double.MAX_VALUE;
                            break;
                        default:
                            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                            v = Double.longBitsToDouble(bits);
                            break;
                    }
                    matrix[i][j] = v;
                    matrix[j][i] = v;
                }
            }

            double splitTolerance;
            switch (data.consumeInt(0, 7)) {
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
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    splitTolerance = Double.longBitsToDouble(bits);
                    break;
            }

            RealMatrix m = new Array2DRowRealMatrix(matrix);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(m, splitTolerance);

            ed.getD();
            ed.getV();
            ed.getVT();
            ed.getDeterminant();
            double[] reals = ed.getRealEigenvalues();
            for (int i = 0; i < reals.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
            ed.getRealEigenvalues();
            ed.getDeterminant();
        }
    }
}