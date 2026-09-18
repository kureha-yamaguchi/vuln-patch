package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int runs = 1;
        if (data.remainingBytes() > 0) {
            runs += (data.consumeByte() & 0x03);
        }

        for (int run = 0; run < runs; run++) {
            int n = 1;
            if (data.remainingBytes() > 0) {
                n = data.consumeInt(1, 32);
            }

            double[] main = new double[n];
            double[] secondary = new double[n > 0 ? n - 1 : 0];

            for (int i = 0; i < main.length; i++) {
                double v;
                int mode = data.remainingBytes() > 0 ? data.consumeInt(0, 6) : 0;
                switch (mode) {
                    case 0: {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        v = Double.longBitsToDouble(bits);
                        break;
                    }
                    case 1:
                        v = (double) data.consumeInt(-16, 16);
                        break;
                    case 2:
                        v = data.consumeBoolean() ? 0.0d : -0.0d;
                        break;
                    case 3:
                        v = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    case 4:
                        v = Double.NaN;
                        break;
                    case 5:
                        v = ((double) data.consumeInt(-1000000, 1000000)) / (data.consumeBoolean() ? 1.0d : 1024.0d);
                        break;
                    default:
                        v = (i == 0 ? Double.MIN_VALUE : Double.MAX_VALUE);
                        if (data.consumeBoolean()) {
                            v = -v;
                        }
                        break;
                }
                main[i] = v;
            }

            for (int i = 0; i < secondary.length; i++) {
                double v;
                int mode = data.remainingBytes() > 0 ? data.consumeInt(0, 6) : 0;
                switch (mode) {
                    case 0: {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        v = Double.longBitsToDouble(bits);
                        break;
                    }
                    case 1:
                        v = (double) data.consumeInt(-16, 16);
                        break;
                    case 2:
                        v = data.consumeBoolean() ? 0.0d : -0.0d;
                        break;
                    case 3:
                        v = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    case 4:
                        v = Double.NaN;
                        break;
                    case 5:
                        v = ((double) data.consumeInt(-1000000, 1000000)) / (data.consumeBoolean() ? 1.0d : 2048.0d);
                        break;
                    default:
                        v = (i == 0 ? Double.MIN_VALUE : Double.MAX_VALUE);
                        if (data.consumeBoolean()) {
                            v = -v;
                        }
                        break;
                }
                secondary[i] = v;
            }

            if (n > 1 && data.consumeBoolean()) {
                main[0] = data.consumeBoolean() ? 0.0d : Double.MIN_VALUE;
                main[n - 1] = data.consumeBoolean() ? Double.MAX_VALUE : 1.0e300d;
            }

            double splitTolerance;
            int tolMode = data.remainingBytes() > 0 ? data.consumeInt(0, 5) : 0;
            switch (tolMode) {
                case 0: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    splitTolerance = Double.longBitsToDouble(bits);
                    break;
                }
                case 1:
                    splitTolerance = (double) data.consumeInt(-16, 16);
                    break;
                case 2:
                    splitTolerance = 0.0d;
                    break;
                case 3:
                    splitTolerance = -0.0d;
                    break;
                case 4:
                    splitTolerance = data.consumeBoolean() ? Double.NaN : Double.POSITIVE_INFINITY;
                    break;
                default:
                    splitTolerance = ((double) data.consumeInt(-1000000, 1000000)) / 65536.0d;
                    break;
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

            ed.getRealEigenvalues();
            ed.getImagEigenvalues();
            ed.getDeterminant();
            ed.getD();
            ed.getV();
            ed.getVT();

            if (n > 0) {
                int index = data.consumeInt(0, n - 1);
                ed.getRealEigenvalue(index);
                ed.getImagEigenvalue(index);
                ed.getEigenvector(index);
            }

            if (data.consumeBoolean()) {
                ed.getSolver();
            }
        }
    }
}