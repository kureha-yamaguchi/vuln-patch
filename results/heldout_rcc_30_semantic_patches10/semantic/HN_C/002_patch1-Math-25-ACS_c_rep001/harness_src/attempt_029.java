package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class Gen {
            double nextDouble() {
                switch (data.consumeInt(0, 9)) {
                    case 0: {
                        long hi = ((long) data.consumeInt()) << 32;
                        long lo = data.consumeInt() & 0xffffffffL;
                        return Double.longBitsToDouble(hi ^ lo);
                    }
                    case 1:
                        return (double) data.consumeInt();
                    case 2: {
                        int num = data.consumeInt();
                        int den = data.consumeInt();
                        if (den == 0) {
                            den = 1;
                        }
                        return ((double) num) / den;
                    }
                    case 3:
                        return data.consumeBoolean() ? Double.NaN
                                : (data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    case 4:
                        return data.consumeBoolean() ? 0.0d : -0.0d;
                    case 5:
                        return data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                    case 6:
                        return (data.consumeBoolean() ? -1.0d : 1.0d) * Math.PI * data.consumeInt();
                    case 7:
                        return (data.consumeBoolean() ? -1.0d : 1.0d) * Math.E * data.consumeInt();
                    case 8:
                        return data.consumeBoolean() ? 1.0d : -1.0d;
                    default:
                        return data.consumeByte();
                }
            }
        }

        Gen gen = new Gen();
        int rounds = 1 + data.consumeInt(0, 3);

        for (int round = 0; round < rounds; round++) {
            int n = data.consumeInt(0, 16);
            WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

            double baseX = gen.nextDouble();
            double baseY = gen.nextDouble();
            double step = gen.nextDouble();
            double amp = gen.nextDouble();
            double freq = gen.nextDouble();
            double phase = gen.nextDouble();

            int xPattern = data.consumeInt(0, 7);
            int yPattern = data.consumeInt(0, 7);

            for (int i = 0; i < n; i++) {
                double x;
                switch (xPattern) {
                    case 0:
                        x = gen.nextDouble();
                        break;
                    case 1:
                        x = baseX + i * step;
                        break;
                    case 2:
                        x = baseX;
                        break;
                    case 3:
                        x = baseX + (i / 2) * step;
                        break;
                    case 4:
                        x = baseX - i * step;
                        break;
                    case 5:
                        x = baseX + ((i % 2 == 0) ? i : -i) * step;
                        break;
                    case 6:
                        x = baseX + (i == 0 ? 0.0d : ((double) i) / (i + 1)) * step;
                        break;
                    default:
                        x = baseX + Math.sin(i) * step;
                        break;
                }

                double y;
                switch (yPattern) {
                    case 0:
                        y = gen.nextDouble();
                        break;
                    case 1:
                        y = baseY + i * amp;
                        break;
                    case 2:
                        y = baseY + amp * Math.sin(freq * x + phase);
                        break;
                    case 3:
                        y = baseY + amp * Math.cos(freq * x + phase);
                        break;
                    case 4:
                        y = baseY;
                        break;
                    case 5:
                        y = baseY + ((i % 2 == 0) ? amp : -amp);
                        break;
                    case 6:
                        y = x;
                        break;
                    default:
                        y = x * x + baseY;
                        break;
                }

                double weight;
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        weight = gen.nextDouble();
                        break;
                    case 1:
                        weight = 1.0d;
                        break;
                    case 2:
                        weight = 0.0d;
                        break;
                    case 3:
                        weight = -1.0d;
                        break;
                    default:
                        weight = Math.abs(gen.nextDouble());
                        break;
                }

                observations[i] = new WeightedObservedPoint(weight, x, y);
            }

            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
            double[] guess = guesser.guess();

            if (guess != null && guess.length == 3 && data.consumeBoolean()) {
                WeightedObservedPoint[] derived = new WeightedObservedPoint[n];
                double a = guess[0];
                double omega = guess[1];
                double phi = guess[2];
                for (int i = 0; i < n; i++) {
                    double x = observations[i].getX();
                    double y = a * Math.cos(omega * x + phi);
                    derived[i] = new WeightedObservedPoint(observations[i].getWeight(), x, y);
                }
                HarmonicFitter.ParameterGuesser guesser2 = new HarmonicFitter.ParameterGuesser(derived);
                guesser2.guess();
            }
        }
    }
}