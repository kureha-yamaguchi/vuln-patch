package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean useMatrixPath = data.consumeBoolean();

        if (useMatrixPath) {
            int n = data.consumeInt(1, 8);
            double[][] m = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = i; j < n; j++) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    double v = Double.longBitsToDouble(bits);
                    switch (data.consumeInt(0, 9)) {
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
                            v = Double.MIN_VALUE;
                            break;
                        case 5:
                            v = Double.MAX_VALUE;
                            break;
                        case 6:
                            v = Double.POSITIVE_INFINITY;
                            break;
                        case 7:
                            v = Double.NEGATIVE_INFINITY;
                            break;
                        case 8:
                            v = Double.NaN;
                            break;
                        default:
                            break;
                    }
                    m[i][j] = v;
                    m[j][i] = v;
                }
            }

            long splitBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            double splitTolerance = Double.longBitsToDouble(splitBits);
            switch (data.consumeInt(0, 5)) {
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
                default:
                    break;
            }

            RealMatrix matrix = new Array2DRowRealMatrix(m);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(matrix, splitTolerance);

            ed.getV();
            ed.getVT();
            ed.getD();
            ed.getDeterminant();
            ed.getRealEigenvalues();
            ed.getImagEigenvalues();

            int idx = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(idx);
            ed.getImagEigenvalue(idx);
            ed.getEigenvector(idx);
        } else {
            int n = data.consumeInt(1, 32);
            double[] main = new double[n];
            double[] secondary = new double[Math.max(0, n - 1)];

            for (int i = 0; i < n; i++) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                double v = Double.longBitsToDouble(bits);
                switch (data.consumeInt(0, 9)) {
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
                        v = Double.MIN_VALUE;
                        break;
                    case 5:
                        v = Double.MAX_VALUE;
                        break;
                    case 6:
                        v = Double.POSITIVE_INFINITY;
                        break;
                    case 7:
                        v = Double.NEGATIVE_INFINITY;
                        break;
                    case 8:
                        v = Double.NaN;
                        break;
                    default:
                        break;
                }
                main[i] = v;
            }

            for (int i = 0; i < secondary.length; i++) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                double v = Double.longBitsToDouble(bits);
                switch (data.consumeInt(0, 9)) {
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
                        v = Double.MIN_VALUE;
                        break;
                    case 5:
                        v = Double.MAX_VALUE;
                        break;
                    case 6:
                        v = Double.POSITIVE_INFINITY;
                        break;
                    case 7:
                        v = Double.NEGATIVE_INFINITY;
                        break;
                    case 8:
                        v = Double.NaN;
                        break;
                    default:
                        break;
                }
                secondary[i] = v;
            }

            long splitBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            double splitTolerance = Double.longBitsToDouble(splitBits);
            switch (data.consumeInt(0, 5)) {
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
                default:
                    break;
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

            ed.getV();
            ed.getVT();
            ed.getD();
            ed.getDeterminant();
            ed.getRealEigenvalues();
            ed.getImagEigenvalues();

            int idx = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(idx);
            ed.getImagEigenvalue(idx);
            ed.getEigenvector(idx);
        }
    }
}