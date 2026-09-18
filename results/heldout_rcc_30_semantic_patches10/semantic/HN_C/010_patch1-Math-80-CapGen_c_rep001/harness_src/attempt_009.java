package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(1, 32);

        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            switch (data.consumeInt(0, 5)) {
                case 0:
                    main[i] = data.consumeInt();
                    break;
                case 1: {
                    int num = data.consumeInt();
                    int den = data.consumeInt();
                    main[i] = den == 0 ? num : ((double) num) / den;
                    break;
                }
                case 2: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
                }
                case 3:
                    main[i] = data.consumeBoolean() ? 0.0d : -0.0d;
                    break;
                case 4:
                    main[i] = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                default:
                    main[i] = Double.NaN;
                    break;
            }
        }

        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            switch (data.consumeInt(0, 5)) {
                case 0:
                    secondary[i] = data.consumeInt();
                    break;
                case 1: {
                    int num = data.consumeInt();
                    int den = data.consumeInt();
                    secondary[i] = den == 0 ? num : ((double) num) / den;
                    break;
                }
                case 2: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
                }
                case 3:
                    secondary[i] = data.consumeBoolean() ? 0.0d : -0.0d;
                    break;
                case 4:
                    secondary[i] = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                    break;
                default:
                    secondary[i] = data.consumeBoolean() ? 1.0d : -1.0d;
                    break;
            }
        }

        double splitTolerance;
        switch (data.consumeInt(0, 5)) {
            case 0:
                splitTolerance = data.consumeInt();
                break;
            case 1: {
                int num = data.consumeInt();
                int den = data.consumeInt();
                splitTolerance = den == 0 ? num : ((double) num) / den;
                break;
            }
            case 2: {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                splitTolerance = Double.longBitsToDouble(bits);
                break;
            }
            case 3:
                splitTolerance = data.consumeBoolean() ? 0.0d : -0.0d;
                break;
            case 4:
                splitTolerance = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                break;
            default:
                splitTolerance = Double.NaN;
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        double[] realEigenvalues = ed.getRealEigenvalues();
        int dim = realEigenvalues.length;

        ed.getDeterminant();
        RealMatrix v = ed.getV();
        RealMatrix vt = ed.getVT();

        for (int i = 0; i < dim; i++) {
            ed.getRealEigenvalue(i);
            ed.getImagEigenvalue(i);
            ed.getEigenvector(i);
        }

        if (dim > 0) {
            int idx = data.consumeInt(0, dim - 1);
            ed.getRealEigenvalue(idx);
            ed.getImagEigenvalue(idx);
            ed.getEigenvector(idx);

            DecompositionSolver solver = ed.getSolver();
            solver.isNonSingular();

            double[] rhs = new double[dim];
            for (int i = 0; i < dim; i++) {
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        rhs[i] = data.consumeInt();
                        break;
                    case 1: {
                        int num = data.consumeInt();
                        int den = data.consumeInt();
                        rhs[i] = den == 0 ? num : ((double) num) / den;
                        break;
                    }
                    case 2: {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        rhs[i] = Double.longBitsToDouble(bits);
                        break;
                    }
                    case 3:
                        rhs[i] = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                        break;
                    default:
                        rhs[i] = data.consumeBoolean() ? 0.0d : -0.0d;
                        break;
                }
            }

            solver.solve(rhs);
            solver.solve(new ArrayRealVector(rhs, true));

            int cols = data.consumeInt(1, 4);
            double[][] rhsMatrixData = new double[dim][cols];
            for (int r = 0; r < dim; r++) {
                for (int c = 0; c < cols; c++) {
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            rhsMatrixData[r][c] = data.consumeInt();
                            break;
                        case 1: {
                            int num = data.consumeInt();
                            int den = data.consumeInt();
                            rhsMatrixData[r][c] = den == 0 ? num : ((double) num) / den;
                            break;
                        }
                        case 2: {
                            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                            rhsMatrixData[r][c] = Double.longBitsToDouble(bits);
                            break;
                        }
                        case 3:
                            rhsMatrixData[r][c] = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                            break;
                        default:
                            rhsMatrixData[r][c] = data.consumeBoolean() ? 0.0d : -0.0d;
                            break;
                    }
                }
            }

            solver.solve(new Array2DRowRealMatrix(rhsMatrixData, false));
        }

        if (v != null) {
            v.getRowDimension();
            v.getColumnDimension();
        }
        if (vt != null) {
            vt.getRowDimension();
            vt.getColumnDimension();
        }
    }
}