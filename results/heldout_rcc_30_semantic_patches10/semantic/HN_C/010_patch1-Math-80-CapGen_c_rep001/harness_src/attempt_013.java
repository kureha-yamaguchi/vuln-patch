package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int n = data.consumeInt(0, 32);

        final double[] main = new double[n];
        final double[] secondary = new double[n > 0 ? n - 1 : 0];

        for (int i = 0; i < main.length; i++) {
            final int mode = data.consumeInt(0, 7);
            switch (mode) {
                case 0:
                    main[i] = data.consumeInt(-4, 4);
                    break;
                case 1:
                    main[i] = data.consumeInt(-1000, 1000);
                    break;
                case 2:
                    main[i] = data.consumeBoolean() ? 0.0 : -0.0;
                    break;
                case 3:
                    main[i] = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = data.consumeByte();
                    break;
                case 6: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
                }
                default:
                    main[i] = data.consumeInt() / (double) (data.consumeInt(-16, 16) == 0 ? 1 : data.consumeInt(-16, 16));
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            final int mode = data.consumeInt(0, 7);
            switch (mode) {
                case 0:
                    secondary[i] = data.consumeInt(-4, 4);
                    break;
                case 1:
                    secondary[i] = data.consumeInt(-1000, 1000);
                    break;
                case 2:
                    secondary[i] = data.consumeBoolean() ? 0.0 : -0.0;
                    break;
                case 3:
                    secondary[i] = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                    break;
                case 4:
                    secondary[i] = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    secondary[i] = data.consumeByte();
                    break;
                case 6: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
                }
                default:
                    secondary[i] = data.consumeInt() * (data.consumeBoolean() ? 1.0 : -1.0);
                    break;
            }
        }

        final double splitTolerance;
        switch (data.consumeInt(0, 7)) {
            case 0:
                splitTolerance = 0.0;
                break;
            case 1:
                splitTolerance = -0.0;
                break;
            case 2:
                splitTolerance = data.consumeInt(-10, 10);
                break;
            case 3:
                splitTolerance = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                break;
            case 4:
                splitTolerance = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                break;
            case 5:
                splitTolerance = Double.NaN;
                break;
            case 6: {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                splitTolerance = Double.longBitsToDouble(bits);
                break;
            }
            default:
                splitTolerance = data.consumeByte();
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        ed.getD();
        ed.getV();
        ed.getVT();
        ed.getDeterminant();
        ed.getRealEigenvalues();

        if (n > 0) {
            for (int i = 0; i < Math.min(n, 4); i++) {
                int idx = data.consumeInt(0, n - 1);
                ed.getRealEigenvalue(idx);
                ed.getImagEigenvalue(idx);
                ed.getEigenvector(idx);
            }

            DecompositionSolver solver = ed.getSolver();
            solver.isNonSingular();

            double[] rhs = new double[n];
            for (int i = 0; i < n; i++) {
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        rhs[i] = data.consumeInt(-8, 8);
                        break;
                    case 1:
                        rhs[i] = data.consumeByte();
                        break;
                    case 2:
                        rhs[i] = data.consumeBoolean() ? 0.0 : -0.0;
                        break;
                    case 3:
                        rhs[i] = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                        break;
                    default: {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        rhs[i] = Double.longBitsToDouble(bits);
                        break;
                    }
                }
            }

            solver.solve(rhs);
            solver.solve(new ArrayRealVector(rhs, true));

            double[][] rhsMatrixData = new double[n][data.consumeInt(1, 3)];
            for (int r = 0; r < rhsMatrixData.length; r++) {
                for (int c = 0; c < rhsMatrixData[r].length; c++) {
                    rhsMatrixData[r][c] = (r < rhs.length) ? rhs[r] : 0.0;
                }
            }
            solver.solve(new Array2DRowRealMatrix(rhsMatrixData, false));
            solver.getInverse();
        }
    }
}