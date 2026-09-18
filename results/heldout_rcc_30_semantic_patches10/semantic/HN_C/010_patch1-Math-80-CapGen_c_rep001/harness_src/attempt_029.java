package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(1, 32);

        double[] main = new double[n];
        double[] secondary = new double[Math.max(0, n - 1)];

        for (int i = 0; i < main.length; i++) {
            int mode = data.consumeInt(0, 11);
            int a = data.consumeInt();
            int b = data.consumeInt();
            switch (mode) {
                case 0:
                    main[i] = 0.0;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = a;
                    break;
                case 3:
                    main[i] = (double) a / (double) (b == 0 ? 1 : b);
                    break;
                case 4:
                    main[i] = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                    break;
                case 5:
                    main[i] = Double.NaN;
                    break;
                case 6:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    main[i] = Math.abs(a);
                    break;
                case 9:
                    main[i] = -Math.abs(a);
                    break;
                case 10:
                    main[i] = (a % 2 == 0) ? Double.MIN_VALUE : -Double.MIN_VALUE;
                    break;
                default:
                    main[i] = (a % 2 == 0) ? Double.MAX_VALUE : -Double.MAX_VALUE;
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int mode = data.consumeInt(0, 11);
            int a = data.consumeInt();
            int b = data.consumeInt();
            switch (mode) {
                case 0:
                    secondary[i] = 0.0;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = a;
                    break;
                case 3:
                    secondary[i] = (double) a / (double) (b == 0 ? 1 : b);
                    break;
                case 4:
                    secondary[i] = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                    break;
                case 5:
                    secondary[i] = Double.NaN;
                    break;
                case 6:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    secondary[i] = Math.abs(a);
                    break;
                case 9:
                    secondary[i] = -Math.abs(a);
                    break;
                case 10:
                    secondary[i] = (a % 2 == 0) ? Double.MIN_VALUE : -Double.MIN_VALUE;
                    break;
                default:
                    secondary[i] = (a % 2 == 0) ? Double.MAX_VALUE : -Double.MAX_VALUE;
                    break;
            }
        }

        double splitTolerance;
        switch (data.consumeInt(0, 7)) {
            case 0:
                splitTolerance = 0.0;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = data.consumeInt();
                break;
            case 3:
                splitTolerance = (double) data.consumeInt() / (double) (data.consumeInt() == 0 ? 1 : data.consumeInt());
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
            default:
                int a = data.consumeInt();
                int b = data.consumeInt();
                splitTolerance = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        ed.getRealEigenvalues();
        ed.getImagEigenvalues();
        ed.getDeterminant();
        ed.getV();
        ed.getD();
        ed.getVT();

        if (n > 0) {
            int index = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(index);
            ed.getImagEigenvalue(index);
            ed.getEigenvector(index);
        }

        if (data.consumeBoolean()) {
            double[] main2 = main.clone();
            double[] secondary2 = secondary.clone();

            for (int i = 0; i < main2.length / 2; i++) {
                double t = main2[i];
                main2[i] = main2[main2.length - 1 - i];
                main2[main2.length - 1 - i] = t;
            }
            for (int i = 0; i < secondary2.length / 2; i++) {
                double t = secondary2[i];
                secondary2[i] = secondary2[secondary2.length - 1 - i];
                secondary2[secondary2.length - 1 - i] = t;
            }

            EigenDecompositionImpl ed2 = new EigenDecompositionImpl(main2, secondary2, splitTolerance);
            ed2.getRealEigenvalues();
            ed2.getImagEigenvalues();
            ed2.getDeterminant();
            ed2.getV();
            ed2.getD();
            ed2.getVT();

            if (n > 0) {
                int index2 = data.consumeInt(0, n - 1);
                ed2.getRealEigenvalue(index2);
                ed2.getImagEigenvalue(index2);
                ed2.getEigenvector(index2);
            }
        }
    }
}