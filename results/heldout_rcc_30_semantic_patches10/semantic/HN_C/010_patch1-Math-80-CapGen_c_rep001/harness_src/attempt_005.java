package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mainLen = data.consumeInt(0, 32);
        int secondaryLen;
        switch (Math.floorMod(data.consumeInt(), 5)) {
            case 0:
                secondaryLen = Math.max(0, mainLen - 1);
                break;
            case 1:
                secondaryLen = mainLen;
                break;
            case 2:
                secondaryLen = Math.max(0, mainLen - 2);
                break;
            case 3:
                secondaryLen = 0;
                break;
            default:
                secondaryLen = data.consumeInt(0, 32);
                break;
        }

        double[] main = new double[mainLen];
        double[] secondary = new double[secondaryLen];

        for (int i = 0; i < main.length; i++) {
            switch (Math.floorMod(data.consumeInt(), 12)) {
                case 0:
                    main[i] = 0.0;
                    break;
                case 1:
                    main[i] = -0.0;
                    break;
                case 2:
                    main[i] = 1.0;
                    break;
                case 3:
                    main[i] = -1.0;
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
                    main[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 9:
                    main[i] = data.consumeInt();
                    break;
                case 10:
                    main[i] = data.consumeInt(-1024, 1024) / (double) (data.consumeInt(1, 32));
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            switch (Math.floorMod(data.consumeInt(), 12)) {
                case 0:
                    secondary[i] = 0.0;
                    break;
                case 1:
                    secondary[i] = -0.0;
                    break;
                case 2:
                    secondary[i] = 1.0;
                    break;
                case 3:
                    secondary[i] = -1.0;
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
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 9:
                    secondary[i] = data.consumeInt();
                    break;
                case 10:
                    secondary[i] = data.consumeInt(-1024, 1024) / (double) (data.consumeInt(1, 32));
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        if (main.length > 0 && data.consumeBoolean()) {
            main[0] = data.consumeBoolean() ? -0.0 : 0.0;
        }
        if (main.length > 1 && data.consumeBoolean()) {
            main[main.length - 1] = data.consumeBoolean() ? Double.MAX_VALUE : Double.MIN_VALUE;
        }
        if (secondary.length > 0 && data.consumeBoolean()) {
            secondary[0] = data.consumeBoolean() ? 0.0 : -0.0;
        }
        if (secondary.length > 1 && data.consumeBoolean()) {
            secondary[secondary.length - 1] = data.consumeBoolean() ? Double.MAX_VALUE : Double.MIN_VALUE;
        }

        double splitTolerance;
        switch (Math.floorMod(data.consumeInt(), 8)) {
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

        EigenDecompositionImpl eig = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.consumeBoolean()) {
            eig.getRealEigenvalues();
        }
        if (data.consumeBoolean()) {
            eig.getImagEigenvalues();
        }
        if (data.consumeBoolean()) {
            eig.getD();
        }
        if (data.consumeBoolean()) {
            eig.getV();
        }
        if (data.consumeBoolean()) {
            eig.getVT();
        }
        if (data.consumeBoolean()) {
            eig.getDeterminant();
        }
        if (data.consumeBoolean()) {
            eig.getSolver();
        }

        int dimension = main.length;
        if (dimension > 0) {
            int index = data.consumeInt(0, dimension - 1);
            if (data.consumeBoolean()) {
                eig.getRealEigenvalue(index);
            }
            if (data.consumeBoolean()) {
                eig.getImagEigenvalue(index);
            }
            if (data.consumeBoolean()) {
                eig.getEigenvector(index);
            }
        }
    }
}