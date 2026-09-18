package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mainLen = data.consumeInt(0, 32);
        double[] main = new double[mainLen];

        for (int i = 0; i < main.length; i++) {
            int kind = Math.abs((int) data.consumeByte()) % 8;
            switch (kind) {
                case 0:
                    main[i] = 0.0;
                    break;
                case 1:
                    main[i] = -0.0;
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
                    main[i] = data.consumeInt();
                    break;
                default:
                    main[i] = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                    );
                    break;
            }
        }

        boolean useValidSecondaryLength = data.consumeBoolean();
        int secondaryLen;
        if (useValidSecondaryLength && mainLen > 0) {
            secondaryLen = mainLen - 1;
        } else {
            secondaryLen = data.consumeInt(0, 32);
        }

        double[] secondary = new double[secondaryLen];
        for (int i = 0; i < secondary.length; i++) {
            int kind = Math.abs((int) data.consumeByte()) % 8;
            switch (kind) {
                case 0:
                    secondary[i] = 0.0;
                    break;
                case 1:
                    secondary[i] = -0.0;
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
                    secondary[i] = data.consumeInt();
                    break;
                default:
                    secondary[i] = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                    );
                    break;
            }
        }

        double splitTolerance;
        {
            int kind = Math.abs((int) data.consumeByte()) % 8;
            switch (kind) {
                case 0:
                    splitTolerance = 0.0;
                    break;
                case 1:
                    splitTolerance = -0.0;
                    break;
                case 2:
                    splitTolerance = Double.NaN;
                    break;
                case 3:
                    splitTolerance = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    splitTolerance = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    splitTolerance = data.consumeInt();
                    break;
                default:
                    splitTolerance = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                    );
                    break;
            }
        }

        if (data.consumeBoolean()) {
            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

            if (data.consumeBoolean()) {
                ed.getRealEigenvalues();
            }
            if (data.consumeBoolean()) {
                ed.getImagEigenvalues();
            }
            if (data.consumeBoolean()) {
                ed.getDeterminant();
            }
            if (data.consumeBoolean()) {
                ed.getV();
            }
            if (data.consumeBoolean()) {
                ed.getVT();
            }
            if (data.consumeBoolean()) {
                ed.getD();
            }
            if (mainLen > 0 && data.consumeBoolean()) {
                ed.getRealEigenvalue(data.consumeInt(0, mainLen - 1));
            }
            if (mainLen > 0 && data.consumeBoolean()) {
                ed.getImagEigenvalue(data.consumeInt(0, mainLen - 1));
            }
            if (mainLen > 0 && data.consumeBoolean()) {
                ed.getEigenvector(data.consumeInt(0, mainLen - 1));
            }
        }

        int dim = data.consumeInt(0, 12);
        double[][] matrixData = new double[dim][dim];
        boolean makeSymmetric = data.consumeBoolean();

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (makeSymmetric && j < i) {
                    matrixData[i][j] = matrixData[j][i];
                    continue;
                }
                int kind = Math.abs((int) data.consumeByte()) % 8;
                double v;
                switch (kind) {
                    case 0:
                        v = 0.0;
                        break;
                    case 1:
                        v = -0.0;
                        break;
                    case 2:
                        v = Double.NaN;
                        break;
                    case 3:
                        v = Double.POSITIVE_INFINITY;
                        break;
                    case 4:
                        v = Double.NEGATIVE_INFINITY;
                        break;
                    case 5:
                        v = data.consumeInt();
                        break;
                    default:
                        v = Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                        );
                        break;
                }
                matrixData[i][j] = v;
                if (makeSymmetric && i != j) {
                    matrixData[j][i] = v;
                }
            }
        }

        if (data.consumeBoolean()) {
            RealMatrix m = new Array2DRowRealMatrix(matrixData);
            EigenDecompositionImpl ed2 = new EigenDecompositionImpl(m, splitTolerance);

            if (data.consumeBoolean()) {
                ed2.getRealEigenvalues();
            }
            if (data.consumeBoolean()) {
                ed2.getImagEigenvalues();
            }
            if (data.consumeBoolean()) {
                ed2.getDeterminant();
            }
            if (data.consumeBoolean()) {
                ed2.getV();
            }
            if (data.consumeBoolean()) {
                ed2.getVT();
            }
            if (data.consumeBoolean()) {
                ed2.getD();
            }
            if (dim > 0 && data.consumeBoolean()) {
                ed2.getRealEigenvalue(data.consumeInt(0, dim - 1));
            }
            if (dim > 0 && data.consumeBoolean()) {
                ed2.getImagEigenvalue(data.consumeInt(0, dim - 1));
            }
            if (dim > 0 && data.consumeBoolean()) {
                ed2.getEigenvector(data.consumeInt(0, dim - 1));
            }
        }
    }
}