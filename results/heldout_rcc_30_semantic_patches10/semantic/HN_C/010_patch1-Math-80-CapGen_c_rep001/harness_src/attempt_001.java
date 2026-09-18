package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(1, 32);

        double[] main = new double[n];
        double[] secondary = new double[Math.max(0, n - 1)];

        for (int i = 0; i < main.length; i++) {
            int kind = data.consumeInt(0, 11);
            switch (kind) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = 1.0d;
                    break;
                case 3:
                    main[i] = -1.0d;
                    break;
                case 4:
                    main[i] = Double.MIN_VALUE;
                    break;
                case 5:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 6:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    main[i] = Double.NaN;
                    break;
                case 9:
                    main[i] = (double) data.consumeInt();
                    break;
                case 10: {
                    int num = data.consumeInt();
                    int den = data.consumeInt();
                    main[i] = (double) num / (den == 0 ? 1 : den);
                    break;
                }
                default: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
                }
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int kind = data.consumeInt(0, 11);
            switch (kind) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = 1.0d;
                    break;
                case 3:
                    secondary[i] = -1.0d;
                    break;
                case 4:
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 5:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 6:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    secondary[i] = Double.NaN;
                    break;
                case 9:
                    secondary[i] = (double) data.consumeInt();
                    break;
                case 10: {
                    int num = data.consumeInt();
                    int den = data.consumeInt();
                    secondary[i] = (double) num / (den == 0 ? 1 : den);
                    break;
                }
                default: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
                }
            }
        }

        double splitTolerance;
        switch (data.consumeInt(0, 9)) {
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
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            case 7:
                splitTolerance = Double.NEGATIVE_INFINITY;
                break;
            case 8:
                splitTolerance = Double.NaN;
                break;
            default: {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                splitTolerance = Double.longBitsToDouble(bits);
                break;
            }
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        ed.getRealEigenvalues();
        for (int i = 0; i < n; i++) {
            ed.getRealEigenvalue(i);
            ed.getImagEigenvalue(i);
            ed.getEigenvector(i);
        }
        ed.getImagEigenvalues();
        ed.getDeterminant();
        ed.getD();
        ed.getV();
        ed.getVT();
        ed.getSolver();
    }
}