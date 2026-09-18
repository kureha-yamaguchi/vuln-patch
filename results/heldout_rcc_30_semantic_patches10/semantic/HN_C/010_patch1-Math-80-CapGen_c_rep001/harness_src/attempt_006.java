package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int mainLen = data.consumeInt(0, 32);

        final boolean useValidSecondaryLength = data.consumeBoolean();
        final int secondaryLen;
        if (useValidSecondaryLength) {
            secondaryLen = mainLen > 0 ? mainLen - 1 : 0;
        } else {
            secondaryLen = data.consumeInt(0, 32);
        }

        final double[] main = new double[mainLen];
        final double[] secondary = new double[secondaryLen];

        for (int i = 0; i < main.length; i++) {
            int choice = data.consumeInt(0, 7);
            if (choice == 0) {
                main[i] = 0.0;
            } else if (choice == 1) {
                main[i] = -0.0;
            } else if (choice == 2) {
                main[i] = Double.POSITIVE_INFINITY;
            } else if (choice == 3) {
                main[i] = Double.NEGATIVE_INFINITY;
            } else if (choice == 4) {
                main[i] = Double.NaN;
            } else {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                main[i] = Double.longBitsToDouble(bits);
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int choice = data.consumeInt(0, 7);
            if (choice == 0) {
                secondary[i] = 0.0;
            } else if (choice == 1) {
                secondary[i] = -0.0;
            } else if (choice == 2) {
                secondary[i] = Double.POSITIVE_INFINITY;
            } else if (choice == 3) {
                secondary[i] = Double.NEGATIVE_INFINITY;
            } else if (choice == 4) {
                secondary[i] = Double.NaN;
            } else {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                secondary[i] = Double.longBitsToDouble(bits);
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
                splitTolerance = 1.0;
                break;
            case 3:
                splitTolerance = -1.0;
                break;
            case 4:
                splitTolerance = Double.MIN_VALUE;
                break;
            case 5:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 6:
                splitTolerance = Double.NaN;
                break;
            default:
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                splitTolerance = Double.longBitsToDouble(bits);
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.consumeBoolean()) {
            ed.getRealEigenvalues();
        }
        if (data.consumeBoolean() && mainLen > 0) {
            int idx = data.consumeInt(0, mainLen - 1);
            ed.getRealEigenvalue(idx);
        }
        if (data.consumeBoolean() && mainLen > 0) {
            int idx = data.consumeInt(0, mainLen - 1);
            ed.getImagEigenvalue(idx);
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
        if (data.consumeBoolean() && mainLen > 0) {
            int idx = data.consumeInt(0, mainLen - 1);
            ed.getEigenvector(idx);
        }
        if (data.consumeBoolean()) {
            ed.getSolver();
        }
    }
}