package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n;
        if (data.remainingBytes() <= 0) {
            n = 0;
        } else {
            n = data.consumeInt(0, Math.min(64, data.remainingBytes() + 2));
        }

        double[] main = new double[n];
        double[] secondary = new double[n > 0 ? n - 1 : 0];

        for (int i = 0; i < main.length; i++) {
            int selector = data.remainingBytes() > 0 ? Math.floorMod((int) data.consumeByte(), 12) : (i % 12);
            switch (selector) {
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
                    main[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    main[i] = -Double.MIN_VALUE;
                    break;
                case 9:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 10:
                    main[i] = -Double.MAX_VALUE;
                    break;
                default:
                    int a = data.remainingBytes() >= 4 ? data.consumeInt() : (i * 1103515245 + 12345);
                    int b = data.remainingBytes() >= 4 ? data.consumeInt() : (~a);
                    long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int selector = data.remainingBytes() > 0 ? Math.floorMod((int) data.consumeByte(), 12) : ((i + 5) % 12);
            switch (selector) {
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
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    secondary[i] = -Double.MIN_VALUE;
                    break;
                case 9:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 10:
                    secondary[i] = -Double.MAX_VALUE;
                    break;
                default:
                    int a = data.remainingBytes() >= 4 ? data.consumeInt() : (i * 1664525 + 1013904223);
                    int b = data.remainingBytes() >= 4 ? data.consumeInt() : (a ^ 0x5a5a5a5a);
                    long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        double splitTolerance;
        int splitSelector = data.remainingBytes() > 0 ? Math.floorMod((int) data.consumeByte(), 10) : 0;
        switch (splitSelector) {
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
                splitTolerance = -Double.MIN_VALUE;
                break;
            case 6:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 7:
                splitTolerance = Double.NaN;
                break;
            case 8:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            default:
                int a = data.remainingBytes() >= 4 ? data.consumeInt() : 0x7ff80000;
                int b = data.remainingBytes() >= 4 ? data.consumeInt() : 0;
                splitTolerance = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.consumeBoolean()) {
            ed.getRealEigenvalues();
        }
        if (n > 0 && data.consumeBoolean()) {
            ed.getRealEigenvalue(data.consumeInt(0, n - 1));
        }
        if (n > 0 && data.consumeBoolean()) {
            ed.getImagEigenvalue(data.consumeInt(0, n - 1));
        }
        if (n > 0 && data.consumeBoolean()) {
            ed.getEigenvector(data.consumeInt(0, n - 1));
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

        if (n >= 2 && data.consumeBoolean()) {
            double[] main2 = main.clone();
            double[] secondary2 = secondary.clone();

            for (int i = 0; i < main2.length; i++) {
                if ((i & 1) == 0) {
                    main2[i] = -main2[i];
                }
            }
            for (int i = 0; i < secondary2.length; i++) {
                if ((i & 1) == 1) {
                    secondary2[i] = -secondary2[i];
                }
            }

            EigenDecompositionImpl ed2 = new EigenDecompositionImpl(main2, secondary2, -splitTolerance);
            if (data.consumeBoolean()) {
                ed2.getRealEigenvalues();
            }
            if (n > 0 && data.consumeBoolean()) {
                ed2.getRealEigenvalue(data.consumeInt(0, n - 1));
            }
            if (n > 0 && data.consumeBoolean()) {
                ed2.getImagEigenvalue(data.consumeInt(0, n - 1));
            }
            if (n > 0 && data.consumeBoolean()) {
                ed2.getEigenvector(data.consumeInt(0, n - 1));
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