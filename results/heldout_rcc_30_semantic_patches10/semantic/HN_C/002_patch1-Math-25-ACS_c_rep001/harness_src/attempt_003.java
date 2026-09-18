package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int len;
        if (data.consumeBoolean()) {
            len = data.consumeInt(0, 12);
        } else {
            len = Math.max(0, Math.min(12, data.consumeInt()));
        }

        WeightedObservedPoint[] observations = new WeightedObservedPoint[len];

        boolean forceSameX = data.consumeBoolean();
        boolean monotonicX = data.consumeBoolean();
        boolean tinySteps = data.consumeBoolean();
        boolean forceBoundaryPatterns = data.consumeBoolean();

        double baseX;
        {
            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            baseX = Double.longBitsToDouble(bits);
            if (forceBoundaryPatterns) {
                switch (Math.floorMod((int) data.consumeByte(), 8)) {
                    case 0:
                        baseX = 0.0d;
                        break;
                    case 1:
                        baseX = -0.0d;
                        break;
                    case 2:
                        baseX = Double.MIN_VALUE;
                        break;
                    case 3:
                        baseX = -Double.MIN_VALUE;
                        break;
                    case 4:
                        baseX = Double.MAX_VALUE;
                        break;
                    case 5:
                        baseX = -Double.MAX_VALUE;
                        break;
                    case 6:
                        baseX = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        baseX = Double.NaN;
                        break;
                }
            }
        }

        double currentX = baseX;

        for (int i = 0; i < len; i++) {
            double weight;
            {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                weight = Double.longBitsToDouble(bits);
                if (forceBoundaryPatterns && data.consumeBoolean()) {
                    switch (Math.floorMod((int) data.consumeByte(), 8)) {
                        case 0:
                            weight = 1.0d;
                            break;
                        case 1:
                            weight = 0.0d;
                            break;
                        case 2:
                            weight = -1.0d;
                            break;
                        case 3:
                            weight = Double.MIN_VALUE;
                            break;
                        case 4:
                            weight = Double.MAX_VALUE;
                            break;
                        case 5:
                            weight = Double.POSITIVE_INFINITY;
                            break;
                        case 6:
                            weight = Double.NEGATIVE_INFINITY;
                            break;
                        default:
                            weight = Double.NaN;
                            break;
                    }
                }
            }

            double x;
            if (i == 0) {
                x = currentX;
            } else if (forceSameX) {
                x = observations[0].getX();
            } else {
                long stepBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                double step = Double.longBitsToDouble(stepBits);
                if (tinySteps) {
                    int selector = Math.floorMod((int) data.consumeByte(), 6);
                    if (selector == 0) {
                        step = 0.0d;
                    } else if (selector == 1) {
                        step = Double.MIN_VALUE;
                    } else if (selector == 2) {
                        step = -Double.MIN_VALUE;
                    } else if (selector == 3) {
                        step = 1.0d;
                    } else if (selector == 4) {
                        step = -1.0d;
                    } else {
                        step = step;
                    }
                }
                if (monotonicX && !Double.isNaN(step)) {
                    step = Math.abs(step);
                }
                x = currentX + step;
                currentX = x;
            }

            double y;
            {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                y = Double.longBitsToDouble(bits);
                if (forceBoundaryPatterns && data.consumeBoolean()) {
                    switch (Math.floorMod((int) data.consumeByte(), 10)) {
                        case 0:
                            y = 0.0d;
                            break;
                        case 1:
                            y = -0.0d;
                            break;
                        case 2:
                            y = 1.0d;
                            break;
                        case 3:
                            y = -1.0d;
                            break;
                        case 4:
                            y = Double.MIN_VALUE;
                            break;
                        case 5:
                            y = -Double.MIN_VALUE;
                            break;
                        case 6:
                            y = Double.MAX_VALUE;
                            break;
                        case 7:
                            y = -Double.MAX_VALUE;
                            break;
                        case 8:
                            y = Double.POSITIVE_INFINITY;
                            break;
                        default:
                            y = Double.NaN;
                            break;
                    }
                }
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        if (data.consumeBoolean() && len > 1) {
            for (int i = 0, j = len - 1; i < j; i++, j--) {
                WeightedObservedPoint tmp = observations[i];
                observations[i] = observations[j];
                observations[j] = tmp;
            }
        }

        if (data.consumeBoolean() && len > 2) {
            int i = Math.floorMod(data.consumeInt(), len);
            int j = Math.floorMod(data.consumeInt(), len);
            WeightedObservedPoint tmp = observations[i];
            observations[i] = observations[j];
            observations[j] = tmp;
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        double[] guess = guesser.guess();

        if (guess.length >= 3 && data.consumeBoolean()) {
            double a = guess[0];
            double omega = guess[1];
            double phase = guess[2];
            int evalCount = Math.min(len, 8);
            for (int i = 0; i < evalCount; i++) {
                double x = observations[i].getX();
                double value = a * Math.cos(omega * x + phase);
                if (Double.doubleToLongBits(value) == 0x7ff8000000000000L) {
                    throw new RuntimeException("unreachable");
                }
            }
        }
    }
}