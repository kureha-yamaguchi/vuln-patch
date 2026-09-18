package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double splitTolerance;
        if (data.consumeBoolean()) {
            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            splitTolerance = Double.longBitsToDouble(bits);
        } else {
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
                    splitTolerance = Double.NaN;
                    break;
                case 5:
                    splitTolerance = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    splitTolerance = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    splitTolerance = (double) data.consumeInt();
                    break;
            }
        }

        if (data.consumeBoolean()) {
            int n = data.consumeInt(0, 32);
            double[] main = new double[n];
            for (int i = 0; i < n; i++) {
                if (data.consumeBoolean()) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                } else {
                    switch (data.consumeInt(0, 9)) {
                        case 0:
                            main[i] = 0.0d;
                            break;
                        case 1:
                            main[i] = -0.0d;
                            break;
                        case 2:
                            main[i] = Double.NaN;
                            break;
                        case 3:
                            main[i] = Double.POSITIVE_INFINITY;
                            break;
                        case 4:
                            main[i] = Double.NEGATIVE_INFINITY;
                            break;
                        case 5:
                            main[i] = Integer.MIN_VALUE;
                            break;
                        case 6:
                            main[i] = Integer.MAX_VALUE;
                            break;
                        case 7:
                            main[i] = data.consumeByte();
                            break;
                        case 8:
                            main[i] = data.consumeInt(-16, 16);
                            break;
                        default:
                            main[i] = data.consumeInt();
                            break;
                    }
                }
            }

            int secondaryLength;
            if (n == 0) {
                secondaryLength = data.consumeInt(0, 2);
            } else if (data.consumeBoolean()) {
                secondaryLength = n - 1;
            } else {
                secondaryLength = Math.max(0, n - 1 + data.consumeInt(-2, 2));
            }

            double[] secondary = new double[secondaryLength];
            for (int i = 0; i < secondaryLength; i++) {
                if (data.consumeBoolean()) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                } else {
                    switch (data.consumeInt(0, 7)) {
                        case 0:
                            secondary[i] = 0.0d;
                            break;
                        case 1:
                            secondary[i] = -0.0d;
                            break;
                        case 2:
                            secondary[i] = Double.NaN;
                            break;
                        case 3:
                            secondary[i] = Double.POSITIVE_INFINITY;
                            break;
                        case 4:
                            secondary[i] = Double.NEGATIVE_INFINITY;
                            break;
                        case 5:
                            secondary[i] = data.consumeInt(-4, 4);
                            break;
                        case 6:
                            secondary[i] = data.consumeByte();
                            break;
                        default:
                            secondary[i] = data.consumeInt();
                            break;
                    }
                }
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);
            ed.getV();
            ed.getD();
            ed.getVT();
            ed.getDeterminant();
            ed.getSolver();

            double[] real = ed.getRealEigenvalues();
            for (int i = 0; i < real.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
            if (real.length > 0) {
                int idx = data.consumeInt(0, real.length - 1);
                ed.getRealEigenvalue(idx);
                ed.getImagEigenvalue(idx);
                ed.getEigenvector(idx);
            }
        } else {
            int n = data.consumeInt(0, 10);
            double[][] matrixData = new double[n][n];
            boolean symmetric = data.consumeBoolean();

            for (int i = 0; i < n; i++) {
                for (int j = (symmetric ? i : 0); j < n; j++) {
                    double value;
                    if (data.consumeBoolean()) {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        value = Double.longBitsToDouble(bits);
                    } else {
                        switch (data.consumeInt(0, 8)) {
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
                                value = data.consumeInt(-8, 8);
                                break;
                            case 6:
                                value = data.consumeByte();
                                break;
                            case 7:
                                value = Integer.MIN_VALUE;
                                break;
                            default:
                                value = Integer.MAX_VALUE;
                                break;
                        }
                    }

                    matrixData[i][j] = value;
                    if (symmetric && i != j) {
                        matrixData[j][i] = value;
                    }
                }
            }

            if (!symmetric && n > 0 && data.consumeBoolean()) {
                int i = data.consumeInt(0, n - 1);
                int j = data.consumeInt(0, n - 1);
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                matrixData[i][j] = Double.longBitsToDouble(bits);
            }

            RealMatrix matrix = new Array2DRowRealMatrix(matrixData);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(matrix, splitTolerance);
            ed.getV();
            ed.getD();
            ed.getVT();
            ed.getDeterminant();
            ed.getSolver();

            double[] real = ed.getRealEigenvalues();
            for (int i = 0; i < real.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
            if (real.length > 0) {
                int idx = data.consumeInt(0, real.length - 1);
                ed.getRealEigenvalue(idx);
                ed.getImagEigenvalue(idx);
                ed.getEigenvector(idx);
            }
        }
    }
}