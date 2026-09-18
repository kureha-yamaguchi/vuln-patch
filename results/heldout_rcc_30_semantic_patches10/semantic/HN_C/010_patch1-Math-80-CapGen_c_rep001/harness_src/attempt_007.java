package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);

        double[] main = new double[n];
        double[] secondary = new double[n > 0 ? n - 1 : 0];

        for (int i = 0; i < n; i++) {
            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            double v = Double.longBitsToDouble(bits);
            if (data.consumeBoolean()) {
                v = (double) data.consumeInt();
            }
            main[i] = v;
        }

        for (int i = 0; i < secondary.length; i++) {
            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            double v = Double.longBitsToDouble(bits);
            if (data.consumeBoolean()) {
                v = (double) data.consumeInt();
            }
            secondary[i] = v;
        }

        long splitBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        double splitTolerance = Double.longBitsToDouble(splitBits);
        if (data.consumeBoolean()) {
            splitTolerance = (double) data.consumeInt();
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        double[] real = ed.getRealEigenvalues();
        for (int i = 0; i < real.length; i++) {
            ed.getRealEigenvalue(i);
            ed.getImagEigenvalue(i);
            if (data.consumeBoolean()) {
                ed.getEigenvector(i);
            }
        }

        if (data.consumeBoolean()) {
            ed.getD();
        }
        if (data.consumeBoolean()) {
            ed.getV();
        }
        if (data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.consumeBoolean()) {
            ed.getDeterminant();
        }
        if (data.consumeBoolean()) {
            DecompositionSolver solver = ed.getSolver();
            if (n > 0) {
                double[] b = new double[n];
                for (int i = 0; i < n; i++) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    double v = Double.longBitsToDouble(bits);
                    if (data.consumeBoolean()) {
                        v = (double) data.consumeInt();
                    }
                    b[i] = v;
                }
                if (data.consumeBoolean()) {
                    solver.solve(b);
                } else {
                    RealVector rv = new ArrayRealVector(b, true);
                    solver.solve(rv);
                }
            }
        }

        if (data.consumeBoolean()) {
            int m = data.consumeInt(0, 16);
            double[][] matrixData = new double[m][m];
            for (int i = 0; i < m; i++) {
                for (int j = i; j < m; j++) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    double v = Double.longBitsToDouble(bits);
                    if (data.consumeBoolean()) {
                        v = (double) data.consumeInt();
                    }
                    matrixData[i][j] = v;
                    matrixData[j][i] = v;
                }
            }

            RealMatrix matrix = new Array2DRowRealMatrix(matrixData, false);
            long splitBits2 = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            double splitTolerance2 = Double.longBitsToDouble(splitBits2);
            if (data.consumeBoolean()) {
                splitTolerance2 = (double) data.consumeInt();
            }

            EigenDecompositionImpl ed2 = new EigenDecompositionImpl(matrix, splitTolerance2);
            double[] real2 = ed2.getRealEigenvalues();
            for (int i = 0; i < real2.length; i++) {
                ed2.getRealEigenvalue(i);
                ed2.getImagEigenvalue(i);
                if (data.consumeBoolean()) {
                    ed2.getEigenvector(i);
                }
            }
            if (data.consumeBoolean()) {
                ed2.getD();
            }
            if (data.consumeBoolean()) {
                ed2.getV();
            }
            if (data.consumeBoolean()) {
                ed2.getVT();
            }
            if (data.consumeBoolean()) {
                ed2.getDeterminant();
            }
        }
    }
}