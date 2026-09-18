package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static double fuzzDouble(FuzzedDataProvider data) {
        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        switch (Math.floorMod(data.consumeInt(), 8)) {
            case 0:
                return Double.longBitsToDouble(bits);
            case 1:
                return (double) data.consumeInt();
            case 2:
                return (double) (-data.consumeInt());
            case 3:
                return 0.0;
            case 4:
                return -0.0;
            case 5:
                return Double.NaN;
            case 6:
                return Double.POSITIVE_INFINITY;
            default:
                return Double.NEGATIVE_INFINITY;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        if (data.consumeBoolean()) {
            int n = data.consumeInt(0, 8);
            double[][] raw = new double[n][n];

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    raw[i][j] = fuzzDouble(data);
                }
            }

            if (data.consumeBoolean()) {
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        raw[j][i] = raw[i][j];
                    }
                }
            }

            RealMatrix matrix = new Array2DRowRealMatrix(raw, false);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(matrix, fuzzDouble(data));

            ed.getV();
            RealMatrix d = ed.getD();
            ed.getVT();
            ed.getDeterminant();

            int dim = d.getRowDimension();
            for (int i = 0; i < dim; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }

            if (data.consumeBoolean()) {
                DecompositionSolver solver = ed.getSolver();
                double[] b = new double[dim];
                for (int i = 0; i < dim; i++) {
                    b[i] = fuzzDouble(data);
                }
                solver.solve(new ArrayRealVector(b, false));
            }
        } else {
            int n = data.consumeInt(0, 16);
            double[] main = new double[n];
            for (int i = 0; i < n; i++) {
                main[i] = fuzzDouble(data);
            }

            int secondaryLen;
            if (n == 0) {
                secondaryLen = data.consumeBoolean() ? 0 : data.consumeInt(0, 4);
            } else {
                secondaryLen = data.consumeBoolean() ? (n - 1) : data.consumeInt(0, 20);
            }

            double[] secondary = new double[secondaryLen];
            for (int i = 0; i < secondaryLen; i++) {
                secondary[i] = fuzzDouble(data);
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, fuzzDouble(data));

            ed.getV();
            ed.getD();
            ed.getVT();
            ed.getDeterminant();

            for (int i = 0; i < main.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }

            if (data.consumeBoolean()) {
                DecompositionSolver solver = ed.getSolver();
                double[] b = new double[main.length];
                for (int i = 0; i < main.length; i++) {
                    b[i] = fuzzDouble(data);
                }
                solver.solve(new ArrayRealVector(b, false));
            }
        }
    }
}