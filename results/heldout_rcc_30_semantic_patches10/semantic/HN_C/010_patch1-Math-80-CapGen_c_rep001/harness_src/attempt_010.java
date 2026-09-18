package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 24);

        double splitTolerance;
        {
            int mode = data.consumeInt(0, 7);
            if (mode == 0) {
                splitTolerance = 0.0;
            } else if (mode == 1) {
                splitTolerance = -0.0;
            } else if (mode == 2) {
                splitTolerance = Double.NaN;
            } else if (mode == 3) {
                splitTolerance = Double.POSITIVE_INFINITY;
            } else if (mode == 4) {
                splitTolerance = Double.NEGATIVE_INFINITY;
            } else {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                splitTolerance = Double.longBitsToDouble(bits);
            }
        }

        if (data.consumeBoolean()) {
            double[] main = new double[n];
            double[] secondary = new double[n > 0 ? n - 1 : 0];

            for (int i = 0; i < main.length; i++) {
                int shape = data.consumeInt(0, 9);
                if (shape == 0) {
                    main[i] = 0.0;
                } else if (shape == 1) {
                    main[i] = -0.0;
                } else if (shape == 2) {
                    main[i] = 1.0;
                } else if (shape == 3) {
                    main[i] = -1.0;
                } else if (shape == 4) {
                    main[i] = data.consumeInt();
                } else if (shape == 5) {
                    main[i] = data.consumeInt(-8, 8);
                } else if (shape == 6) {
                    main[i] = Double.NaN;
                } else if (shape == 7) {
                    main[i] = Double.POSITIVE_INFINITY;
                } else if (shape == 8) {
                    main[i] = Double.NEGATIVE_INFINITY;
                } else {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                }
            }

            for (int i = 0; i < secondary.length; i++) {
                int shape = data.consumeInt(0, 9);
                if (shape == 0) {
                    secondary[i] = 0.0;
                } else if (shape == 1) {
                    secondary[i] = -0.0;
                } else if (shape == 2) {
                    secondary[i] = 1.0;
                } else if (shape == 3) {
                    secondary[i] = -1.0;
                } else if (shape == 4) {
                    secondary[i] = data.consumeInt();
                } else if (shape == 5) {
                    secondary[i] = data.consumeInt(-8, 8);
                } else if (shape == 6) {
                    secondary[i] = Double.NaN;
                } else if (shape == 7) {
                    secondary[i] = Double.POSITIVE_INFINITY;
                } else if (shape == 8) {
                    secondary[i] = Double.NEGATIVE_INFINITY;
                } else {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                }
            }

            if (n > 0) {
                if (data.consumeBoolean()) {
                    main[0] = data.consumeBoolean() ? 0.0 : main[0];
                }
                if (data.consumeBoolean()) {
                    main[n - 1] = data.consumeBoolean() ? main[0] : -main[n - 1];
                }
            }
            if (secondary.length > 0) {
                if (data.consumeBoolean()) {
                    secondary[0] = 0.0;
                }
                if (data.consumeBoolean()) {
                    secondary[secondary.length - 1] = -secondary[secondary.length - 1];
                }
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);
            ed.getD();
            ed.getV();
            ed.getVT();
            ed.getDeterminant();
            for (int i = 0; i < n; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
            if (data.consumeBoolean()) {
                ed.getSolver();
            }
        } else {
            double[][] matrixData = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = i; j < n; j++) {
                    double v;
                    int shape = data.consumeInt(0, 9);
                    if (shape == 0) {
                        v = 0.0;
                    } else if (shape == 1) {
                        v = -0.0;
                    } else if (shape == 2) {
                        v = 1.0;
                    } else if (shape == 3) {
                        v = -1.0;
                    } else if (shape == 4) {
                        v = data.consumeInt();
                    } else if (shape == 5) {
                        v = data.consumeInt(-8, 8);
                    } else if (shape == 6) {
                        v = Double.NaN;
                    } else if (shape == 7) {
                        v = Double.POSITIVE_INFINITY;
                    } else if (shape == 8) {
                        v = Double.NEGATIVE_INFINITY;
                    } else {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        v = Double.longBitsToDouble(bits);
                    }
                    matrixData[i][j] = v;
                    matrixData[j][i] = v;
                }
            }

            if (n > 0) {
                if (data.consumeBoolean()) {
                    matrixData[0][0] = 0.0;
                }
                if (data.consumeBoolean()) {
                    matrixData[n - 1][n - 1] = -matrixData[n - 1][n - 1];
                }
            }

            RealMatrix m = new Array2DRowRealMatrix(matrixData, false);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(m, splitTolerance);
            ed.getD();
            ed.getV();
            ed.getVT();
            ed.getDeterminant();
            for (int i = 0; i < n; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
            if (data.consumeBoolean()) {
                ed.getSolver();
            }
        }
    }
}