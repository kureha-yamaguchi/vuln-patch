package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mainLen;
        if (data.consumeBoolean()) {
            mainLen = data.consumeInt(0, 32);
        } else {
            mainLen = Math.floorMod((int) data.consumeByte(), 33);
        }

        double[] main = new double[mainLen];
        for (int i = 0; i < mainLen; i++) {
            int selector = data.consumeInt(0, 9);
            switch (selector) {
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
                    main[i] = (double) data.consumeInt();
                    break;
                case 8:
                    main[i] = ((double) data.consumeInt()) / (Math.floorMod((int) data.consumeByte(), 255) + 1.0);
                    break;
                default:
                    main[i] = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                    break;
            }
        }

        int secondaryLen;
        if (data.consumeBoolean()) {
            if (data.consumeBoolean()) {
                secondaryLen = Math.max(0, mainLen - 1);
            } else {
                secondaryLen = data.consumeInt(0, 32);
            }
        } else {
            secondaryLen = Math.floorMod((int) data.consumeByte(), 33);
        }

        double[] secondary = new double[secondaryLen];
        for (int i = 0; i < secondaryLen; i++) {
            int selector = data.consumeInt(0, 9);
            switch (selector) {
                case 0:
                    secondary[i] = 0.0;
                    break;
                case 1:
                    secondary[i] = -0.0;
                    break;
                case 2:
                    secondary[i] = 2.0;
                    break;
                case 3:
                    secondary[i] = -2.0;
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
                    secondary[i] = (double) data.consumeInt();
                    break;
                case 8:
                    secondary[i] = ((double) data.consumeInt()) / (Math.floorMod((int) data.consumeByte(), 255) + 1.0);
                    break;
                default:
                    secondary[i] = data.consumeBoolean() ? Double.MIN_NORMAL : -Double.MAX_VALUE;
                    break;
            }
        }

        double splitTolerance;
        switch (data.consumeInt(0, 9)) {
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
                splitTolerance = Double.NaN;
                break;
            case 5:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            case 6:
                splitTolerance = Double.NEGATIVE_INFINITY;
                break;
            case 7:
                splitTolerance = (double) data.consumeInt();
                break;
            case 8:
                splitTolerance = ((double) data.consumeInt()) / (Math.floorMod((int) data.consumeByte(), 255) + 1.0);
                break;
            default:
                splitTolerance = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

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

        int iterations = data.consumeInt(0, 12);
        for (int t = 0; t < iterations; t++) {
            int op = data.consumeInt(0, 7);
            switch (op) {
                case 0:
                    ed.getRealEigenvalues();
                    break;
                case 1:
                    ed.getImagEigenvalues();
                    break;
                case 2:
                    if (mainLen > 0) {
                        ed.getRealEigenvalue(data.consumeInt(0, mainLen - 1));
                    } else {
                        ed.getRealEigenvalue(0);
                    }
                    break;
                case 3:
                    if (mainLen > 0) {
                        ed.getImagEigenvalue(data.consumeInt(0, mainLen - 1));
                    } else {
                        ed.getImagEigenvalue(0);
                    }
                    break;
                case 4:
                    if (mainLen > 0) {
                        ed.getEigenvector(data.consumeInt(0, mainLen - 1));
                    } else {
                        ed.getEigenvector(0);
                    }
                    break;
                case 5:
                    ed.getSolver();
                    break;
                case 6:
                    ed.getDeterminant();
                    break;
                default:
                    ed.getV();
                    ed.getD();
                    ed.getVT();
                    break;
            }
        }
    }
}