package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int actions = data.consumeInt(0, 32);
        for (int i = 0; i < actions; i++) {
            int kind = data.consumeInt(0, 5);

            double a;
            switch (data.consumeInt(0, 11)) {
                case 0:
                    a = 0.0d;
                    break;
                case 1:
                    a = -0.0d;
                    break;
                case 2:
                    a = Double.NaN;
                    break;
                case 3:
                    a = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    a = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    a = data.consumeByte();
                    break;
                case 6:
                    a = data.consumeInt();
                    break;
                case 7:
                    a = data.consumeInt() / 1.0d;
                    break;
                case 8:
                    a = data.consumeInt() / 3.0d;
                    break;
                case 9:
                    a = Math.scalb((double) data.consumeByte(), data.consumeInt(-32, 32));
                    break;
                case 10:
                    a = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                default:
                    String s = data.consumeAsciiString(16);
                    a = s.length() == 0 ? 0.0d : s.charAt(0);
                    break;
            }

            double b;
            switch (data.consumeInt(0, 11)) {
                case 0:
                    b = 0.0d;
                    break;
                case 1:
                    b = -0.0d;
                    break;
                case 2:
                    b = Double.NaN;
                    break;
                case 3:
                    b = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    b = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    b = data.consumeByte();
                    break;
                case 6:
                    b = data.consumeInt();
                    break;
                case 7:
                    b = data.consumeInt() / 1.0d;
                    break;
                case 8:
                    b = data.consumeInt() / 7.0d;
                    break;
                case 9:
                    b = Math.scalb((double) data.consumeByte(), data.consumeInt(-32, 32));
                    break;
                case 10:
                    b = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                default:
                    String s = data.consumeString(16);
                    b = s.length() == 0 ? 0.0d : s.charAt(s.length() - 1);
                    break;
            }

            double c;
            switch (data.consumeInt(0, 11)) {
                case 0:
                    c = 0.0d;
                    break;
                case 1:
                    c = -0.0d;
                    break;
                case 2:
                    c = Double.NaN;
                    break;
                case 3:
                    c = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    c = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    c = data.consumeByte();
                    break;
                case 6:
                    c = data.consumeInt();
                    break;
                case 7:
                    c = data.consumeInt() / 1.0d;
                    break;
                case 8:
                    c = data.consumeInt() / 11.0d;
                    break;
                case 9:
                    c = Math.scalb((double) data.consumeByte(), data.consumeInt(-32, 32));
                    break;
                case 10:
                    c = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                default:
                    byte[] bs = data.consumeBytes(8);
                    long bits = 0L;
                    for (int j = 0; j < bs.length; j++) {
                        bits = (bits << 8) ^ (bs[j] & 0xffL);
                    }
                    c = Double.longBitsToDouble(bits);
                    break;
            }

            switch (kind) {
                case 0:
                    fitter.addObservedPoint(a, b);
                    break;
                case 1:
                    fitter.addObservedPoint(a, b, c);
                    break;
                case 2:
                    fitter.clearObservations();
                    break;
                case 3:
                    if (data.consumeBoolean()) {
                        fitter.addObservedPoint(1.0d, a, b);
                    } else {
                        fitter.addObservedPoint(-1.0d, a, b);
                    }
                    break;
                case 4:
                    if (data.consumeBoolean()) {
                        fitter.fit();
                    }
                    break;
                default:
                    fitter.addObservedPoint(c, a, b);
                    if (data.consumeBoolean()) {
                        fitter.fit();
                    }
                    break;
            }
        }

        fitter.fit();
    }
}