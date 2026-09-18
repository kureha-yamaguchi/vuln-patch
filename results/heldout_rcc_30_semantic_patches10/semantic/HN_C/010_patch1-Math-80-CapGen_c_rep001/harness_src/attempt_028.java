package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.remainingBytes() > 0 ? data.consumeInt(0, 64) : 0;

        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            int kind = data.remainingBytes() > 0 ? data.consumeInt(0, 11) : 0;
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
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    main[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    main[i] = Double.MIN_VALUE;
                    break;
                case 10:
                    main[i] = -Double.MIN_VALUE;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        int secondaryLength;
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            secondaryLength = n > 0 ? n - 1 : 0;
        } else if (data.remainingBytes() > 0) {
            secondaryLength = data.consumeInt(0, 64);
        } else {
            secondaryLength = n > 0 ? n - 1 : 0;
        }

        double[] secondary = new double[secondaryLength];
        for (int i = 0; i < secondaryLength; i++) {
            int kind = data.remainingBytes() > 0 ? data.consumeInt(0, 11) : 0;
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
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    secondary[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 10:
                    secondary[i] = -Double.MIN_VALUE;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        double splitTolerance;
        if (data.remainingBytes() > 0) {
            int kind = data.consumeInt(0, 9);
            switch (kind) {
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
                case 7:
                    splitTolerance = Double.MIN_VALUE;
                    break;
                case 8:
                    splitTolerance = Double.MAX_VALUE;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    splitTolerance = Double.longBitsToDouble(bits);
                    break;
            }
        } else {
            splitTolerance = 0.0d;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getV();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getD();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getDeterminant();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getRealEigenvalues();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getImagEigenvalues();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getSolver();
        }

        int queries = data.remainingBytes() > 0 ? data.consumeInt(0, 16) : 0;
        for (int i = 0; i < queries; i++) {
            int index;
            if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                index = data.consumeInt();
            } else if (n > 0 && data.remainingBytes() > 0) {
                index = data.consumeInt(0, n - 1);
            } else {
                index = 0;
            }

            int action = data.remainingBytes() > 0 ? data.consumeInt(0, 3) : 0;
            switch (action) {
                case 0:
                    ed.getRealEigenvalue(index);
                    break;
                case 1:
                    ed.getImagEigenvalue(index);
                    break;
                case 2:
                    ed.getEigenvector(index);
                    break;
                default:
                    ed.getSolver();
                    break;
            }
        }
    }
}